package com.college.erp.module.library.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.module.library.domain.BookCopy;
import com.college.erp.module.library.domain.BookCopyRepository;
import com.college.erp.module.library.domain.BookTitleRepository;
import com.college.erp.module.library.domain.BorrowerType;
import com.college.erp.module.library.domain.CopyCondition;
import com.college.erp.module.library.domain.LibraryCustodyLog;
import com.college.erp.module.library.domain.LibraryCustodyLogRepository;
import com.college.erp.module.library.domain.LibraryFineRepository;
import com.college.erp.module.library.domain.LoanPolicy;
import com.college.erp.module.library.domain.LoanPolicyRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** 03-srs.md [LIB-005]. Named to match 08-system-architecture.md's test tree. */
@ExtendWith(MockitoExtension.class)
class FineAccrualTest {

    @Mock
    private LibraryCustodyLogRepository libraryCustodyLogRepository;
    @Mock
    private LibraryFineRepository libraryFineRepository;
    @Mock
    private LoanPolicyRepository loanPolicyRepository;
    @Mock
    private BookCopyRepository bookCopyRepository;
    @Mock
    private BookTitleRepository bookTitleRepository;
    @Mock
    private CatalogService catalogService;
    @Mock
    private ReservationService reservationService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private CirculationService circulationService;

    private LibraryCustodyLog buildLog(Instant dueAt) {
        BookCopy copy = BookCopy.builder().barcode("LIB-2026-000001").accessionNumber("2026-000001").build();
        return LibraryCustodyLog.builder()
                .copy(copy).borrowerUserId(UUID.randomUUID()).borrowerName("Arjun Kumar")
                .borrowerType(BorrowerType.STUDENT).issuedByUserId(UUID.randomUUID())
                .issuedAt(dueAt.minus(14, ChronoUnit.DAYS)).dueAt(dueAt).conditionOnIssue(CopyCondition.GOOD)
                .build();
    }

    private LoanPolicy buildPolicy(BigDecimal finePerDay, BigDecimal maxFine) {
        return LoanPolicy.builder().borrowerType(BorrowerType.STUDENT)
                .finePerDay(finePerDay).maxFine(maxFine).build();
    }

    @Test
    @DisplayName("calculateFine() — returned on or before the due date accrues nothing")
    void calculateFine_onTime_returnsNull() {
        // ARRANGE
        circulationService = new CirculationService(libraryCustodyLogRepository, libraryFineRepository,
                loanPolicyRepository, bookCopyRepository, bookTitleRepository, catalogService,
                reservationService, auditLogWriter);
        Instant dueAt = Instant.now().plus(1, ChronoUnit.DAYS);
        LibraryCustodyLog log = buildLog(dueAt);

        // ACT
        BigDecimal fine = circulationService.calculateFine(log, Instant.now());

        // ASSERT
        assertThat(fine).isNull();
    }

    @Test
    @DisplayName("calculateFine() — days_late × fine_per_day, matching 03-srs.md [LIB-005]'s formula")
    void calculateFine_overdue_multipliesDaysLateByRate() {
        // ARRANGE
        circulationService = new CirculationService(libraryCustodyLogRepository, libraryFineRepository,
                loanPolicyRepository, bookCopyRepository, bookTitleRepository, catalogService,
                reservationService, auditLogWriter);
        Instant dueAt = Instant.now().minus(5, ChronoUnit.DAYS);
        LibraryCustodyLog log = buildLog(dueAt);
        when(loanPolicyRepository.findByBorrowerTypeAndDeletedFalse(BorrowerType.STUDENT))
                .thenReturn(Optional.of(buildPolicy(BigDecimal.valueOf(2.00), BigDecimal.valueOf(200.00))));

        // ACT — 5 days late × 2.00/day = 10.00
        BigDecimal fine = circulationService.calculateFine(log, Instant.now());

        // ASSERT
        assertThat(fine).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("calculateFine() — capped at the loan policy's max_fine")
    void calculateFine_exceedsMaxFine_isCapped() {
        // ARRANGE
        circulationService = new CirculationService(libraryCustodyLogRepository, libraryFineRepository,
                loanPolicyRepository, bookCopyRepository, bookTitleRepository, catalogService,
                reservationService, auditLogWriter);
        Instant dueAt = Instant.now().minus(200, ChronoUnit.DAYS); // way overdue
        LibraryCustodyLog log = buildLog(dueAt);
        when(loanPolicyRepository.findByBorrowerTypeAndDeletedFalse(BorrowerType.STUDENT))
                .thenReturn(Optional.of(buildPolicy(BigDecimal.valueOf(2.00), BigDecimal.valueOf(200.00))));

        // ACT — 200 days × 2.00 = 400.00, capped at 200.00
        BigDecimal fine = circulationService.calculateFine(log, Instant.now());

        // ASSERT
        assertThat(fine).isEqualByComparingTo("200.00");
    }
}
