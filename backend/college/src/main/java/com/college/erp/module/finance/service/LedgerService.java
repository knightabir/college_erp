package com.college.erp.module.finance.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.module.finance.domain.AccountHead;
import com.college.erp.module.finance.domain.AccountType;
import com.college.erp.module.finance.domain.EntryType;
import com.college.erp.module.finance.domain.FeeInvoice;
import com.college.erp.module.finance.domain.FeePayment;
import com.college.erp.module.finance.domain.FinancialYear;
import com.college.erp.module.finance.domain.LedgerEntry;
import com.college.erp.module.finance.domain.LedgerEntryRepository;
import com.college.erp.module.finance.dto.LedgerEntryDto;
import com.college.erp.module.finance.dto.ManualLedgerEntryRequest;

import lombok.RequiredArgsConstructor;

/**
 * 03-srs.md [FIN-004]: double-entry, append-only (V007's trg_ledger_no_update/no_delete, reusing
 * V002's shared trigger function).
 *
 * Fee collection is auto-paired here (DEBIT receivable / CREDIT fee income, per FIN-004's exact
 * wording) using two well-known lazily-created "system" account heads — see
 * AccountHeadService.getOrCreateSystemHead. Manual journal entries are NOT auto-paired: the
 * documented request shape (05-api-documentation.md §7) is single-sided (one account head, one
 * entry type per call), so a balanced correction is the Head Accountant's responsibility to
 * enter as two calls with a shared reference_id — this method posts exactly what's asked, once.
 */
@Service
@RequiredArgsConstructor
public class LedgerService {

    private static final String MODULE = "FINANCE";
    private static final String RECEIVABLE_CODE = "ACCOUNTS_RECEIVABLE";
    private static final String FEE_INCOME_CODE = "FEE_INCOME";

    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountHeadService accountHeadService;
    private final FinancialYearLockGuard financialYearLockGuard;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public void postFeeCollection(FeePayment payment, FeeInvoice invoice, FinancialYear financialYear) {
        financialYearLockGuard.assertOpen(financialYear);

        AccountHead receivable = accountHeadService.getOrCreateSystemHead(
                RECEIVABLE_CODE, "Accounts Receivable", AccountType.ASSET);
        AccountHead feeIncome = accountHeadService.getOrCreateSystemHead(
                FEE_INCOME_CODE, "Fee Income", AccountType.INCOME);

        String description = "Fee payment " + payment.getReceiptNumber() + " for invoice " + invoice.getInvoiceNumber();
        LocalDate today = LocalDate.now();

        LedgerEntry debit = LedgerEntry.builder()
                .accountHead(receivable)
                .description(description)
                .entryType(EntryType.DEBIT)
                .amount(payment.getAmount())
                .entryDate(today)
                .financialYear(financialYear)
                .referenceType("FEE_PAYMENT")
                .referenceId(payment.getId().toString())
                .createdBy(payment.getCollectedByUserId())
                .build();

        LedgerEntry credit = LedgerEntry.builder()
                .accountHead(feeIncome)
                .description(description)
                .entryType(EntryType.CREDIT)
                .amount(payment.getAmount())
                .entryDate(today)
                .financialYear(financialYear)
                .referenceType("FEE_PAYMENT")
                .referenceId(payment.getId().toString())
                .createdBy(payment.getCollectedByUserId())
                .build();

        ledgerEntryRepository.save(debit);
        LedgerEntry saved = ledgerEntryRepository.save(credit);

        auditLogWriter.write(AuditAction.LEDGER_ENTRY_POSTED, MODULE, "LedgerEntry", saved.getId().toString(),
                description);
    }

    @Transactional
    public LedgerEntryDto postManualEntry(ManualLedgerEntryRequest request, FinancialYear financialYear,
            UUID postedByUserId) {
        financialYearLockGuard.assertOpen(financialYear);
        AccountHead accountHead = accountHeadService.getAccountHeadEntity(request.getAccountHeadId());

        LedgerEntry entry = LedgerEntry.builder()
                .accountHead(accountHead)
                .description(request.getDescription())
                .entryType(request.getEntryType())
                .amount(request.getAmount())
                .entryDate(LocalDate.now())
                .financialYear(financialYear)
                .referenceType(request.getReferenceType())
                .referenceId(request.getReferenceId())
                .createdBy(postedByUserId)
                .build();

        LedgerEntry saved = ledgerEntryRepository.save(entry);
        auditLogWriter.write(AuditAction.LEDGER_ENTRY_POSTED, MODULE, "LedgerEntry", saved.getId().toString(),
                "Manual entry: " + request.getDescription());

        return LedgerEntryDto.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryDto> browse(LocalDate from, LocalDate to, Pageable pageable) {
        return ledgerEntryRepository.browse(from, to, pageable).map(LedgerEntryDto::from);
    }
}
