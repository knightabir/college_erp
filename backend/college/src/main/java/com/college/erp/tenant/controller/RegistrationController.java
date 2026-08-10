package com.college.erp.tenant.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.tenant.dto.RegisterCollegeRequest;
import com.college.erp.tenant.dto.RegisterCollegeResponse;
import com.college.erp.tenant.service.TenantRegistrationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The one public, unauthenticated endpoint in this entire system — see SecurityConfig's permitAll
 * list and TenantRegistrationService's javadoc. Deliberately not under {@code /api/v1/auth} —
 * {@code AuthController} covers an existing account logging in/out; this is a wholly different
 * concern (a brand-new tenant coming into existence).
 */
@RestController
@RequestMapping("/api/v1/register")
@RequiredArgsConstructor
public class RegistrationController {

    private final TenantRegistrationService tenantRegistrationService;

    @PostMapping
    public ResponseEntity<ApiResponse<RegisterCollegeResponse>> register(
            @Valid @RequestBody RegisterCollegeRequest request, HttpServletRequest httpRequest) {
        RegisterCollegeResponse response = tenantRegistrationService.register(
                request, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
