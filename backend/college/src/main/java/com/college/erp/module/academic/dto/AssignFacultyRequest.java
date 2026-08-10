package com.college.erp.module.academic.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignFacultyRequest {

    @NotNull(message = "Subject offering is required")
    private UUID offeringId;

    @NotNull(message = "Section is required")
    private UUID sectionId;

    @NotNull(message = "Faculty user is required")
    private UUID facultyUserId;

    private boolean primary = true;
}
