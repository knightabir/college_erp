package com.college.erp.module.admin.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.admin.dto.AssignRoleRequest;
import com.college.erp.module.admin.dto.CreateUserRequest;
import com.college.erp.module.admin.dto.RoleAssignmentDto;
import com.college.erp.module.admin.dto.UserDto;
import com.college.erp.module.auth.domain.RoleAssignment;
import com.college.erp.module.auth.domain.RoleAssignmentRepository;
import com.college.erp.module.auth.domain.User;
import com.college.erp.module.auth.domain.UserRepository;
import com.college.erp.module.auth.domain.UserRole;
import com.college.erp.tenant.domain.Tenant;
import com.college.erp.tenant.domain.TenantRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 05-api-documentation.md §13 "User Management". Named to match the module's other *ServiceTest files. */
@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleAssignmentRepository roleAssignmentRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditLogWriter auditLogWriter;

    private UserManagementService userManagementService;

    private UserManagementService service() {
        return new UserManagementService(userRepository, roleAssignmentRepository, tenantRepository,
                passwordEncoder, auditLogWriter);
    }

    private CreateUserRequest buildCreateRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("jdoe");
        request.setEmail("jdoe@college.edu");
        request.setFullName("John Doe");
        request.setPassword("TempPass@123");
        request.setRole(UserRole.FACULTY);
        return request;
    }

    @Test
    @DisplayName("create() — registers the user with a hashed password, forces a password change, "
            + "and creates the initial role assignment")
    void create_registersUserAndInitialRole() {
        // ARRANGE
        userManagementService = service();
        UUID adminId = UUID.randomUUID();
        when(userRepository.existsByUsernameAndDeletedFalse("jdoe")).thenReturn(false);
        when(userRepository.existsByEmailAndDeletedFalse("jdoe@college.edu")).thenReturn(false);
        when(passwordEncoder.encode("TempPass@123")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(roleAssignmentRepository.save(any(RoleAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        UserDto dto = userManagementService.create(buildCreateRequest(), adminId);

        // ASSERT
        assertThat(dto.getUsername()).isEqualTo("jdoe");
        assertThat(dto.isMustChangePassword()).isTrue();
        assertThat(dto.getRoles()).containsExactly("FACULTY");
    }

    @Test
    @DisplayName("create() — duplicate username is rejected")
    void create_duplicateUsername_throws() {
        // ARRANGE
        userManagementService = service();
        when(userRepository.existsByUsernameAndDeletedFalse("jdoe")).thenReturn(true);

        // ACT / ASSERT
        assertThatThrownBy(() -> userManagementService.create(buildCreateRequest(), UUID.randomUUID()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("create() — duplicate email is rejected")
    void create_duplicateEmail_throws() {
        // ARRANGE
        userManagementService = service();
        when(userRepository.existsByUsernameAndDeletedFalse("jdoe")).thenReturn(false);
        when(userRepository.existsByEmailAndDeletedFalse("jdoe@college.edu")).thenReturn(true);

        // ACT / ASSERT
        assertThatThrownBy(() -> userManagementService.create(buildCreateRequest(), UUID.randomUUID()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("lock() — sets locked=true")
    void lock_setsLockedTrue() {
        // ARRANGE
        userManagementService = service();
        UUID userId = UUID.randomUUID();
        User user = User.builder().username("jdoe").email("jdoe@college.edu").fullName("John Doe")
                .passwordHash("x").build();
        user.setId(userId);
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleAssignmentRepository.findActiveByUserId(userId)).thenReturn(List.of());

        // ACT
        UserDto dto = userManagementService.lock(userId);

        // ASSERT
        assertThat(dto.isLocked()).isTrue();
    }

    @Test
    @DisplayName("unlock() — resets locked, failedLoginAttempts, and lockedUntil")
    void unlock_resetsLockState() {
        // ARRANGE
        userManagementService = service();
        UUID userId = UUID.randomUUID();
        User user = User.builder().username("jdoe").email("jdoe@college.edu").fullName("John Doe")
                .passwordHash("x").locked(true).failedLoginAttempts(5)
                .lockedUntil(java.time.Instant.now().plusSeconds(600)).build();
        user.setId(userId);
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleAssignmentRepository.findActiveByUserId(userId)).thenReturn(List.of());

        // ACT
        UserDto dto = userManagementService.unlock(userId);

        // ASSERT
        assertThat(dto.isLocked()).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("getById() — unknown user raises ResourceNotFoundException")
    void getById_unknownUser_throws() {
        // ARRANGE
        userManagementService = service();
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

        // ACT / ASSERT
        assertThatThrownBy(() -> userManagementService.getById(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("assignRole() — a user cannot assign a role to themselves (FR-RBAC-3)")
    void assignRole_self_throws() {
        // ARRANGE
        userManagementService = service();
        UUID userId = UUID.randomUUID();
        AssignRoleRequest request = new AssignRoleRequest();
        request.setRole(UserRole.HOD);

        // ACT / ASSERT
        assertThatThrownBy(() -> userManagementService.assignRole(userId, request, userId))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("assignRole() — a different admin may assign a role")
    void assignRole_differentUser_succeeds() {
        // ARRANGE
        userManagementService = service();
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        User user = User.builder().username("jdoe").email("jdoe@college.edu").fullName("John Doe")
                .passwordHash("x").build();
        user.setId(userId);
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(roleAssignmentRepository.save(any(RoleAssignment.class))).thenAnswer(inv -> {
            RoleAssignment ra = inv.getArgument(0);
            ra.setId(UUID.randomUUID());
            return ra;
        });

        AssignRoleRequest request = new AssignRoleRequest();
        request.setRole(UserRole.HOD);
        UUID departmentId = UUID.randomUUID();
        request.setDepartmentId(departmentId);

        // ACT
        RoleAssignmentDto dto = userManagementService.assignRole(userId, request, adminId);

        // ASSERT
        assertThat(dto.getRole()).isEqualTo(UserRole.HOD);
        assertThat(dto.getDepartmentId()).isEqualTo(departmentId);
        assertThat(dto.getAssignedBy()).isEqualTo(adminId);
    }

    @Test
    @DisplayName("bootstrapTenantAndSuperAdminIfMissing() — no SUPER_ADMIN exists yet: creates the "
            + "default tenant and a self-attributed, must-change-password admin")
    void bootstrapTenantAndSuperAdminIfMissing_noneExists_createsTenantAndSelfAttributedAccount() {
        // ARRANGE
        userManagementService = service();
        when(roleAssignmentRepository.existsByRoleAndActiveTrue(UserRole.SUPER_ADMIN)).thenReturn(false);
        when(tenantRepository.findByCodeAndDeletedFalse("default")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant tenant = inv.getArgument(0);
            tenant.setId(UUID.randomUUID());
            return tenant;
        });
        when(userRepository.existsByUsernameAndDeletedFalse("superadmin")).thenReturn(false);
        when(userRepository.existsByEmailAndDeletedFalse("superadmin@college.edu")).thenReturn(false);
        when(passwordEncoder.encode("ChangeMe@123")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(roleAssignmentRepository.save(any(RoleAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        UUID createdId = userManagementService.bootstrapTenantAndSuperAdminIfMissing(
                "Default College", "default", "admin@college.edu",
                "superadmin", "superadmin@college.edu", "System Administrator", "ChangeMe@123");

        // ASSERT
        assertThat(createdId).isNotNull();
        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getCode()).isEqualTo("default");
        ArgumentCaptor<RoleAssignment> captor = ArgumentCaptor.forClass(RoleAssignment.class);
        verify(roleAssignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.SUPER_ADMIN);
        assertThat(captor.getValue().getAssignedBy()).isEqualTo(createdId); // self-attributed
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().isMustChangePassword()).isTrue();
    }

    @Test
    @DisplayName("bootstrapTenantAndSuperAdminIfMissing() — an active SUPER_ADMIN already exists "
            + "anywhere: no-op, idempotent across restarts")
    void bootstrapTenantAndSuperAdminIfMissing_alreadyExists_noOp() {
        // ARRANGE
        userManagementService = service();
        when(roleAssignmentRepository.existsByRoleAndActiveTrue(UserRole.SUPER_ADMIN)).thenReturn(true);

        // ACT
        UUID createdId = userManagementService.bootstrapTenantAndSuperAdminIfMissing(
                "Default College", "default", "admin@college.edu",
                "superadmin", "superadmin@college.edu", "System Administrator", "ChangeMe@123");

        // ASSERT
        assertThat(createdId).isNull();
        verify(userRepository, never()).save(any(User.class));
        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    @DisplayName("bootstrapTenantAndSuperAdminIfMissing() — username/email already taken by a "
            + "non-admin account: skipped rather than silently colliding")
    void bootstrapTenantAndSuperAdminIfMissing_usernameTakenByOther_skipped() {
        // ARRANGE
        userManagementService = service();
        when(roleAssignmentRepository.existsByRoleAndActiveTrue(UserRole.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.existsByUsernameAndDeletedFalse("superadmin")).thenReturn(true);

        // ACT
        UUID createdId = userManagementService.bootstrapTenantAndSuperAdminIfMissing(
                "Default College", "default", "admin@college.edu",
                "superadmin", "superadmin@college.edu", "System Administrator", "ChangeMe@123");

        // ASSERT
        assertThat(createdId).isNull();
        verify(userRepository, never()).save(any(User.class));
        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    @DisplayName("createTenantOwner() — creates a self-attributed SUPER_ADMIN scoped to the given tenant")
    void createTenantOwner_createsSelfAttributedAccountForTenant() {
        // ARRANGE
        userManagementService = service();
        UUID tenantId = UUID.randomUUID();
        when(userRepository.existsByUsernameAndDeletedFalse("newadmin")).thenReturn(false);
        when(userRepository.existsByEmailAndDeletedFalse("newadmin@newcollege.edu")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd@123")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(roleAssignmentRepository.save(any(RoleAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        UUID createdId = userManagementService.createTenantOwner(
                tenantId, "newadmin", "newadmin@newcollege.edu", "New Admin", "Passw0rd@123");

        // ASSERT
        assertThat(createdId).isNotNull();
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getTenantId()).isEqualTo(tenantId);
        ArgumentCaptor<RoleAssignment> captor = ArgumentCaptor.forClass(RoleAssignment.class);
        verify(roleAssignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.SUPER_ADMIN);
    }

    @Test
    @DisplayName("createTenantOwner() — duplicate username is rejected")
    void createTenantOwner_duplicateUsername_throws() {
        // ARRANGE
        userManagementService = service();
        when(userRepository.existsByUsernameAndDeletedFalse("newadmin")).thenReturn(true);

        // ACT / ASSERT
        assertThatThrownBy(() -> userManagementService.createTenantOwner(
                UUID.randomUUID(), "newadmin", "newadmin@newcollege.edu", "New Admin", "Passw0rd@123"))
                .isInstanceOf(DuplicateResourceException.class);
    }
}
