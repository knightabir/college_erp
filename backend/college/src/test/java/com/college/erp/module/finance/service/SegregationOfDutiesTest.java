package com.college.erp.module.finance.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.config.ErpProperties;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** FR-RBAC-5 (02-prd.md §4.2). Named to match 08-system-architecture.md's test tree. */
class SegregationOfDutiesTest {

    private final ErpProperties erpProperties = new ErpProperties();
    private final SegregationOfDutiesGuard guard = new SegregationOfDutiesGuard(erpProperties);

    @Test
    @DisplayName("assertNotSelfApproval() — same user, amount above threshold: rejected")
    void sameUserAboveThreshold_throws() {
        // ARRANGE
        UUID userId = UUID.randomUUID();
        BigDecimal aboveThreshold = erpProperties.getFinance().getSegregationOfDutiesThreshold()
                .add(BigDecimal.ONE);

        // ACT / ASSERT
        assertThatThrownBy(() -> guard.assertNotSelfApproval(userId, userId, aboveThreshold))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("assertNotSelfApproval() — same user, amount at/below threshold: allowed")
    void sameUserAtOrBelowThreshold_doesNotThrow() {
        // ARRANGE
        UUID userId = UUID.randomUUID();
        BigDecimal atThreshold = erpProperties.getFinance().getSegregationOfDutiesThreshold();

        // ACT / ASSERT — no exception
        assertThatCode(() -> guard.assertNotSelfApproval(userId, userId, atThreshold)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertNotSelfApproval() — different users, any amount: allowed")
    void differentUsers_doesNotThrow() {
        // ARRANGE
        BigDecimal largeAmount = erpProperties.getFinance().getSegregationOfDutiesThreshold()
                .multiply(BigDecimal.TEN);

        // ACT / ASSERT — no exception
        assertThatCode(() -> guard.assertNotSelfApproval(UUID.randomUUID(), UUID.randomUUID(), largeAmount))
                .doesNotThrowAnyException();
    }
}
