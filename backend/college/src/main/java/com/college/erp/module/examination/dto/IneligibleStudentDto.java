package com.college.erp.module.examination.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IneligibleStudentDto {

    private UUID studentId;
    private String enrollmentNumber;
    private String reason;
}
