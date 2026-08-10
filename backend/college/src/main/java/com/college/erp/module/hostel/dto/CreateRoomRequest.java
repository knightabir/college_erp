package com.college.erp.module.hostel.dto;

import com.college.erp.module.hostel.domain.RoomType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoomRequest {

    @NotBlank
    private String roomNumber;

    private Integer floor;

    @Min(1)
    private int capacity = 2;

    private RoomType roomType;
}
