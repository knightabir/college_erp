package com.college.erp.module.admissions.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.admissions.domain.Application;
import com.college.erp.module.admissions.domain.ApplicationCategory;
import com.college.erp.module.admissions.domain.ApplicationStatus;

import lombok.Builder;
import lombok.Getter;

/** INVENTED — see V015__create_admissions_tables.sql's header comment. */
@Getter
@Builder
public class ApplicationDto {

    private UUID id;
    private String applicationNumber;
    private String applicantName;
    private String applicantEmail;
    private String applicantPhone;
    private LocalDate dateOfBirth;
    private String gender;
    private UUID courseId;
    private String courseName;
    private UUID academicYearId;
    private ApplicationCategory category;
    private BigDecimal qualifyingExamScore;
    private String qualifyingExamDetails;
    private ApplicationStatus status;
    private Instant submittedAt;

    public static ApplicationDto from(Application application) {
        return ApplicationDto.builder()
                .id(application.getId())
                .applicationNumber(application.getApplicationNumber())
                .applicantName(application.getApplicantName())
                .applicantEmail(application.getApplicantEmail())
                .applicantPhone(application.getApplicantPhone())
                .dateOfBirth(application.getDateOfBirth())
                .gender(application.getGender())
                .courseId(application.getCourse().getId())
                .courseName(application.getCourse().getName())
                .academicYearId(application.getAcademicYear().getId())
                .category(application.getCategory())
                .qualifyingExamScore(application.getQualifyingExamScore())
                .qualifyingExamDetails(application.getQualifyingExamDetails())
                .status(application.getStatus())
                .submittedAt(application.getSubmittedAt())
                .build();
    }
}
