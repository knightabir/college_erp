package com.college.erp.module.auth.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.module.auth.domain.PermissionGrant;
import com.college.erp.module.auth.domain.PermissionGrantRepository;
import com.college.erp.security.rbac.ActiveGrant;
import com.college.erp.security.rbac.PermissionGrantSource;

import lombok.RequiredArgsConstructor;

/**
 * The general-purpose implementation of the security kernel's dependency-inversion seam (see
 * PermissionGrantSource javadoc). GrantResolver aggregates this alongside every other registered
 * PermissionGrantSource (e.g. module.finance's accountant grants) — no @Primary needed, they're
 * all consulted together, not competing for one slot.
 */
@Component
@RequiredArgsConstructor
public class PermissionGrantSourceImpl implements PermissionGrantSource {

    private final PermissionGrantRepository permissionGrantRepository;

    @Override
    public List<ActiveGrant> findActiveGrants(UUID userId) {
        return permissionGrantRepository.findActiveByGranteeUserId(userId).stream()
                .map(this::toActiveGrant)
                .toList();
    }

    /** For PermissionGrantExpiryJob — kept here rather than the job injecting
     *  PermissionGrantRepository directly (07-coding-standards.md §4.4: no cross-module
     *  repository access). Not part of the PermissionGrantSource interface — this bean already
     *  owns the repository, this is just a second, unrelated use of it. */
    @Transactional(readOnly = true)
    public List<PermissionGrant> findExpiringBetween(Instant from, Instant to) {
        return permissionGrantRepository.findExpiringBetween(from, to);
    }

    private ActiveGrant toActiveGrant(PermissionGrant grant) {
        return new ActiveGrant(
                grant.getPermissionKey(),
                grant.getScopeDepartmentId(),
                grant.getScopeBatchId(),
                grant.getScopeFeeHead(),
                grant.getAmountThreshold());
    }
}
