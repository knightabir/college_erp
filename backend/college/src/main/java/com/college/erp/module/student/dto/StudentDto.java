package com.college.erp.module.student.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.student.domain.Gender;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.domain.StudentCategory;
import com.college.erp.module.student.domain.StudentStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentDto {

    private UUID id;
    private UUID userId;
    private String enrollmentNumber;
    private String firstName;
    private String lastName;
    private String fullName;
    private String personalEmail;
    private String phone;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String bloodGroup;
    /** Masked — never the raw value (04-database-schema.md §7.1: "stored, masked on retrieval"). */
    private String aadharNumberMasked;
    private StudentCategory category;
    private String nationality;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String photoFileId;
    private StudentStatus status;
    private LocalDate admissionDate;
    private LocalDate graduationDate;

    public static StudentDto from(Student student) {
        return StudentDto.builder()
                .id(student.getId())
                .userId(student.getUserId())
                .enrollmentNumber(student.getEnrollmentNumber())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .fullName(student.getFullName())
                .personalEmail(student.getPersonalEmail())
                .phone(student.getPhone())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .bloodGroup(student.getBloodGroup())
                .aadharNumberMasked(maskAadhar(student.getAadharNumber()))
                .category(student.getCategory())
                .nationality(student.getNationality())
                .address(student.getAddress())
                .city(student.getCity())
                .state(student.getState())
                .pincode(student.getPincode())
                .photoFileId(student.getPhotoFileId())
                .status(student.getStatus())
                .admissionDate(student.getAdmissionDate())
                .graduationDate(student.getGraduationDate())
                .build();
    }

    private static String maskAadhar(String aadhar) {
        if (aadhar == null || aadhar.length() < 4) {
            return null;
        }
        return "X".repeat(aadhar.length() - 4) + aadhar.substring(aadhar.length() - 4);
    }
}
