package com.college.erp.module.transport.dto;

import java.util.UUID;

import com.college.erp.module.transport.domain.StudentRouteMapping;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentRouteMappingDto {

    private UUID id;
    private UUID studentId;
    private String studentName;
    private UUID routeId;
    private String routeName;
    private UUID vehicleId;
    private String stopName;
    private boolean active;

    public static StudentRouteMappingDto from(StudentRouteMapping mapping) {
        return StudentRouteMappingDto.builder()
                .id(mapping.getId())
                .studentId(mapping.getStudent().getId())
                .studentName(mapping.getStudent().getFullName())
                .routeId(mapping.getRoute().getId())
                .routeName(mapping.getRoute().getName())
                .vehicleId(mapping.getVehicle() != null ? mapping.getVehicle().getId() : null)
                .stopName(mapping.getStopName())
                .active(mapping.isActive())
                .build();
    }
}
