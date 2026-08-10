package com.college.erp.module.transport.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.transport.domain.Route;
import com.college.erp.module.transport.domain.RouteRepository;
import com.college.erp.module.transport.dto.CreateRouteRequest;
import com.college.erp.module.transport.dto.RouteDto;

import lombok.RequiredArgsConstructor;

/** 04-database-schema.md §14.2 — see the module's CLAUDE.md deviations note (no SRS/API basis). */
@Service
@RequiredArgsConstructor
public class RouteService {

    private static final String MODULE = "TRANSPORT";

    private final RouteRepository routeRepository;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public RouteDto create(CreateRouteRequest request) {
        Route route = Route.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        Route saved = routeRepository.save(route);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "Route", saved.getId().toString(),
                "Created: " + saved.getName());

        return RouteDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RouteDto> listActive() {
        return routeRepository.findByActiveTrueAndDeletedFalse().stream()
                .map(RouteDto::from)
                .toList();
    }

    /** For StudentRouteMappingService — see BatchService.getBatchEntity javadoc. */
    @Transactional(readOnly = true)
    public Route getRouteEntity(UUID id) {
        return routeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route", id));
    }
}
