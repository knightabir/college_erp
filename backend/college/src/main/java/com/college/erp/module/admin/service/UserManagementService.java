package com.college.erp.module.admin.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
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
import com.college.erp.tenant.TenantContext;
import com.college.erp.tenant.domain.Tenant;
import com.college.erp.tenant.domain.TenantRepository;
import com.college.erp.tenant.domain.TenantStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 05-api-documentation.md §13 "User Management". 08-system-architecture.md places
 * `UserManagementService` under `module.admin`, operating directly on module.auth's
 * `User`/`RoleAssignment` repositories rather than through a module.auth service — a deliberate
 * exception to the usual "cross-module access goes through the other module's service" rule
 * (07-coding-standards.md §4.4), since admin's entire purpose here IS managing auth's own
 * entities and no `UserService` with a fuller lifecycle exists in module.auth to route through
 * (`AuthService` only covers login/refresh/logout/change-password). Also injects the kernel
 * {@code TenantRepository} directly — {@code com.college.erp.tenant} isn't a {@code module.*}
 * package, so that isn't a cross-module-repository violation either (see ModuleBoundaryTest).
 *
 * <p><strong>Multi-tenancy</strong> (08-system-architecture.md §5.2): every method below except
 * {@link #bootstrapTenantAndSuperAdminIfMissing} and {@link #createTenantOwner} runs inside an
 * authenticated request and scopes strictly to the caller's own tenant, read ambiently from
 * {@link TenantContext} — the same ThreadLocal-read pattern {@code AuditLogWriter} already uses
 * for {@code SecurityContextHolder} rather than threading a "current tenant" parameter through
 * every method signature. {@code users}/{@code role_assignments} carry a {@code tenant_id} column
 * but — unlike {@code module.academic}/{@code module.student}'s tables — are NOT Postgres
 * Row-Level-Security-protected (see V017's header comment: login resolves a user by username/email
 * alone, which needs an unscoped, cross-tenant lookup RLS would break). Tenant isolation for this
 * module is therefore enforced here, in application code, not by the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private static final String MODULE = "ADMIN";

    private final UserRepository userRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public UserDto create(CreateUserRequest request, UUID createdByUserId) {
        if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' already exists");
        }
        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .mustChangePassword(true) // admin-issued temp password — force a change on first login
                .build();
        // tenantId lives on TenantScopedEntity (a superclass) — plain @Builder (07-coding-
        // standards.md §2.2) doesn't expose inherited fields, so it's set via the inherited
        // setter instead, same as every other TenantScopedEntity subclass's create() method.
        user.setTenantId(TenantContext.getCurrentTenantId());
        User saved = userRepository.save(user);

        RoleAssignment roleAssignment = RoleAssignment.builder()
                .tenantId(TenantContext.getCurrentTenantId())
                .userId(saved.getId())
                .role(request.getRole())
                .assignedBy(createdByUserId)
                .build();
        roleAssignmentRepository.save(roleAssignment);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "User", saved.getId().toString(),
                "Created user " + saved.getUsername() + " with role " + request.getRole());
        log.info("User created by admin: [{}] role=[{}]", saved.getUsername(), request.getRole());

        return UserDto.from(saved, List.of(request.getRole().name()));
    }

    @Transactional(readOnly = true)
    public UserDto getById(UUID id) {
        return toDto(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<UserDto> list(Pageable pageable) {
        return userRepository.findByTenantIdAndDeletedFalse(TenantContext.getCurrentTenantId(), pageable)
                .map(this::toDto);
    }

    @Transactional
    public UserDto lock(UUID id) {
        User user = getEntity(id);
        user.setLocked(true);
        User saved = userRepository.save(user);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "User", id.toString(), "Locked by admin");

        return toDto(saved);
    }

    @Transactional
    public UserDto unlock(UUID id) {
        User user = getEntity(id);
        user.setLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        User saved = userRepository.save(user);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "User", id.toString(), "Unlocked by admin");

        return toDto(saved);
    }

    /** FR-RBAC-3 (see PermissionGrant's javadoc): a user must never grant themselves a role —
     *  enforced the same way as PermissionGrantService, here for role assignment specifically. */
    @Transactional
    public RoleAssignmentDto assignRole(UUID userId, AssignRoleRequest request, UUID assignedByUserId) {
        if (userId.equals(assignedByUserId)) {
            throw new BusinessRuleViolationException("A user cannot assign a role to themselves");
        }
        User user = getEntity(userId);

        RoleAssignment roleAssignment = RoleAssignment.builder()
                .tenantId(user.getTenantId())
                .userId(user.getId())
                .role(request.getRole())
                .departmentId(request.getDepartmentId())
                .assignedBy(assignedByUserId)
                .expiresAt(request.getExpiresAt())
                .build();
        RoleAssignment saved = roleAssignmentRepository.save(roleAssignment);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "RoleAssignment", saved.getId().toString(),
                "Assigned role " + request.getRole() + " to user " + userId);

        return RoleAssignmentDto.from(saved);
    }

    /**
     * Solves the auth bootstrap problem for **air-gap/on-prem, single-college deployments**: every
     * user-creation path in this system requires an already-authenticated SUPER_ADMIN/ADMIN caller
     * except the public self-service {@code POST /api/v1/register} flow (see
     * {@code RegistrationController}/{@link #createTenantOwner}), which a closed air-gapped network
     * has no use for — there's only ever going to be one college on that deployment. Called once at
     * startup by {@code BootstrapAdminRunner} (see its javadoc and
     * {@code ErpProperties.Bootstrap.Admin}). Ensures a single "default" {@link Tenant} row exists
     * first (matching 08-system-architecture.md §5.2's "single-tenant deployments use the same
     * schema with a single fixed tenant row — no code changes between modes"), then a SUPER_ADMIN
     * scoped to it.
     *
     * <p>Idempotent — a no-op once any active SUPER_ADMIN role assignment exists <em>anywhere</em>
     * (not scoped to any one tenant), so it's safe to run on every restart. This also means a cloud
     * deployment where real colleges have already self-registered (each with their own SUPER_ADMIN)
     * naturally never triggers this path either — set {@code erp.bootstrap.admin.enabled=false}
     * for such deployments to skip even checking, since the "default" tenant this method would
     * create serves no purpose there.
     *
     * <p>The created account's role assignment is self-attributed ({@code assignedBy} = its own
     * id) since, by definition, there is no prior actor to attribute a system's very first grant
     * to — {@code role_assignments.assigned_by} is {@code NOT NULL} with no "system" sentinel
     * column in the schema, so self-reference is the only value that doesn't invent one.
     *
     * @return the created account's id, or {@code null} if bootstrapping was skipped because a
     *         SUPER_ADMIN already exists
     */
    @Transactional
    public UUID bootstrapTenantAndSuperAdminIfMissing(String tenantName, String tenantCode, String tenantContactEmail,
            String username, String email, String fullName, String rawPassword) {
        if (roleAssignmentRepository.existsByRoleAndActiveTrue(UserRole.SUPER_ADMIN)) {
            return null;
        }
        if (userRepository.existsByUsernameAndDeletedFalse(username)
                || userRepository.existsByEmailAndDeletedFalse(email)) {
            // A user with this username/email already exists but holds no active SUPER_ADMIN
            // role — most likely a stale/renamed bootstrap account. Don't silently create a
            // second, colliding account; surface it instead so an operator can decide (e.g.
            // grant that existing user SUPER_ADMIN via POST /admin/users/{id}/roles).
            log.warn("Bootstrap SUPER_ADMIN skipped: username='{}' or email='{}' already exists but holds no "
                    + "active SUPER_ADMIN role — assign the role manually if this account should be the root admin.",
                    username, email);
            return null;
        }

        Tenant tenant = tenantRepository.findByCodeAndDeletedFalse(tenantCode).orElseGet(() -> {
            Tenant created = Tenant.builder()
                    .name(tenantName)
                    .code(tenantCode)
                    .contactEmail(tenantContactEmail)
                    .status(TenantStatus.ACTIVE)
                    .build();
            Tenant savedTenant = tenantRepository.save(created);
            log.warn("Bootstrap tenant created: code='{}' name='{}'", tenantCode, tenantName);
            return savedTenant;
        });

        UUID createdId = createTenantOwner(tenant.getId(), username, email, fullName, rawPassword);
        log.warn("Bootstrap SUPER_ADMIN account created: username='{}' email='{}' tenant='{}' — log in and change "
                + "this password immediately (mustChangePassword is set).", username, email, tenantCode);
        return createdId;
    }

    /**
     * Creates a brand-new tenant's own first administrator (SUPER_ADMIN, scoped to that tenant) —
     * used by {@code TenantRegistrationService} for the public self-service college-registration
     * flow, and by {@link #bootstrapTenantAndSuperAdminIfMissing} for the air-gap/on-prem bootstrap
     * path above. Unlike that method, this one performs no idempotency check against an existing
     * SUPER_ADMIN — a brand-new tenant can never already have one. Username/email uniqueness IS
     * still checked here (same as {@link #create}) since usernames/emails stay globally unique
     * across the whole platform, not just within a tenant — see {@code User}'s class javadoc.
     * Role assignment is self-attributed for the same reason as the bootstrap path above.
     */
    @Transactional
    public UUID createTenantOwner(UUID tenantId, String username, String email, String fullName,
            String rawPassword) {
        if (userRepository.existsByUsernameAndDeletedFalse(username)) {
            throw new DuplicateResourceException("Username '" + username + "' already exists");
        }
        if (userRepository.existsByEmailAndDeletedFalse(email)) {
            throw new DuplicateResourceException("Email '" + email + "' already exists");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .fullName(fullName)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .enabled(true)
                .mustChangePassword(true)
                .build();
        user.setTenantId(tenantId); // see create()'s comment — inherited setter, not the builder
        User saved = userRepository.save(user);

        RoleAssignment roleAssignment = RoleAssignment.builder()
                .tenantId(tenantId)
                .userId(saved.getId())
                .role(UserRole.SUPER_ADMIN)
                .assignedBy(saved.getId())
                .build();
        roleAssignmentRepository.save(roleAssignment);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "User", saved.getId().toString(),
                "Tenant owner (SUPER_ADMIN) account created");

        return saved.getId();
    }

    private UserDto toDto(User user) {
        List<String> roles = roleAssignmentRepository.findActiveByUserId(user.getId()).stream()
                .map(ra -> ra.getRole().name())
                .toList();
        return UserDto.from(user, roles);
    }

    /** Tenant-scoped lookup — a user belonging to a different tenant is treated as not found
     *  (404), never as a 403, so a caller can't distinguish "wrong tenant" from "doesn't exist" —
     *  same "deliberately generic" posture as AuthService.invalidCredentials(). Objects.equals
     *  (not user.getTenantId().equals(...)) so this stays a safe comparison in unit tests, where
     *  TenantContext is never set and both sides are null. */
    private User getEntity(UUID id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (!Objects.equals(user.getTenantId(), TenantContext.getCurrentTenantId())) {
            throw new ResourceNotFoundException("User", id);
        }
        return user;
    }
}
