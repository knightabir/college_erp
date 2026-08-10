package com.college.erp.module.finance.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.module.finance.domain.AccountHead;
import com.college.erp.module.finance.domain.AccountType;
import com.college.erp.module.finance.domain.EntryType;
import com.college.erp.module.finance.domain.FeeInvoice;
import com.college.erp.module.finance.domain.FeePayment;
import com.college.erp.module.finance.domain.FinancialYear;
import com.college.erp.module.finance.domain.FinancialYearStatus;
import com.college.erp.module.finance.domain.LedgerEntry;
import com.college.erp.module.finance.domain.LedgerEntryRepository;
import com.college.erp.module.finance.domain.PaymentMode;
import com.college.erp.module.student.domain.Student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 03-srs.md [FIN-004]. Named to match 08-system-architecture.md's test tree. */
@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;
    @Mock
    private AccountHeadService accountHeadService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private final FinancialYearLockGuard financialYearLockGuard = new FinancialYearLockGuard();
    private LedgerService ledgerService;

    private FinancialYear buildYear(FinancialYearStatus status) {
        FinancialYear year = FinancialYear.builder().label("2026-2027").status(status).build();
        year.setId(UUID.randomUUID());
        return year;
    }

    private FeePayment buildPayment() {
        Student student = Student.builder().enrollmentNumber("2024CSE001").firstName("Arjun").lastName("Kumar").build();
        student.setId(UUID.randomUUID());
        FeeInvoice invoice = FeeInvoice.builder().invoiceNumber("INV-2026-00000001").student(student)
                .totalAmount(BigDecimal.valueOf(45000)).netAmount(BigDecimal.valueOf(45000)).build();
        invoice.setId(UUID.randomUUID());
        FeePayment payment = FeePayment.builder().invoice(invoice).receiptNumber("RCP-2026-00000001")
                .amount(BigDecimal.valueOf(25000)).paymentMode(PaymentMode.ONLINE_UPI)
                .collectedByUserId(UUID.randomUUID()).paymentTimestamp(Instant.now()).build();
        payment.setId(UUID.randomUUID());
        return payment;
    }

    @Test
    @DisplayName("postFeeCollection() — posts a balanced DEBIT/CREDIT pair for the same amount")
    void postFeeCollection_postsBalancedPair() {
        // ARRANGE
        ledgerService = new LedgerService(ledgerEntryRepository, accountHeadService, financialYearLockGuard,
                auditLogWriter);
        FinancialYear year = buildYear(FinancialYearStatus.OPEN);
        FeePayment payment = buildPayment();
        AccountHead receivable = AccountHead.builder().code("ACCOUNTS_RECEIVABLE").name("Accounts Receivable")
                .type(AccountType.ASSET).build();
        AccountHead feeIncome = AccountHead.builder().code("FEE_INCOME").name("Fee Income")
                .type(AccountType.INCOME).build();

        when(accountHeadService.getOrCreateSystemHead(eq("ACCOUNTS_RECEIVABLE"), any(), any())).thenReturn(receivable);
        when(accountHeadService.getOrCreateSystemHead(eq("FEE_INCOME"), any(), any())).thenReturn(feeIncome);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(inv -> {
            LedgerEntry entry = inv.getArgument(0);
            return entry.toBuilder().id(UUID.randomUUID()).build();
        });

        // ACT
        ledgerService.postFeeCollection(payment, payment.getInvoice(), year);

        // ASSERT
        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(2)).save(captor.capture());

        LedgerEntry debit = captor.getAllValues().get(0);
        LedgerEntry credit = captor.getAllValues().get(1);
        assertThat(debit.getEntryType()).isEqualTo(EntryType.DEBIT);
        assertThat(debit.getAccountHead()).isEqualTo(receivable);
        assertThat(credit.getEntryType()).isEqualTo(EntryType.CREDIT);
        assertThat(credit.getAccountHead()).isEqualTo(feeIncome);
        assertThat(debit.getAmount()).isEqualByComparingTo(credit.getAmount());
        assertThat(debit.getAmount()).isEqualByComparingTo(payment.getAmount());
    }

    @Test
    @DisplayName("postFeeCollection() — a CLOSED financial year rejects the post entirely")
    void postFeeCollection_closedYear_throws() {
        // ARRANGE
        ledgerService = new LedgerService(ledgerEntryRepository, accountHeadService, financialYearLockGuard,
                auditLogWriter);
        FinancialYear closedYear = buildYear(FinancialYearStatus.CLOSED);
        FeePayment payment = buildPayment();

        // ACT / ASSERT
        assertThatThrownBy(() -> ledgerService.postFeeCollection(payment, payment.getInvoice(), closedYear))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
