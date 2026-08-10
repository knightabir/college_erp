package com.college.erp.module.student.dto;

import java.util.UUID;

import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.domain.StudentStatus;

import lombok.Builder;
import lombok.Getter;

/** Lean projection for list views (by-section, by-batch) — full detail is StudentDto. */
@Getter
@Builder
public class StudentSummaryDto {

    private UUID id;
    private String enrollmentNumber;
    private String fullName;
    private String personalEmail;
    private String phone;
    private StudentStatus status;

    public static StudentSummaryDto from(Student student) {
        return StudentSummaryDto.builder()
                .id(student.getId())
                .enrollmentNumber(student.getEnrollmentNumber())
                .fullName(student.getFullName())
                .personalEmail(student.getPersonalEmail())
                .phone(student.getPhone())
                .status(student.getStatus())
                .build();
    }
}
