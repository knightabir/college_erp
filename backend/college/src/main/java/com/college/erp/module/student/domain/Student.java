package com.college.erp.module.student.domain;

import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.common.domain.TenantScopedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 04-database-schema.md §7.1. aadhar_number is stored but must never appear in logs or be
 * returned unmasked (07-coding-standards.md §5.2) — masking happens in StudentDto.from(), never
 * strip/mask it here so the raw value stays available to services that legitimately need it
 * (document verification).
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "students",
    indexes = {
        @Index(name = "idx_student_enrollment", columnList = "enrollment_number"),
        @Index(name = "idx_student_email", columnList = "personal_email")
    }
)
public class Student extends TenantScopedEntity {

    @Column(name = "user_id", unique = true)
    private UUID userId;

    @Column(name = "enrollment_number", nullable = false, unique = true, length = 30)
    private String enrollmentNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "personal_email", nullable = false, length = 150)
    private String personalEmail;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    @Column(name = "blood_group", length = 5)
    private String bloodGroup;

    @Column(name = "aadhar_number", length = 12)
    private String aadharNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30)
    private StudentCategory category;

    @Builder.Default
    @Column(name = "nationality", nullable = false, length = 50)
    private String nationality = "Indian";

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "photo_file_id", length = 36)
    private String photoFileId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StudentStatus status = StudentStatus.ACTIVE;

    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;

    @Column(name = "graduation_date")
    private LocalDate graduationDate;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
