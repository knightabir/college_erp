package com.college.erp.config;

import java.time.Instant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.college.erp.common.dto.ErrorResponse;
import com.college.erp.security.jwt.JwtAuthenticationFilter;
import com.college.erp.security.jwt.JwtTokenProvider;
import com.college.erp.security.rbac.PermissionEvaluator;
import com.college.erp.tenant.TenantFilter;
import com.college.erp.web.RequestIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Filter chain, CORS, auth provider (06-design-document.md §2.1). Endpoints default to
 * authenticated-only; @PreAuthorize on each controller method narrows by role/permission
 * (07-coding-standards.md §5.1 — every non-public endpoint must carry an explicit @PreAuthorize).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private static final int BCRYPT_STRENGTH = 12; // FR-AUTH-1: cost factor >= 12

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    // No AuthenticationManager/AuthenticationProvider bean: login is handled explicitly in
    // AuthService (load user, BCrypt-verify, issue JWT — 08-system-architecture.md §4.1), not via
    // Spring Security's authenticate() flow. JwtAuthenticationFilter is the only thing that reads
    // UserDetailsService, for subsequent requests once a token exists.

    @Bean
    public DefaultMethodSecurityExpressionHandler methodSecurityExpressionHandler(
            PermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
        TenantFilter tenantFilter = new TenantFilter();

        http
            .csrf(csrf -> csrf.disable()) // stateless JSON API, no cookie-based session
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/api/v1/auth/login",
                            "/api/v1/auth/refresh",
                            "/api/v1/register",
                            "/actuator/health")
                    .permitAll()
                    .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(authenticationEntryPoint())
                    .accessDeniedHandler(accessDeniedHandler()))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            // Multi-tenancy (08-system-architecture.md §5.2): must run AFTER JwtAuthenticationFilter
            // — it reads the tenant id off the UserPrincipal that filter just resolved.
            .addFilterAfter(tenantFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> writeError(response, 401, "unauthorized",
                "Missing or invalid authentication token", request.getRequestURI());
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> writeError(response, 403, "forbidden",
                "You do not have permission to perform this action", request.getRequestURI());
    }

    private void writeError(jakarta.servlet.http.HttpServletResponse response, int status, String type,
            String detail, String instance) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String requestId = org.slf4j.MDC.get(RequestIdFilter.MDC_REQUEST_ID);
        ErrorResponse body = ErrorResponse.builder()
                .type("https://erp.college.edu/errors/" + type)
                .title(type)
                .status(status)
                .detail(detail)
                .instance(instance)
                .requestId(requestId)
                .timestamp(Instant.now())
                .build();
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
