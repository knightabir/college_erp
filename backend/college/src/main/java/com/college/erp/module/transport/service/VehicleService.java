package com.college.erp.module.transport.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.transport.domain.Vehicle;
import com.college.erp.module.transport.domain.VehicleRepository;
import com.college.erp.module.transport.dto.CreateVehicleRequest;
import com.college.erp.module.transport.dto.VehicleDto;

import lombok.RequiredArgsConstructor;

/** 04-database-schema.md §14.2 — see the module's CLAUDE.md deviations note (no SRS/API basis). */
@Service
@RequiredArgsConstructor
public class VehicleService {

    private static final String MODULE = "TRANSPORT";

    private final VehicleRepository vehicleRepository;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public VehicleDto create(CreateVehicleRequest request) {
        if (vehicleRepository.existsByRegistrationNo(request.getRegistrationNo())) {
            throw new DuplicateResourceException(
                    "Vehicle with registration '" + request.getRegistrationNo() + "' already exists");
        }

        Vehicle vehicle = Vehicle.builder()
                .registrationNo(request.getRegistrationNo())
                .vehicleType(request.getVehicleType())
                .capacity(request.getCapacity())
                .driverName(request.getDriverName())
                .driverPhone(request.getDriverPhone())
                .driverLicense(request.getDriverLicense())
                .licenseExpiry(request.getLicenseExpiry())
                .build();
        Vehicle saved = vehicleRepository.save(vehicle);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "Vehicle", saved.getId().toString(),
                "Registered vehicle " + saved.getRegistrationNo());

        return VehicleDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<VehicleDto> listActive() {
        return vehicleRepository.findByActiveTrueAndDeletedFalse().stream()
                .map(VehicleDto::from)
                .toList();
    }

    /** For StudentRouteMappingService — see BatchService.getBatchEntity javadoc. */
    @Transactional(readOnly = true)
    public Vehicle getVehicleEntity(UUID id) {
        return vehicleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
    }
}
