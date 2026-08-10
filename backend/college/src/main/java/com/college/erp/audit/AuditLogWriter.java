package com.college.erp.audit;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.college.erp.security.jwt.UserPrincipal;
import com.college.erp.web.RequestIdFilter;

import lombok.extern.slf4j.Slf4j;

/**
 * Used by every service that creates/updates/deletes data (06-design-document.md §2.5). Runs
 * async in a fresh REQUIRES_NEW transaction so a slow or failed audit write never blocks — and
 * never rolls back — the caller's business transaction.
 *
 * Context (current user, request id, remote IP) is read from SecurityContextHolder/MDC/
 * RequestContextHolder here in the synchronous entry method, before the @Async hop switches
 * threads — those ThreadLocals are not visible from the async executor's thread.
 */
@Slf4j
@Service
public class AuditLogWriter {

    private final AuditLogRepository auditLogRepository;

    public AuditLogWriter(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void write(AuditAction action, String module, String entityType, String entityId, String description) {
        write(action, module, entityType, entityId, description, null, null);
    }

    public void write(AuditAction action, String module, String entityType, String entityId, String description,
            String beforeValue, String afterValue) {
        UUID userId = null;
        String username = null;
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof UserPrincipal principal) {
            userId = principal.getId();
            username = principal.getUsername();
        }

        String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID);
        String ipAddress = resolveRemoteAddress();

        writeAsync(userId, username, requestId, ipAddress, action, module, entityType, entityId,
                description, beforeValue, afterValue);
    }

    // Public: Spring's CGLIB proxy for @Async/@Transactional must be able to override this
    // reliably regardless of proxy package placement — package-private is proxy-fragile.
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeAsync(UUID userId, String username, String requestId, String ipAddress, AuditAction action,
            String module, String entityType, String entityId, String description, String beforeValue,
            String afterValue) {
        try {
            AuditLog entry = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .module(module)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .beforeValue(beforeValue)
                    .afterValue(afterValue)
                    .ipAddress(ipAddress)
                    .requestId(requestId)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            // An audit write failure must never surface to the caller (it already ran async and
            // detached from the caller's transaction) — log loudly instead.
            log.error("Failed to write audit log entry: action={} entityType={} entityId={}",
                    action, entityType, entityId, ex);
        }
    }

    private String resolveRemoteAddress() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getRemoteAddr();
        }
        return null;
    }
}
