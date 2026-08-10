package com.college.erp.module.admissions.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.college.erp.common.domain.BaseEntity;
import com.college.erp.module.academic.domain.AcademicYear;
import com.college.erp.module.academic.domain.Course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** INVENTED — see V015__create_admissions_tables.sql's header comment. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "applications")
public class Application extends BaseEntity {

    @Column(name = "application_number", nullable = false, unique = true, length = 30)
    private String applicationNumber;

    @Column(name = "applicant_name", nullable = false, length = 200)
    private String applicantName;

    @Column(name = "applicant_email", nullable = false, length = 150)
    private String applicantEmail;

    @Column(name = "applicant_phone", length = 20)
    private String applicantPhone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 10)
    private String gender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20)
    private ApplicationCategory category;

    @Column(name = "qualifying_exam_score", precision = 5, scale = 2)
    private BigDecimal qualifyingExamScore;

    @Column(name = "qualifying_exam_details", columnDefinition = "TEXT")
    private String qualifyingExamDetails;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @Builder.Default
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt = Instant.now();

    public void transitionTo(ApplicationStatus newStatus) {
        this.status = newStatus;
    }
}
