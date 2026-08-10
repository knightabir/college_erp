package com.college.erp.module.examination.domain;

import java.time.Instant;

import com.college.erp.common.domain.BaseEntity;
import com.college.erp.module.student.domain.Student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 04-database-schema.md §9.2, 03-srs.md [EXAM-002]. "Seating info" mentioned in the GET
 * .../hall-tickets/{studentId} response example (05-api-documentation.md §6) has no backing table
 * anywhere (SeatingAllocation is undocumented — see V006 migration header) — HallTicketDto omits it.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "hall_tickets",
    indexes = {
        @Index(name = "idx_hall_ticket_exam", columnList = "exam_id"),
        @Index(name = "idx_hall_ticket_student", columnList = "student_id")
    },
    uniqueConstraints = @UniqueConstraint(columnNames = {"exam_id", "student_id"})
)
public class HallTicket extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "hall_ticket_number", nullable = false, unique = true, length = 30)
    private String hallTicketNumber;

    @Builder.Default
    @Column(name = "is_eligible", nullable = false)
    private boolean eligible = true;

    @Column(name = "ineligibility_reason", columnDefinition = "TEXT")
    private String ineligibilityReason;

    @Column(name = "issued_at")
    private Instant issuedAt;
}
