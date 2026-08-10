package com.college.erp.module.hostel.dto;

import java.util.UUID;

import com.college.erp.module.hostel.domain.HostelGender;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateHostelBlockRequest {

    @NotBlank
    private String name;

    @NotNull
    private HostelGender gender;

    private UUID wardenId;
    private Integer capacity;
}
