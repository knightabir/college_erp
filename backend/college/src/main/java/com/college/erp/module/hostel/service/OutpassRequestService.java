package com.college.erp.module.hostel.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.AccessDeniedException;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.hostel.domain.OutpassRequest;
import com.college.erp.module.hostel.domain.OutpassRequestRepository;
import com.college.erp.module.hostel.domain.OutpassStatus;
import com.college.erp.module.hostel.dto.CreateOutpassRequest;
import com.college.erp.module.hostel.dto.OutpassRequestDto;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.service.StudentService;
import com.college.erp.notification.NotificationRequest;
import com.college.erp.notification.NotificationService;

import lombok.RequiredArgsConstructor;

/**
 * 04-database-schema.md §14.1 — see the module's CLAUDE.md deviations note. apply/approve/reject/
 * cancel deliberately mirrors module.hr's LeaveRequestService shape (same PENDING → APPROVED/
 * REJECTED/CANCELLED lifecycle, single-stage review by whoever holding the right role acts first)
 * since there's no dedicated outpass requirement to follow instead.
 */
@Service
@RequiredArgsConstructor
public class OutpassRequestService {

    private static final String MODULE = "HOSTEL";

    private final OutpassRequestRepository outpassRequestRepository;
    private final StudentService studentService;
    private final AuditLogWriter auditLogWriter;
    private final NotificationService notificationService;

    @Transactional
    public OutpassRequestDto apply(CreateOutpassRequest request, UUID studentId) {
        if (!request.getToDatetime().isAfter(request.getFromDatetime())) {
            throw new BusinessRuleViolationException("to_datetime must be after from_datetime");
        }
        Student student = studentService.getStudentEntity(studentId);

        OutpassRequest outpass = OutpassRequest.builder()
                .student(student)
                .fromDatetime(request.getFromDatetime())
                .toDatetime(request.getToDatetime())
                .reason(request.getReason())
                .destination(request.getDestination())
                .status(OutpassStatus.PENDING)
                .build();
        OutpassRequest saved = outpassRequestRepository.save(outpass);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "OutpassRequest", saved.getId().toString(),
                "Requested outpass for " + student.getFullName());

        return OutpassRequestDto.from(saved);
    }

    @Transactional
    public OutpassRequestDto approve(UUID id, UUID approverUserId) {
        OutpassRequest outpass = getPendingEntity(id);
        outpass.approve(approverUserId);
        OutpassRequest saved = outpassRequestRepository.save(outpass);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "OutpassRequest", id.toString(), "Approved");
        notifyStudent(outpass, "Outpass Approved",
                "Your outpass request (" + outpass.getFromDatetime() + " to " + outpass.getToDatetime()
                        + ") was approved.");

        return OutpassRequestDto.from(saved);
    }

    @Transactional
    public OutpassRequestDto reject(UUID id, UUID approverUserId) {
        OutpassRequest outpass = getPendingEntity(id);
        outpass.reject(approverUserId);
        OutpassRequest saved = outpassRequestRepository.save(outpass);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "OutpassRequest", id.toString(), "Rejected");
        notifyStudent(outpass, "Outpass Rejected",
                "Your outpass request (" + outpass.getFromDatetime() + " to " + outpass.getToDatetime()
                        + ") was rejected.");

        return OutpassRequestDto.from(saved);
    }

    @Transactional
    public void cancel(UUID id, UUID requestingStudentUserId) {
        OutpassRequest outpass = getEntity(id);
        if (!outpass.getStudent().getUserId().equals(requestingStudentUserId)) {
            throw new AccessDeniedException("This outpass request does not belong to you");
        }
        outpass.cancel();
        outpassRequestRepository.save(outpass);
        auditLogWriter.write(AuditAction.UPDATE, MODULE, "OutpassRequest", id.toString(), "Cancelled by requester");
    }

    @Transactional(readOnly = true)
    public List<OutpassRequestDto> listByStudent(UUID studentId) {
        return outpassRequestRepository.findByStudentIdAndDeletedFalseOrderByFromDatetimeDesc(studentId).stream()
                .map(OutpassRequestDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OutpassRequestDto> listPending() {
        return outpassRequestRepository.findByStatusAndDeletedFalse(OutpassStatus.PENDING).stream()
                .map(OutpassRequestDto::from)
                .toList();
    }

    private void notifyStudent(OutpassRequest outpass, String title, String body) {
        if (outpass.getStudent().getUserId() == null) {
            return;
        }
        notificationService.send(NotificationRequest.builder()
                .userId(outpass.getStudent().getUserId())
                .title(title).body(body)
                .referenceType("OutpassRequest").referenceId(outpass.getId().toString())
                .build());
    }

    private OutpassRequest getPendingEntity(UUID id) {
        OutpassRequest outpass = getEntity(id);
        if (outpass.getStatus() != OutpassStatus.PENDING) {
            throw new BusinessRuleViolationException("Outpass request is not pending review");
        }
        return outpass;
    }

    private OutpassRequest getEntity(UUID id) {
        return outpassRequestRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("OutpassRequest", id));
    }
}
