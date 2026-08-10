package com.college.erp.module.hr.service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.AccessDeniedException;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.hr.domain.LeaveBalance;
import com.college.erp.module.hr.domain.LeaveRequest;
import com.college.erp.module.hr.domain.LeaveRequestRepository;
import com.college.erp.module.hr.domain.LeaveRequestStatus;
import com.college.erp.module.hr.domain.LeaveType;
import com.college.erp.module.hr.dto.ApplyLeaveRequest;
import com.college.erp.module.hr.dto.LeaveRequestDto;
import com.college.erp.notification.NotificationRequest;
import com.college.erp.notification.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 03-srs.md [HR-003]/[HR-004], 05-api-documentation.md §10 "HR & Leave". See LeaveRequest's
 * javadoc for why approval is single-stage rather than the SRS's conceptual "HOD → HR Manager"
 * two-step chain.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private static final String MODULE = "HR";

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeService leaveTypeService;
    private final LeaveBalanceService leaveBalanceService;
    private final AuditLogWriter auditLogWriter;
    private final NotificationService notificationService;

    /** [HR-003]: "checks against available balance before submission." */
    @Transactional
    public LeaveRequestDto apply(ApplyLeaveRequest request, UUID userId) {
        if (request.getToDate().isBefore(request.getFromDate())) {
            throw new BusinessRuleViolationException("to_date cannot be before from_date");
        }
        LeaveType leaveType = leaveTypeService.getLeaveTypeEntity(request.getLeaveTypeId());
        int daysRequested = (int) ChronoUnit.DAYS.between(request.getFromDate(), request.getToDate()) + 1;
        int year = request.getFromDate().getYear();

        LeaveBalance balance = leaveBalanceService.getBalanceEntity(userId, leaveType.getId(), year);
        if (balance.remainingDays() < daysRequested) {
            throw new BusinessRuleViolationException(
                    "Insufficient leave balance: " + balance.remainingDays() + " day(s) remaining, "
                            + daysRequested + " requested");
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .userId(userId)
                .leaveType(leaveType)
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .daysRequested(daysRequested)
                .reason(request.getReason())
                .status(LeaveRequestStatus.PENDING)
                .build();
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "LeaveRequest", saved.getId().toString(),
                "Applied for " + daysRequested + " day(s) of " + leaveType.getName());

        return LeaveRequestDto.from(saved);
    }

    /** [HR-004]: "On final approval, the used_days count is incremented on the leave balance." */
    @Transactional
    public LeaveRequestDto approve(UUID id, String remarks, UUID reviewerUserId) {
        LeaveRequest leaveRequest = getPendingEntity(id);

        LeaveBalance balance = leaveBalanceService.getBalanceEntity(
                leaveRequest.getUserId(), leaveRequest.getLeaveType().getId(), leaveRequest.getFromDate().getYear());
        balance.consume(leaveRequest.getDaysRequested());

        leaveRequest.approve(reviewerUserId, remarks);
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "LeaveRequest", id.toString(),
                "Approved: " + remarks);
        notificationService.send(NotificationRequest.builder()
                .userId(leaveRequest.getUserId())
                .title("Leave Request Approved")
                .body("Your " + leaveRequest.getLeaveType().getName() + " leave ("
                        + leaveRequest.getFromDate() + " to " + leaveRequest.getToDate() + ") was approved.")
                .referenceType("LeaveRequest").referenceId(id.toString())
                .build());

        return LeaveRequestDto.from(saved);
    }

    @Transactional
    public LeaveRequestDto reject(UUID id, String remarks, UUID reviewerUserId) {
        LeaveRequest leaveRequest = getPendingEntity(id);

        leaveRequest.reject(reviewerUserId, remarks);
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "LeaveRequest", id.toString(),
                "Rejected: " + remarks);
        notificationService.send(NotificationRequest.builder()
                .userId(leaveRequest.getUserId())
                .title("Leave Request Rejected")
                .body("Your " + leaveRequest.getLeaveType().getName() + " leave ("
                        + leaveRequest.getFromDate() + " to " + leaveRequest.getToDate() + ") was rejected: "
                        + remarks)
                .referenceType("LeaveRequest").referenceId(id.toString())
                .build());

        return LeaveRequestDto.from(saved);
    }

    @Transactional
    public void cancel(UUID id, UUID requestingUserId) {
        LeaveRequest leaveRequest = getEntity(id);
        if (!leaveRequest.getUserId().equals(requestingUserId)) {
            throw new AccessDeniedException("This leave request does not belong to you");
        }
        if (leaveRequest.getStatus() == LeaveRequestStatus.APPROVED) {
            LeaveBalance balance = leaveBalanceService.getBalanceEntity(
                    leaveRequest.getUserId(), leaveRequest.getLeaveType().getId(),
                    leaveRequest.getFromDate().getYear());
            balance.release(leaveRequest.getDaysRequested());
        }

        leaveRequest.cancel();
        leaveRequestRepository.save(leaveRequest);
        auditLogWriter.write(AuditAction.UPDATE, MODULE, "LeaveRequest", id.toString(), "Cancelled by requester");
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> listByUser(UUID userId) {
        return leaveRequestRepository.findByUserIdAndDeletedFalseOrderByFromDateDesc(userId).stream()
                .map(LeaveRequestDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> listPending() {
        return leaveRequestRepository.findByStatusAndDeletedFalse(LeaveRequestStatus.PENDING).stream()
                .map(LeaveRequestDto::from)
                .toList();
    }

    private LeaveRequest getPendingEntity(UUID id) {
        LeaveRequest leaveRequest = getEntity(id);
        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new BusinessRuleViolationException("Leave request is not pending review");
        }
        return leaveRequest;
    }

    private LeaveRequest getEntity(UUID id) {
        return leaveRequestRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", id));
    }
}
