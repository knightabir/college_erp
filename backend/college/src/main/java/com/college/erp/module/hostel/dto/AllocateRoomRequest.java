package com.college.erp.module.hostel.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AllocateRoomRequest {

    @NotNull
    private UUID roomId;

    @NotNull
    private UUID studentId;

    @NotNull
    private LocalDate fromDate;
}
