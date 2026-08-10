package com.college.erp.module.finance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.module.academic.domain.AcademicYear;
import com.college.erp.module.academic.domain.Batch;
import com.college.erp.module.academic.domain.Course;
import com.college.erp.module.academic.domain.CourseLevel;
import com.college.erp.module.academic.domain.Term;
import com.college.erp.module.academic.service.AcademicYearService;
import com.college.erp.module.academic.service.BatchService;
import com.college.erp.module.academic.service.CourseService;
import com.college.erp.module.academic.service.TermService;
import com.college.erp.module.finance.domain.FeeHead;
import com.college.erp.module.finance.domain.FeeInvoice;
import com.college.erp.module.finance.domain.FeeInvoiceRepository;
import com.college.erp.module.finance.domain.FeePaymentRepository;
import com.college.erp.module.finance.domain.FeeStructure;
import com.college.erp.module.finance.domain.FinancialYear;
import com.college.erp.module.finance.domain.InvoiceStatus;
import com.college.erp.module.finance.domain.PaymentMode;
import com.college.erp.module.finance.dto.CollectPaymentRequest;
import com.college.erp.module.finance.dto.FeeInvoiceDto;
import com.college.erp.module.finance.dto.GenerateInvoiceRequest;
import com.college.erp.module.finance.dto.ReceiptDto;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.service.StudentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 03-srs.md [FIN-002]/[FIN-003]. Named to match 08-system-architecture.md's test tree. */
@ExtendWith(MockitoExtension.class)
class FeeCollectionServiceTest {

    @Mock
    private FeeInvoiceRepository feeInvoiceRepository;
    @Mock
    private FeePaymentRepository feePaymentRepository;
    @Mock
    private StudentService studentService;
    @Mock
    private CourseService courseService;
    @Mock
    private BatchService batchService;
    @Mock
    private AcademicYearService academicYearService;
    @Mock
    private TermService termService;
    @Mock
    private FeeStructureService feeStructureService;
    @Mock
    private FinancialYearService financialYearService;
    @Mock
    private LedgerService ledgerService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private FeeCollectionService feeCollectionService;

    private Student buildStudent() {
        Student student = Student.builder().enrollmentNumber("2024CSE001").firstName("Arjun").lastName("Kumar").build();
        student.setId(UUID.randomUUID());
        return student;
    }

    private Course buildCourse() {
        Course course = Course.builder().name("BTech CSE").code("BTCSE").level(CourseLevel.UG)
                .durationYears(4).intakeCapacity(60).build();
        course.setId(UUID.randomUUID());
        return course;
    }

    private Batch buildBatch() {
        Batch batch = Batch.builder().label("CSE 2024").intakeYear(2024).build();
        batch.setId(UUID.randomUUID());
        return batch;
    }

    private AcademicYear buildAcademicYear() {
        AcademicYear year = AcademicYear.builder().label("2024-2025").startYear(2024).endYear(2025).build();
        year.setId(UUID.randomUUID());
        return year;
    }

    private Term buildTerm() {
        Term term = Term.builder().name("Semester I").termNumber(1).startDate(LocalDate.of(2024, 8, 1)).build();
        term.setId(UUID.randomUUID());
        return term;
    }

    private GenerateInvoiceRequest buildInvoiceRequest(Student s, Course c, Batch b, AcademicYear y) {
        GenerateInvoiceRequest request = new GenerateInvoiceRequest();
        request.setStudentId(s.getId());
        request.setCourseId(c.getId());
        request.setBatchId(b.getId());
        request.setAcademicYearId(y.getId());
        request.setTermNumber(1);
        return request;
    }

    @Test
    @DisplayName("generateInvoice() — duplicate invoice for student/batch/term is rejected")
    void generateInvoice_duplicate_throws() {
        // ARRANGE
        feeCollectionService = new FeeCollectionService(feeInvoiceRepository, feePaymentRepository, studentService,
                courseService, batchService, academicYearService, termService, feeStructureService,
                financialYearService, ledgerService, auditLogWriter);
        Student student = buildStudent();
        Course course = buildCourse();
        Batch batch = buildBatch();
        AcademicYear year = buildAcademicYear();
        Term term = buildTerm();

        when(studentService.getStudentEntity(student.getId())).thenReturn(student);
        when(courseService.getCourseEntity(course.getId())).thenReturn(course);
        when(batchService.getBatchEntity(batch.getId())).thenReturn(batch);
        when(academicYearService.getAcademicYearEntity(year.getId())).thenReturn(year);
        when(termService.getTermByAcademicYearAndNumber(year.getId(), 1)).thenReturn(term);
        when(feeInvoiceRepository.existsByStudentIdAndBatchIdAndTermNumberAndDeletedFalse(
                student.getId(), batch.getId(), 1)).thenReturn(true);

        // ACT / ASSERT
        assertThatThrownBy(() -> feeCollectionService.generateInvoice(
                buildInvoiceRequest(student, course, batch, year)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("generateInvoice() — no applicable fee structure is a 422, not a 500")
    void generateInvoice_noFeeStructure_throws() {
        // ARRANGE
        feeCollectionService = new FeeCollectionService(feeInvoiceRepository, feePaymentRepository, studentService,
                courseService, batchService, academicYearService, termService, feeStructureService,
                financialYearService, ledgerService, auditLogWriter);
        Student student = buildStudent();
        Course course = buildCourse();
        Batch batch = buildBatch();
        AcademicYear year = buildAcademicYear();
        Term term = buildTerm();

        when(studentService.getStudentEntity(student.getId())).thenReturn(student);
        when(courseService.getCourseEntity(course.getId())).thenReturn(course);
        when(batchService.getBatchEntity(batch.getId())).thenReturn(batch);
        when(academicYearService.getAcademicYearEntity(year.getId())).thenReturn(year);
        when(termService.getTermByAcademicYearAndNumber(year.getId(), 1)).thenReturn(term);
        when(feeInvoiceRepository.existsByStudentIdAndBatchIdAndTermNumberAndDeletedFalse(
                student.getId(), batch.getId(), 1)).thenReturn(false);
        when(feeStructureService.resolveApplicable(course.getId(), batch.getId(), 1)).thenReturn(List.of());

        // ACT / ASSERT
        assertThatThrownBy(() -> feeCollectionService.generateInvoice(
                buildInvoiceRequest(student, course, batch, year)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("generateInvoice() — sums all applicable fee structures into total_amount")
    void generateInvoice_success_sumsStructures() {
        // ARRANGE
        feeCollectionService = new FeeCollectionService(feeInvoiceRepository, feePaymentRepository, studentService,
                courseService, batchService, academicYearService, termService, feeStructureService,
                financialYearService, ledgerService, auditLogWriter);
        Student student = buildStudent();
        Course course = buildCourse();
        Batch batch = buildBatch();
        AcademicYear year = buildAcademicYear();
        Term term = buildTerm();

        FeeHead tuition = FeeHead.builder().code("TUITION").name("Tuition Fee").build();
        FeeHead library = FeeHead.builder().code("LIBRARY").name("Library Fee").build();
        FeeStructure s1 = FeeStructure.builder().course(course).feeHead(tuition).termNumber(1)
                .amount(BigDecimal.valueOf(40000)).dueDateOffsetDays(30).build();
        FeeStructure s2 = FeeStructure.builder().course(course).feeHead(library).termNumber(1)
                .amount(BigDecimal.valueOf(5000)).dueDateOffsetDays(15).build();

        when(studentService.getStudentEntity(student.getId())).thenReturn(student);
        when(courseService.getCourseEntity(course.getId())).thenReturn(course);
        when(batchService.getBatchEntity(batch.getId())).thenReturn(batch);
        when(academicYearService.getAcademicYearEntity(year.getId())).thenReturn(year);
        when(termService.getTermByAcademicYearAndNumber(year.getId(), 1)).thenReturn(term);
        when(feeInvoiceRepository.existsByStudentIdAndBatchIdAndTermNumberAndDeletedFalse(
                student.getId(), batch.getId(), 1)).thenReturn(false);
        when(feeStructureService.resolveApplicable(course.getId(), batch.getId(), 1)).thenReturn(List.of(s1, s2));
        when(feeInvoiceRepository.save(any(FeeInvoice.class))).thenAnswer(inv -> {
            FeeInvoice i = inv.getArgument(0);
            i.setId(UUID.randomUUID());
            return i;
        });

        // ACT
        FeeInvoiceDto result = feeCollectionService.generateInvoice(buildInvoiceRequest(student, course, batch, year));

        // ASSERT — 40000 + 5000 = 45000; due date = term start (Aug 1) + max(30,15) days
        assertThat(result.getTotalAmount()).isEqualByComparingTo("45000");
        assertThat(result.getNetAmount()).isEqualByComparingTo("45000");
        assertThat(result.getDueDate()).isEqualTo(LocalDate.of(2024, 8, 31));
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
    }

    private FeeInvoice buildInvoice(BigDecimal netAmount, BigDecimal paidAmount, InvoiceStatus status) {
        Student student = buildStudent();
        Course course = buildCourse();
        Batch batch = buildBatch();
        AcademicYear year = buildAcademicYear();
        FeeInvoice invoice = FeeInvoice.builder()
                .invoiceNumber("INV-2026-00000001").student(student).course(course).batch(batch)
                .termNumber(1).academicYear(year)
                .totalAmount(netAmount).netAmount(netAmount).paidAmount(paidAmount)
                .dueDate(LocalDate.now().plusDays(30)).status(status).build();
        invoice.setId(UUID.randomUUID());
        return invoice;
    }

    @Test
    @DisplayName("collectPayment() — amount exceeding the outstanding balance is rejected")
    void collectPayment_exceedsBalance_throws() {
        // ARRANGE
        feeCollectionService = new FeeCollectionService(feeInvoiceRepository, feePaymentRepository, studentService,
                courseService, batchService, academicYearService, termService, feeStructureService,
                financialYearService, ledgerService, auditLogWriter);
        FeeInvoice invoice = buildInvoice(BigDecimal.valueOf(45000), BigDecimal.valueOf(20000), InvoiceStatus.PARTIAL);
        when(feeInvoiceRepository.findByIdAndDeletedFalse(invoice.getId())).thenReturn(Optional.of(invoice));

        CollectPaymentRequest request = new CollectPaymentRequest();
        request.setAmount(BigDecimal.valueOf(30000)); // balance is only 25000
        request.setPaymentMode(PaymentMode.CASH);

        // ACT / ASSERT
        assertThatThrownBy(() -> feeCollectionService.collectPayment(invoice.getId(), request, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("collectPayment() — an already-PAID invoice rejects further payment")
    void collectPayment_alreadyPaid_throws() {
        // ARRANGE
        feeCollectionService = new FeeCollectionService(feeInvoiceRepository, feePaymentRepository, studentService,
                courseService, batchService, academicYearService, termService, feeStructureService,
                financialYearService, ledgerService, auditLogWriter);
        FeeInvoice invoice = buildInvoice(BigDecimal.valueOf(45000), BigDecimal.valueOf(45000), InvoiceStatus.PAID);
        when(feeInvoiceRepository.findByIdAndDeletedFalse(invoice.getId())).thenReturn(Optional.of(invoice));

        CollectPaymentRequest request = new CollectPaymentRequest();
        request.setAmount(BigDecimal.valueOf(100));
        request.setPaymentMode(PaymentMode.CASH);

        // ACT / ASSERT
        assertThatThrownBy(() -> feeCollectionService.collectPayment(invoice.getId(), request, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("collectPayment() — full balance payment moves invoice to PAID and posts to the ledger")
    void collectPayment_fullBalance_marksInvoicePaidAndPostsLedger() {
        // ARRANGE
        feeCollectionService = new FeeCollectionService(feeInvoiceRepository, feePaymentRepository, studentService,
                courseService, batchService, academicYearService, termService, feeStructureService,
                financialYearService, ledgerService, auditLogWriter);
        FeeInvoice invoice = buildInvoice(BigDecimal.valueOf(45000), BigDecimal.valueOf(20000), InvoiceStatus.PARTIAL);
        FinancialYear year = FinancialYear.builder().label("2026-2027").build();
        year.setId(UUID.randomUUID());

        when(feeInvoiceRepository.findByIdAndDeletedFalse(invoice.getId())).thenReturn(Optional.of(invoice));
        when(feePaymentRepository.save(any())).thenAnswer(inv -> {
            var p = inv.getArgument(0, com.college.erp.module.finance.domain.FeePayment.class);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(feeInvoiceRepository.save(any(FeeInvoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(financialYearService.getCurrentEntity()).thenReturn(year);
        doNothing().when(ledgerService).postFeeCollection(any(), any(), any());

        CollectPaymentRequest request = new CollectPaymentRequest();
        request.setAmount(BigDecimal.valueOf(25000)); // exactly the balance
        request.setPaymentMode(PaymentMode.ONLINE_UPI);
        request.setTransactionReference("TXN123");

        // ACT
        ReceiptDto receipt = feeCollectionService.collectPayment(invoice.getId(), request, UUID.randomUUID());

        // ASSERT
        assertThat(receipt.getAmount()).isEqualByComparingTo("25000");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.getPaidAmount()).isEqualByComparingTo("45000");
        verify(ledgerService).postFeeCollection(any(), eq(invoice), eq(year));
    }
}
