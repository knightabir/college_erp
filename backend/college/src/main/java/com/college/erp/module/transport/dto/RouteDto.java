package com.college.erp.module.transport.dto;

import java.util.UUID;

import com.college.erp.module.transport.domain.Route;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RouteDto {

    private UUID id;
    private String name;
    private String description;
    private boolean active;

    public static RouteDto from(Route route) {
        return RouteDto.builder()
                .id(route.getId())
                .name(route.getName())
                .description(route.getDescription())
                .active(route.isActive())
                .build();
    }
}
