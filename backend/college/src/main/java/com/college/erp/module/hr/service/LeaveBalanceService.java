package com.college.erp.module.hr.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.hr.domain.LeaveBalance;
import com.college.erp.module.hr.domain.LeaveBalanceRepository;
import com.college.erp.module.hr.domain.LeaveType;
import com.college.erp.module.hr.domain.StaffProfile;
import com.college.erp.module.hr.domain.StaffProfileRepository;
import com.college.erp.module.hr.dto.LeaveBalanceDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 03-srs.md [HR-002]: "Balances are initialized at the start of each academic year." No endpoint
 * for this is documented in 05-api-documentation.md §10, and there's no scheduled-job trigger for
 * it in 03-srs.md [SCHED-004] either — modeled as an idempotent bulk operation
 * (initializeForYear) an Admin/HR Manager calls once per year, rather than an automatic cron job,
 * since nothing in the docs specifies *when* in the year it should fire.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveBalanceService {

    private static final String MODULE = "HR";

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final LeaveTypeService leaveTypeService;
    private final AuditLogWriter auditLogWriter;

    /** Creates a LeaveBalance row (total_days = the leave type's days_per_year) for every active
     *  staff member × every active leave type, for the given year. Skips any (user, leaveType,
     *  year) combination that already exists — safe to call more than once. */
    @Transactional
    public int initializeForYear(int year) {
        List<StaffProfile> staff = staffProfileRepository.findByDeletedFalse();
        List<LeaveType> leaveTypes = leaveTypeService.listActiveEntities();

        int created = 0;
        for (StaffProfile profile : staff) {
            for (LeaveType leaveType : leaveTypes) {
                if (leaveBalanceRepository.existsByUserIdAndLeaveTypeIdAndYearAndDeletedFalse(
                        profile.getUserId(), leaveType.getId(), year)) {
                    continue;
                }
                LeaveBalance balance = LeaveBalance.builder()
                        .userId(profile.getUserId())
                        .leaveType(leaveType)
                        .year(year)
                        .totalDays(leaveType.getDaysPerYear())
                        .build();
                leaveBalanceRepository.save(balance);
                created++;
            }
        }

        auditLogWriter.write(AuditAction.CREATE, MODULE, "LeaveBalance", "year-" + year,
                "Initialized " + created + " leave balance(s) for " + year);
        log.info("LeaveBalanceService: initialized {} balance(s) for {} staff × {} leave type(s) in {}",
                created, staff.size(), leaveTypes.size(), year);

        return created;
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceDto> listByUserAndYear(UUID userId, int year) {
        return leaveBalanceRepository.findByUserIdAndYearAndDeletedFalse(userId, year).stream()
                .map(LeaveBalanceDto::from)
                .toList();
    }

    /** For LeaveRequestService — the managed entity, so consume()/release() can be persisted. */
    @Transactional(readOnly = true)
    public LeaveBalance getBalanceEntity(UUID userId, UUID leaveTypeId, int year) {
        return leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYearAndDeletedFalse(userId, leaveTypeId, year)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "LeaveBalance for user/leaveType/year", userId + "/" + leaveTypeId + "/" + year));
    }
}
