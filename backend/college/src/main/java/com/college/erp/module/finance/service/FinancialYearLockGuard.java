package com.college.erp.module.finance.service;

import org.springframework.stereotype.Component;

import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.module.finance.domain.FinancialYear;
import com.college.erp.module.finance.domain.FinancialYearStatus;

/**
 * 03-srs.md [FIN-009]: "On close, no new ledger entries may be posted for the closed year."
 * Called before every ledger post — fee collection, manual journal entries, procurement payment.
 */
@Component
public class FinancialYearLockGuard {

    public void assertOpen(FinancialYear financialYear) {
        if (financialYear.getStatus() != FinancialYearStatus.OPEN) {
            throw new BusinessRuleViolationException(
                    "Financial year '" + financialYear.getLabel() + "' is " + financialYear.getStatus()
                            + " — no new ledger entries may be posted");
        }
    }
}
