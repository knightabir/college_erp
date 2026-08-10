package com.college.erp.module.finance.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.module.finance.domain.FinancialYear;
import com.college.erp.module.finance.domain.FinancialYearRepository;
import com.college.erp.module.finance.domain.FinancialYearStatus;
import com.college.erp.module.finance.dto.FinancialYearDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 03-srs.md [FIN-009]. Named to match 08-system-architecture.md's test tree. */
class FinancialYearLockTest {

    private final FinancialYearLockGuard guard = new FinancialYearLockGuard();

    @Test
    @DisplayName("assertOpen() — OPEN year does not throw")
    void assertOpen_openYear_doesNotThrow() {
        FinancialYear year = FinancialYear.builder().label("2026-2027").status(FinancialYearStatus.OPEN).build();

        assertThatCode(() -> guard.assertOpen(year)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertOpen() — CLOSED year blocks new ledger entries")
    void assertOpen_closedYear_throws() {
        FinancialYear year = FinancialYear.builder().label("2025-2026").status(FinancialYearStatus.CLOSED).build();

        assertThatThrownBy(() -> guard.assertOpen(year)).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("assertOpen() — CLOSING_PENDING year also blocks new ledger entries")
    void assertOpen_closingPendingYear_throws() {
        FinancialYear year = FinancialYear.builder().label("2025-2026")
                .status(FinancialYearStatus.CLOSING_PENDING).build();

        assertThatThrownBy(() -> guard.assertOpen(year)).isInstanceOf(BusinessRuleViolationException.class);
    }

    @ExtendWith(MockitoExtension.class)
    static class CloseWorkflowTest {

        @Mock
        private FinancialYearRepository financialYearRepository;
        @Mock
        private AuditLogWriter auditLogWriter;

        private FinancialYearService financialYearService;

        private FinancialYear buildOpenYear(UUID id) {
            FinancialYear year = FinancialYear.builder().label("2026-2027")
                    .startDate(LocalDate.of(2026, 4, 1)).endDate(LocalDate.of(2027, 3, 31))
                    .status(FinancialYearStatus.OPEN).build();
            year.setId(id);
            return year;
        }

        @Test
        @DisplayName("initiateOrCompleteClose() — first call moves OPEN to CLOSING_PENDING")
        void firstCall_movesToClosingPending() {
            // ARRANGE
            financialYearService = new FinancialYearService(financialYearRepository, auditLogWriter);
            UUID yearId = UUID.randomUUID();
            UUID headAccountantId = UUID.randomUUID();
            FinancialYear year = buildOpenYear(yearId);

            when(financialYearRepository.findByIdAndDeletedFalse(yearId)).thenReturn(Optional.of(year));
            when(financialYearRepository.save(any(FinancialYear.class))).thenAnswer(inv -> inv.getArgument(0));

            // ACT
            FinancialYearDto result = financialYearService.initiateOrCompleteClose(yearId, headAccountantId);

            // ASSERT
            assertThat(result.getStatus()).isEqualTo(FinancialYearStatus.CLOSING_PENDING);
            assertThat(year.getClosedByUserId()).isEqualTo(headAccountantId);
        }

        @Test
        @DisplayName("initiateOrCompleteClose() — second call by a different user completes the close")
        void secondCallByDifferentUser_completesClose() {
            // ARRANGE
            financialYearService = new FinancialYearService(financialYearRepository, auditLogWriter);
            UUID yearId = UUID.randomUUID();
            UUID headAccountantId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();
            FinancialYear year = buildOpenYear(yearId);
            year.setStatus(FinancialYearStatus.CLOSING_PENDING);
            year.setClosedByUserId(headAccountantId);

            when(financialYearRepository.findByIdAndDeletedFalse(yearId)).thenReturn(Optional.of(year));
            when(financialYearRepository.save(any(FinancialYear.class))).thenAnswer(inv -> inv.getArgument(0));

            // ACT
            FinancialYearDto result = financialYearService.initiateOrCompleteClose(yearId, adminId);

            // ASSERT
            assertThat(result.getStatus()).isEqualTo(FinancialYearStatus.CLOSED);
            assertThat(result.getApprovedByUserId()).isEqualTo(adminId);
        }

        @Test
        @DisplayName("initiateOrCompleteClose() — the same user cannot provide both approvals")
        void secondCallBySameUser_throws() {
            // ARRANGE
            financialYearService = new FinancialYearService(financialYearRepository, auditLogWriter);
            UUID yearId = UUID.randomUUID();
            UUID headAccountantId = UUID.randomUUID();
            FinancialYear year = buildOpenYear(yearId);
            year.setStatus(FinancialYearStatus.CLOSING_PENDING);
            year.setClosedByUserId(headAccountantId);

            when(financialYearRepository.findByIdAndDeletedFalse(yearId)).thenReturn(Optional.of(year));

            // ACT / ASSERT
            assertThatThrownBy(() -> financialYearService.initiateOrCompleteClose(yearId, headAccountantId))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("initiateOrCompleteClose() — an already-CLOSED year cannot be closed again")
        void alreadyClosed_throws() {
            // ARRANGE
            financialYearService = new FinancialYearService(financialYearRepository, auditLogWriter);
            UUID yearId = UUID.randomUUID();
            FinancialYear year = buildOpenYear(yearId);
            year.setStatus(FinancialYearStatus.CLOSED);

            when(financialYearRepository.findByIdAndDeletedFalse(yearId)).thenReturn(Optional.of(year));

            // ACT / ASSERT
            assertThatThrownBy(() -> financialYearService.initiateOrCompleteClose(yearId, UUID.randomUUID()))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }
}
