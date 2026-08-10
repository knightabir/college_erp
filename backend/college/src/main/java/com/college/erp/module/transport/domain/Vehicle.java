package com.college.erp.module.transport.domain;

import java.time.LocalDate;

import com.college.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 04-database-schema.md §14.2. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vehicles")
public class Vehicle extends BaseEntity {

    @Column(name = "registration_no", nullable = false, unique = true, length = 20)
    private String registrationNo;

    @Column(name = "vehicle_type", length = 30)
    private String vehicleType;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "driver_name", length = 100)
    private String driverName;

    @Column(name = "driver_phone", length = 20)
    private String driverPhone;

    @Column(name = "driver_license", length = 30)
    private String driverLicense;

    @Column(name = "license_expiry")
    private LocalDate licenseExpiry;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
