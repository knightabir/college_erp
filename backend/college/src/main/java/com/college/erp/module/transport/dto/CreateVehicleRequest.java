package com.college.erp.module.transport.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateVehicleRequest {

    @NotBlank
    private String registrationNo;

    private String vehicleType;
    private Integer capacity;
    private String driverName;
    private String driverPhone;
    private String driverLicense;
    private LocalDate licenseExpiry;
}
