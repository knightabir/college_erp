package com.college.erp.module.placement.domain;

import java.math.BigDecimal;
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

/** INVENTED — see V016's header comment. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "placement_drives")
public class PlacementDrive extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** NULL = open to every course. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eligible_course_id")
    private Course eligibleCourse;

    @Column(name = "eligibility_min_cgpa", precision = 4, scale = 2)
    private BigDecimal eligibilityMinCgpa;

    @Column(name = "ctc_offered", precision = 12, scale = 2)
    private BigDecimal ctcOffered;

    @Column(name = "drive_date")
    private LocalDate driveDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DriveStatus status = DriveStatus.SCHEDULED;
}
