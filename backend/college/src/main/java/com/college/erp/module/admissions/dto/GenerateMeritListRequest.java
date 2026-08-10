package com.college.erp.module.admissions.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** INVENTED — see V015's header comment. Ranks every SHORTLISTED application for the given
 *  course/year by qualifying_exam_score, descending. */
@Getter
@Setter
public class GenerateMeritListRequest {

    @NotNull
    private UUID courseId;

    @NotNull
    private UUID academicYearId;

    @NotBlank
    private String name;
}
