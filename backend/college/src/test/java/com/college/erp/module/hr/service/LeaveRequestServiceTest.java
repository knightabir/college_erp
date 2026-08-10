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
import com.college.erp.common.exception.AccessDeniedException;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.module.hr.domain.LeaveBalance;
import com.college.erp.module.hr.domain.LeaveRequest;
import com.college.erp.module.hr.domain.LeaveRequestRepository;
import com.college.erp.module.hr.domain.LeaveRequestStatus;
import com.college.erp.module.hr.domain.LeaveType;
import com.college.erp.module.hr.dto.ApplyLeaveRequest;
import com.college.erp.module.hr.dto.LeaveRequestDto;
import com.college.erp.notification.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 03-srs.md [HR-003]/[HR-004]. Named to match the module's other *ServiceTest files. */
@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private LeaveTypeService leaveTypeService;
    @Mock
    private LeaveBalanceService leaveBalanceService;
    @Mock
    private AuditLogWriter auditLogWriter;
    @Mock
    private NotificationService notificationService;

    private LeaveRequestService leaveRequestService;

    private LeaveType buildLeaveType() {
        LeaveType leaveType = LeaveType.builder().code("CL").name("Casual Leave").daysPerYear(12).build();
        leaveType.setId(UUID.randomUUID());
        return leaveType;
    }

    private LeaveBalance buildBalance(LeaveType leaveType, int total, int used) {
        return LeaveBalance.builder().leaveType(leaveType).year(2026).totalDays(total).usedDays(used).build();
    }

    @Test
    @DisplayName("apply() — 3-day request (inclusive) against a sufficient balance succeeds")
    void apply_sufficientBalance_succeeds() {
        // ARRANGE
        leaveRequestService = new LeaveRequestService(leaveRequestRepository, leaveTypeService,
                leaveBalanceService, auditLogWriter, notificationService);
        UUID userId = UUID.randomUUID();
        LeaveType leaveType = buildLeaveType();
        when(leaveTypeService.getLeaveTypeEntity(leaveType.getId())).thenReturn(leaveType);
        when(leaveBalanceService.getBalanceEntity(userId, leaveType.getId(), 2026))
                .thenReturn(buildBalance(leaveType, 12, 0));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> {
            LeaveRequest request = inv.getArgument(0);
            request.setId(UUID.randomUUID());
            return request;
        });

        ApplyLeaveRequest request = new ApplyLeaveRequest();
        request.setLeaveTypeId(leaveType.getId());
        request.setFromDate(LocalDate.of(2026, 7, 1));
        request.setToDate(LocalDate.of(2026, 7, 3));
        request.setReason("Family function");

        // ACT — 1st, 2nd, 3rd July = 3 days inclusive
        LeaveRequestDto dto = leaveRequestService.apply(request, userId);

        // ASSERT
        assertThat(dto.getDaysRequested()).isEqualTo(3);
        assertThat(dto.getStatus()).isEqualTo(LeaveRequestStatus.PENDING);
    }

    @Test
    @DisplayName("apply() — insufficient balance is rejected (05-api-documentation.md §10: 422)")
    void apply_insufficientBalance_throws() {
        // ARRANGE
        leaveRequestService = new LeaveRequestService(leaveRequestRepository, leaveTypeService,
                leaveBalanceService, auditLogWriter, notificationService);
        UUID userId = UUID.randomUUID();
        LeaveType leaveType = buildLeaveType();
        when(leaveTypeService.getLeaveTypeEntity(leaveType.getId())).thenReturn(leaveType);
        when(leaveBalanceService.getBalanceEntity(userId, leaveType.getId(), 2026))
                .thenReturn(buildBalance(leaveType, 12, 11)); // only 1 day remaining

        ApplyLeaveRequest request = new ApplyLeaveRequest();
        request.setLeaveTypeId(leaveType.getId());
        request.setFromDate(LocalDate.of(2026, 7, 1));
        request.setToDate(LocalDate.of(2026, 7, 3)); // 3 days requested > 1 remaining

        // ACT / ASSERT
        assertThatThrownBy(() -> leaveRequestService.apply(request, userId))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("apply() — to_date before from_date is rejected")
    void apply_invalidDateRange_throws() {
        // ARRANGE
        leaveRequestService = new LeaveRequestService(leaveRequestRepository, leaveTypeService,
                leaveBalanceService, auditLogWriter, notificationService);
        ApplyLeaveRequest request = new ApplyLeaveRequest();
        request.setLeaveTypeId(UUID.randomUUID());
        request.setFromDate(LocalDate.of(2026, 7, 5));
        request.setToDate(LocalDate.of(2026, 7, 1));

        // ACT / ASSERT
        assertThatThrownBy(() -> leaveRequestService.apply(request, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("approve() — increments used_days on the leave balance [HR-004]")
    void approve_incrementsUsedDays() {
        // ARRANGE
        leaveRequestService = new LeaveRequestService(leaveRequestRepository, leaveTypeService,
                leaveBalanceService, auditLogWriter, notificationService);
        UUID userId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        LeaveType leaveType = buildLeaveType();
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .userId(userId).leaveType(leaveType)
                .fromDate(LocalDate.of(2026, 7, 1)).toDate(LocalDate.of(2026, 7, 3))
                .daysRequested(3).status(LeaveRequestStatus.PENDING).build();
        UUID requestId = UUID.randomUUID();
        leaveRequest.setId(requestId);
        LeaveBalance balance = buildBalance(leaveType, 12, 0);
        when(leaveRequestRepository.findByIdAndDeletedFalse(requestId)).thenReturn(Optional.of(leaveRequest));
        when(leaveBalanceService.getBalanceEntity(userId, leaveType.getId(), 2026)).thenReturn(balance);
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        LeaveRequestDto dto = leaveRequestService.approve(requestId, "Approved", reviewerId);

        // ASSERT
        assertThat(dto.getStatus()).isEqualTo(LeaveRequestStatus.APPROVED);
        assertThat(dto.getReviewedBy()).isEqualTo(reviewerId);
        assertThat(balance.getUsedDays()).isEqualTo(3);
    }

    @Test
    @DisplayName("approve() — a non-pending request cannot be approved again")
    void approve_notPending_throws() {
        // ARRANGE
        leaveRequestService = new LeaveRequestService(leaveRequestRepository, leaveTypeService,
                leaveBalanceService, auditLogWriter, notificationService);
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .userId(UUID.randomUUID()).leaveType(buildLeaveType())
                .fromDate(LocalDate.now()).toDate(LocalDate.now()).daysRequested(1)
                .status(LeaveRequestStatus.REJECTED).build();
        UUID requestId = UUID.randomUUID();
        leaveRequest.setId(requestId);
        when(leaveRequestRepository.findByIdAndDeletedFalse(requestId)).thenReturn(Optional.of(leaveRequest));

        // ACT / ASSERT
        assertThatThrownBy(() -> leaveRequestService.approve(requestId, "x", UUID.randomUUID()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("cancel() — only the requester may cancel their own request")
    void cancel_notOwner_throws() {
        // ARRANGE
        leaveRequestService = new LeaveRequestService(leaveRequestRepository, leaveTypeService,
                leaveBalanceService, auditLogWriter, notificationService);
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .userId(UUID.randomUUID()).leaveType(buildLeaveType())
                .fromDate(LocalDate.now()).toDate(LocalDate.now()).daysRequested(1)
                .status(LeaveRequestStatus.PENDING).build();
        UUID requestId = UUID.randomUUID();
        leaveRequest.setId(requestId);
        when(leaveRequestRepository.findByIdAndDeletedFalse(requestId)).thenReturn(Optional.of(leaveRequest));

        // ACT / ASSERT
        assertThatThrownBy(() -> leaveRequestService.cancel(requestId, UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("cancel() — cancelling an already-approved request releases the consumed balance")
    void cancel_approvedRequest_releasesBalance() {
        // ARRANGE
        leaveRequestService = new LeaveRequestService(leaveRequestRepository, leaveTypeService,
                leaveBalanceService, auditLogWriter, notificationService);
        UUID userId = UUID.randomUUID();
        LeaveType leaveType = buildLeaveType();
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .userId(userId).leaveType(leaveType)
                .fromDate(LocalDate.of(2026, 7, 1)).toDate(LocalDate.of(2026, 7, 3))
                .daysRequested(3).status(LeaveRequestStatus.APPROVED).build();
        UUID requestId = UUID.randomUUID();
        leaveRequest.setId(requestId);
        LeaveBalance balance = buildBalance(leaveType, 12, 3);
        when(leaveRequestRepository.findByIdAndDeletedFalse(requestId)).thenReturn(Optional.of(leaveRequest));
        when(leaveBalanceService.getBalanceEntity(userId, leaveType.getId(), 2026)).thenReturn(balance);

        // ACT
        leaveRequestService.cancel(requestId, userId);

        // ASSERT
        assertThat(leaveRequest.getStatus()).isEqualTo(LeaveRequestStatus.CANCELLED);
        assertThat(balance.getUsedDays()).isEqualTo(0);
    }
}
