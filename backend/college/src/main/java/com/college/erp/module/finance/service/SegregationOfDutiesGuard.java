package com.college.erp.module.finance.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.config.ErpProperties;

import lombok.RequiredArgsConstructor;

/**
 * FR-RBAC-5 (02-prd.md §4.2): "A single user creating AND approving the same financial
 * transaction above the configured threshold SHALL be prevented." Used wherever one user's role
 * lets them both create and approve something with money attached — currently purchase order
 * approval (ProcurementService).
 */
@Component
@RequiredArgsConstructor
public class SegregationOfDutiesGuard {

    private final ErpProperties erpProperties;

    public void assertNotSelfApproval(UUID creatorUserId, UUID approverUserId, BigDecimal amount) {
        if (amount == null || creatorUserId == null || approverUserId == null) {
            return;
        }
        boolean sameUser = creatorUserId.equals(approverUserId);
        boolean aboveThreshold = amount.compareTo(erpProperties.getFinance().getSegregationOfDutiesThreshold()) > 0;

        if (sameUser && aboveThreshold) {
            throw new BusinessRuleViolationException(
                    "The creator of a transaction above the segregation-of-duties threshold "
                            + "cannot also approve it");
        }
    }
}
