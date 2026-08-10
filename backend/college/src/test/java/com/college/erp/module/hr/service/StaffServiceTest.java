package com.college.erp.module.hr.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.module.academic.domain.Department;
import com.college.erp.module.academic.service.DepartmentService;
import com.college.erp.module.hr.domain.StaffProfile;
import com.college.erp.module.hr.domain.StaffProfileRepository;
import com.college.erp.module.hr.domain.StaffType;
import com.college.erp.module.hr.dto.CreateStaffProfileRequest;
import com.college.erp.module.hr.dto.StaffProfileDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 03-srs.md [HR-001]. Named to match the module's other *ServiceTest files. */
@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock
    private StaffProfileRepository staffProfileRepository;
    @Mock
    private DepartmentService departmentService;
    @Mock
    private DesignationService designationService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private StaffService staffService;

    private Department buildDepartment() {
        Department department = Department.builder().name("Computer Science").code("CSE").build();
        department.setId(UUID.randomUUID());
        return department;
    }

    private CreateStaffProfileRequest buildRequest(UUID departmentId) {
        CreateStaffProfileRequest request = new CreateStaffProfileRequest();
        request.setUserId(UUID.randomUUID());
        request.setEmployeeNumber("EMP-0001");
        request.setDepartmentId(departmentId);
        request.setStaffType(StaffType.TEACHING);
        request.setJoiningDate(LocalDate.of(2020, 6, 1));
        return request;
    }

    @Test
    @DisplayName("create() — registers a staff profile linked to a department")
    void create_registersProfile() {
        // ARRANGE
        staffService = new StaffService(staffProfileRepository, departmentService, designationService,
                auditLogWriter);
        Department department = buildDepartment();
        CreateStaffProfileRequest request = buildRequest(department.getId());
        when(staffProfileRepository.existsByEmployeeNumber("EMP-0001")).thenReturn(false);
        when(staffProfileRepository.existsByUserIdAndDeletedFalse(request.getUserId())).thenReturn(false);
        when(departmentService.getDepartmentEntity(department.getId())).thenReturn(department);
        when(staffProfileRepository.save(any(StaffProfile.class))).thenAnswer(inv -> {
            StaffProfile profile = inv.getArgument(0);
            profile.setId(UUID.randomUUID());
            return profile;
        });

        // ACT
        StaffProfileDto dto = staffService.create(request);

        // ASSERT
        assertThat(dto.getEmployeeNumber()).isEqualTo("EMP-0001");
        assertThat(dto.getDepartmentId()).isEqualTo(department.getId());
        assertThat(dto.getStaffType()).isEqualTo(StaffType.TEACHING);
    }

    @Test
    @DisplayName("create() — duplicate employee number is rejected")
    void create_duplicateEmployeeNumber_throws() {
        // ARRANGE
        staffService = new StaffService(staffProfileRepository, departmentService, designationService,
                auditLogWriter);
        CreateStaffProfileRequest request = buildRequest(UUID.randomUUID());
        when(staffProfileRepository.existsByEmployeeNumber("EMP-0001")).thenReturn(true);

        // ACT / ASSERT
        assertThatThrownBy(() -> staffService.create(request)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("create() — a user who already has a staff profile is rejected")
    void create_duplicateUser_throws() {
        // ARRANGE
        staffService = new StaffService(staffProfileRepository, departmentService, designationService,
                auditLogWriter);
        CreateStaffProfileRequest request = buildRequest(UUID.randomUUID());
        when(staffProfileRepository.existsByEmployeeNumber("EMP-0001")).thenReturn(false);
        when(staffProfileRepository.existsByUserIdAndDeletedFalse(request.getUserId())).thenReturn(true);

        // ACT / ASSERT
        assertThatThrownBy(() -> staffService.create(request)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("getByUserId() — returns the profile for a linked user account")
    void getByUserId_returnsProfile() {
        // ARRANGE
        staffService = new StaffService(staffProfileRepository, departmentService, designationService,
                auditLogWriter);
        UUID userId = UUID.randomUUID();
        StaffProfile profile = StaffProfile.builder().userId(userId).employeeNumber("EMP-0001")
                .department(buildDepartment()).staffType(StaffType.TEACHING)
                .joiningDate(LocalDate.now()).build();
        profile.setId(UUID.randomUUID());
        when(staffProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile));

        // ACT
        StaffProfileDto dto = staffService.getByUserId(userId);

        // ASSERT
        assertThat(dto.getUserId()).isEqualTo(userId);
    }
}
