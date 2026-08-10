package com.college.erp.module.finance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.college.erp.common.domain.BaseEntity;
import com.college.erp.module.academic.domain.AcademicYear;
import com.college.erp.module.academic.domain.Batch;
import com.college.erp.module.academic.domain.Course;
import com.college.erp.module.student.domain.Student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 04-database-schema.md §10.5, 03-srs.md [FIN-002]. The (student_id, batch_id, term_number)
 * unique constraint isn't in the doc's own DDL — only invoice_number is documented as unique —
 * added as defense-in-depth for the FIN-002 duplicate-invoice rule (05-api-documentation.md §7
 * already documents a 409 for this case; this makes it impossible to bypass at the DB level too).
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "fee_invoices",
    indexes = @Index(name = "idx_invoice_student", columnList = "student_id"),
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "batch_id", "term_number"})
)
public class FeeInvoice extends BaseEntity {

    @Column(name = "invoice_number", nullable = false, unique = true, length = 30)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(name = "term_number", nullable = false)
    private int termNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Builder.Default
    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "late_fee", nullable = false, precision = 14, scale = 2)
    private BigDecimal lateFee = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal netAmount;

    @Builder.Default
    @Column(name = "paid_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    public BigDecimal balanceDue() {
        return netAmount.subtract(paidAmount);
    }
}
