package com.college.erp.module.transport.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.transport.domain.Vehicle;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VehicleDto {

    private UUID id;
    private String registrationNo;
    private String vehicleType;
    private Integer capacity;
    private String driverName;
    private String driverPhone;
    private String driverLicense;
    private LocalDate licenseExpiry;
    private boolean active;

    public static VehicleDto from(Vehicle vehicle) {
        return VehicleDto.builder()
                .id(vehicle.getId())
                .registrationNo(vehicle.getRegistrationNo())
                .vehicleType(vehicle.getVehicleType())
                .capacity(vehicle.getCapacity())
                .driverName(vehicle.getDriverName())
                .driverPhone(vehicle.getDriverPhone())
                .driverLicense(vehicle.getDriverLicense())
                .licenseExpiry(vehicle.getLicenseExpiry())
                .active(vehicle.isActive())
                .build();
    }
}
