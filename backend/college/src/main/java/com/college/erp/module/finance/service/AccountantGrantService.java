package com.college.erp.module.finance.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.AccessDeniedException;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.finance.domain.AccountantPermissionGrant;
import com.college.erp.module.finance.domain.AccountantPermissionGrantRepository;
import com.college.erp.module.finance.dto.AccountantGrantDto;
import com.college.erp.module.finance.dto.CreateAccountantGrantRequest;

import lombok.RequiredArgsConstructor;

/** 03-srs.md [FIN-008], 05-api-documentation.md §7 "Accountant Permission Grants". */
@Service
@RequiredArgsConstructor
public class AccountantGrantService {

    private static final String MODULE = "FINANCE";

    private final AccountantPermissionGrantRepository accountantPermissionGrantRepository;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public AccountantGrantDto grant(CreateAccountantGrantRequest request, UUID headAccountantUserId) {
        // FR-RBAC-3 (02-prd.md §4.2): a user cannot grant themselves a permission.
        if (request.getGranteeUserId().equals(headAccountantUserId)) {
            throw new BusinessRuleViolationException("You cannot grant a permission to yourself");
        }

        AccountantPermissionGrant grant = AccountantPermissionGrant.builder()
                .granteeUserId(request.getGranteeUserId())
                .grantedByUserId(headAccountantUserId)
                .permissionKey(request.getPermissionKey())
                .scopeDepartmentId(request.getScopeDepartmentId())
                .scopeBatchId(request.getScopeBatchId())
                .scopeFeeHeadCode(request.getScopeFeeHeadCode())
                .amountThreshold(request.getAmountThreshold())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveUntil(request.getEffectiveUntil())
                .build();

        AccountantPermissionGrant saved = accountantPermissionGrantRepository.save(grant);
        auditLogWriter.write(AuditAction.PERMISSION_GRANT_CREATED, MODULE, "AccountantPermissionGrant",
                saved.getId().toString(), "Granted " + saved.getPermissionKey() + " to " + saved.getGranteeUserId());

        return AccountantGrantDto.from(saved);
    }

    @Transactional
    public void revoke(UUID id, UUID revokedByUserId) {
        AccountantPermissionGrant grant = accountantPermissionGrantRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("AccountantPermissionGrant", id));

        if (!grant.getGrantedByUserId().equals(revokedByUserId)) {
            throw new AccessDeniedException("Only the Head Accountant who issued this grant may revoke it");
        }

        grant.setRevokedAt(Instant.now());
        grant.setRevokedByUserId(revokedByUserId);
        accountantPermissionGrantRepository.save(grant);

        auditLogWriter.write(AuditAction.PERMISSION_GRANT_REVOKED, MODULE, "AccountantPermissionGrant",
                id.toString(), "Revoked");
    }

    @Transactional(readOnly = true)
    public List<AccountantGrantDto> listGrantedBy(UUID headAccountantUserId) {
        return accountantPermissionGrantRepository.findByGrantedByUserIdAndDeletedFalse(headAccountantUserId)
                .stream()
                .map(AccountantGrantDto::from)
                .toList();
    }
}
