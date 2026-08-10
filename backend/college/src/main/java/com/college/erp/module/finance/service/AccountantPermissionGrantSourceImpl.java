package com.college.erp.module.finance.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.college.erp.module.finance.domain.AccountantPermissionGrant;
import com.college.erp.module.finance.domain.AccountantPermissionGrantRepository;
import com.college.erp.security.rbac.ActiveGrant;
import com.college.erp.security.rbac.PermissionGrantSource;

import lombok.RequiredArgsConstructor;

/**
 * Finance-specific implementation of the security kernel's dependency-inversion seam — see
 * GrantResolver javadoc for why this is aggregated alongside module.auth's PermissionGrantSourceImpl
 * rather than replacing it. scope_fee_head_code maps onto ActiveGrant's generic "feeHead" slot.
 */
@Component
@RequiredArgsConstructor
public class AccountantPermissionGrantSourceImpl implements PermissionGrantSource {

    private final AccountantPermissionGrantRepository accountantPermissionGrantRepository;

    @Override
    public List<ActiveGrant> findActiveGrants(UUID userId) {
        return accountantPermissionGrantRepository.findActiveByGranteeUserId(userId).stream()
                .map(this::toActiveGrant)
                .toList();
    }

    private ActiveGrant toActiveGrant(AccountantPermissionGrant grant) {
        return new ActiveGrant(
                grant.getPermissionKey(),
                grant.getScopeDepartmentId(),
                grant.getScopeBatchId(),
                grant.getScopeFeeHeadCode(),
                grant.getAmountThreshold());
    }
}
