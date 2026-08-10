package com.college.erp.module.finance.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.AcademicYear;
import com.college.erp.module.academic.domain.Batch;
import com.college.erp.module.academic.domain.Course;
import com.college.erp.module.academic.domain.Term;
import com.college.erp.module.academic.service.AcademicYearService;
import com.college.erp.module.academic.service.BatchService;
import com.college.erp.module.academic.service.CourseService;
import com.college.erp.module.academic.service.TermService;
import com.college.erp.module.finance.domain.FeeInvoice;
import com.college.erp.module.finance.domain.FeeInvoiceRepository;
import com.college.erp.module.finance.domain.FeePayment;
import com.college.erp.module.finance.domain.FeePaymentRepository;
import com.college.erp.module.finance.domain.FeeStructure;
import com.college.erp.module.finance.domain.FinancialYear;
import com.college.erp.module.finance.domain.InvoiceStatus;
import com.college.erp.module.finance.dto.CollectPaymentRequest;
import com.college.erp.module.finance.dto.FeeInvoiceDto;
import com.college.erp.module.finance.dto.GenerateInvoiceRequest;
import com.college.erp.module.finance.dto.ReceiptDto;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.service.StudentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 03-srs.md [FIN-002]/[FIN-003]/[FIN-005], 05-api-documentation.md §7. */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeCollectionService {

    private static final String MODULE = "FINANCE";
    private static final List<InvoiceStatus> OUTSTANDING_STATUSES =
            List.of(InvoiceStatus.UNPAID, InvoiceStatus.PARTIAL, InvoiceStatus.OVERDUE);

    private final FeeInvoiceRepository feeInvoiceRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final StudentService studentService;
    private final CourseService courseService;
    private final BatchService batchService;
    private final AcademicYearService academicYearService;
    private final TermService termService;
    private final FeeStructureService feeStructureService;
    private final FinancialYearService financialYearService;
    private final LedgerService ledgerService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public FeeInvoiceDto generateInvoice(GenerateInvoiceRequest request) {
        Student student = studentService.getStudentEntity(request.getStudentId());
        Course course = courseService.getCourseEntity(request.getCourseId());
        Batch batch = batchService.getBatchEntity(request.getBatchId());
        AcademicYear academicYear = academicYearService.getAcademicYearEntity(request.getAcademicYearId());
        Term term = termService.getTermByAcademicYearAndNumber(academicYear.getId(), request.getTermNumber());

        if (feeInvoiceRepository.existsByStudentIdAndBatchIdAndTermNumberAndDeletedFalse(
                student.getId(), batch.getId(), request.getTermNumber())) {
            throw new DuplicateResourceException("Invoice already exists for this student/batch/term");
        }

        List<FeeStructure> structures =
                feeStructureService.resolveApplicable(course.getId(), batch.getId(), request.getTermNumber());
        if (structures.isEmpty()) {
            throw new BusinessRuleViolationException("No fee structure defined for this course/batch/term");
        }

        BigDecimal totalAmount = structures.stream()
                .map(FeeStructure::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int dueDateOffset = structures.stream()
                .map(FeeStructure::getDueDateOffsetDays)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);

        FeeInvoice invoice = FeeInvoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .student(student)
                .course(course)
                .batch(batch)
                .termNumber(request.getTermNumber())
                .academicYear(academicYear)
                .totalAmount(totalAmount)
                .netAmount(totalAmount)
                .dueDate(term.getStartDate().plusDays(dueDateOffset))
                .build();

        FeeInvoice saved = feeInvoiceRepository.save(invoice);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "FeeInvoice", saved.getId().toString(),
                "Generated invoice " + saved.getInvoiceNumber() + " for " + student.getEnrollmentNumber());
        log.info("Invoice generated: [{}] for [{}], amount {}", saved.getInvoiceNumber(),
                student.getEnrollmentNumber(), totalAmount);

        return FeeInvoiceDto.from(saved);
    }

    @Transactional
    public ReceiptDto collectPayment(UUID invoiceId, CollectPaymentRequest request, UUID collectedByUserId) {
        FeeInvoice invoice = getEntity(invoiceId);

        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessRuleViolationException("Invoice is " + invoice.getStatus() + " — cannot collect payment");
        }

        applyLateFeeIfOverdue(invoice);

        BigDecimal balance = invoice.balanceDue();
        if (request.getAmount().compareTo(balance) > 0) {
            throw new BusinessRuleViolationException("Amount exceeds the outstanding balance of " + balance);
        }

        FeePayment payment = FeePayment.builder()
                .invoice(invoice)
                .receiptNumber(generateReceiptNumber())
                .amount(request.getAmount())
                .paymentMode(request.getPaymentMode())
                .transactionReference(request.getTransactionReference())
                .collectedByUserId(collectedByUserId)
                .paymentTimestamp(Instant.now())
                .build();
        FeePayment savedPayment = feePaymentRepository.save(payment);

        invoice.setPaidAmount(invoice.getPaidAmount().add(request.getAmount()));
        invoice.setStatus(invoice.getPaidAmount().compareTo(invoice.getNetAmount()) >= 0
                ? InvoiceStatus.PAID : InvoiceStatus.PARTIAL);
        feeInvoiceRepository.save(invoice);

        FinancialYear financialYear = financialYearService.getCurrentEntity();
        ledgerService.postFeeCollection(savedPayment, invoice, financialYear);

        auditLogWriter.write(AuditAction.FEE_COLLECTED, MODULE, "FeeInvoice", invoiceId.toString(),
                "Collected " + request.getAmount() + " via " + request.getPaymentMode()
                        + " — receipt " + savedPayment.getReceiptNumber());
        log.info("Fee collected: invoice [{}] amount {} receipt [{}]",
                invoice.getInvoiceNumber(), request.getAmount(), savedPayment.getReceiptNumber());

        return ReceiptDto.from(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<FeeInvoiceDto> getStudentInvoices(UUID studentId) {
        return feeInvoiceRepository.findByStudentIdAndDeletedFalse(studentId).stream()
                .map(FeeInvoiceDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeeInvoiceDto> getOutstanding(UUID studentId) {
        return feeInvoiceRepository.findByStudentIdAndStatusInAndDeletedFalse(studentId, OUTSTANDING_STATUSES)
                .stream()
                .map(FeeInvoiceDto::from)
                .toList();
    }

    /** For FeeDueDateReminderJob — invoices due soon, as managed entities (the job needs
     *  invoice.getStudent().getUserId(), not just a DTO). Kept here rather than the job injecting
     *  FeeInvoiceRepository directly (07-coding-standards.md §4.4: no cross-module repository access). */
    @Transactional(readOnly = true)
    public List<FeeInvoice> listDueSoonEntities(List<InvoiceStatus> statuses, LocalDate from, LocalDate to) {
        return feeInvoiceRepository.findByStatusInAndDueDateBetweenAndDeletedFalse(statuses, from, to);
    }

    @Transactional(readOnly = true)
    public List<ReceiptDto> getPayments(UUID invoiceId) {
        return feePaymentRepository.findByInvoiceIdAndDeletedFalse(invoiceId).stream()
                .map(ReceiptDto::from)
                .toList();
    }

    /** 03-srs.md [FIN-005]: days_late × late_fee_per_day, computed once (guarded by lateFee still
     *  being zero) so repeated partial payments after the due date don't keep re-adding it. */
    private void applyLateFeeIfOverdue(FeeInvoice invoice) {
        LocalDate today = LocalDate.now();
        if (!today.isAfter(invoice.getDueDate()) || invoice.getLateFee().signum() != 0) {
            return;
        }

        List<FeeStructure> structures = feeStructureService.resolveApplicable(
                invoice.getCourse().getId(), invoice.getBatch().getId(), invoice.getTermNumber());
        BigDecimal maxLateFeePerDay = structures.stream()
                .map(FeeStructure::getLateFeePerDay)
                .filter(java.util.Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        if (maxLateFeePerDay.signum() <= 0) {
            return;
        }

        long daysLate = ChronoUnit.DAYS.between(invoice.getDueDate(), today);
        BigDecimal lateFee = maxLateFeePerDay.multiply(BigDecimal.valueOf(daysLate));

        invoice.setLateFee(lateFee);
        invoice.setNetAmount(invoice.getTotalAmount().subtract(invoice.getDiscountAmount()).add(lateFee));
    }

    private String generateInvoiceNumber() {
        return "INV-" + LocalDate.now().getYear() + "-" + shortId();
    }

    private String generateReceiptNumber() {
        return "RCP-" + LocalDate.now().getYear() + "-" + shortId();
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private FeeInvoice getEntity(UUID id) {
        return feeInvoiceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("FeeInvoice", id));
    }
}
