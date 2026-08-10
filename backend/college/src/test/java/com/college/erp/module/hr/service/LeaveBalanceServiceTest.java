package com.college.erp.module.hr.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.module.academic.domain.Department;
import com.college.erp.module.hr.domain.LeaveBalanceRepository;
import com.college.erp.module.hr.domain.LeaveType;
import com.college.erp.module.hr.domain.StaffProfile;
import com.college.erp.module.hr.domain.StaffProfileRepository;
import com.college.erp.module.hr.domain.StaffType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 03-srs.md [HR-002]. Named to match the module's other *ServiceTest files. */
@ExtendWith(MockitoExtension.class)
class LeaveBalanceServiceTest {

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;
    @Mock
    private StaffProfileRepository staffProfileRepository;
    @Mock
    private LeaveTypeService leaveTypeService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private LeaveBalanceService leaveBalanceService;

    private StaffProfile buildStaff(UUID userId) {
        Department department = Department.builder().name("CS").code("CS").build();
        department.setId(UUID.randomUUID());
        StaffProfile profile = StaffProfile.builder().userId(userId).employeeNumber("EMP-1")
                .department(department).staffType(StaffType.TEACHING).joiningDate(LocalDate.now()).build();
        profile.setId(UUID.randomUUID());
        return profile;
    }

    private LeaveType buildLeaveType(String code, int daysPerYear) {
        LeaveType leaveType = LeaveType.builder().code(code).name(code).daysPerYear(daysPerYear).build();
        leaveType.setId(UUID.randomUUID());
        return leaveType;
    }

    @Test
    @DisplayName("initializeForYear() — creates one balance per staff × leave type, seeded from days_per_year")
    void initializeForYear_createsBalancePerStaffPerType() {
        // ARRANGE
        leaveBalanceService = new LeaveBalanceService(leaveBalanceRepository, staffProfileRepository,
                leaveTypeService, auditLogWriter);
        UUID userId = UUID.randomUUID();
        StaffProfile staff = buildStaff(userId);
        LeaveType casual = buildLeaveType("CL", 12);
        LeaveType sick = buildLeaveType("SL", 10);
        when(staffProfileRepository.findByDeletedFalse()).thenReturn(List.of(staff));
        when(leaveTypeService.listActiveEntities()).thenReturn(List.of(casual, sick));
        when(leaveBalanceRepository.existsByUserIdAndLeaveTypeIdAndYearAndDeletedFalse(any(), any(), anyInt()))
                .thenReturn(false);

        // ACT
        int created = leaveBalanceService.initializeForYear(2026);

        // ASSERT
        assertThat(created).isEqualTo(2);
        verify(leaveBalanceRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("initializeForYear() — is idempotent: an already-existing balance is skipped, not duplicated")
    void initializeForYear_skipsExisting() {
        // ARRANGE
        leaveBalanceService = new LeaveBalanceService(leaveBalanceRepository, staffProfileRepository,
                leaveTypeService, auditLogWriter);
        UUID userId = UUID.randomUUID();
        StaffProfile staff = buildStaff(userId);
        LeaveType casual = buildLeaveType("CL", 12);
        when(staffProfileRepository.findByDeletedFalse()).thenReturn(List.of(staff));
        when(leaveTypeService.listActiveEntities()).thenReturn(List.of(casual));
        when(leaveBalanceRepository.existsByUserIdAndLeaveTypeIdAndYearAndDeletedFalse(userId, casual.getId(), 2026))
                .thenReturn(true);

        // ACT
        int created = leaveBalanceService.initializeForYear(2026);

        // ASSERT
        assertThat(created).isEqualTo(0);
        verify(leaveBalanceRepository, never()).save(any());
    }
}
