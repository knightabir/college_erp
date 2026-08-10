package com.college.erp.module.library.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 03-srs.md [LIB-003]. Named to match 08-system-architecture.md's test tree — verifies
 * LibraryCustodyLog's Java-level partial immutability (core fields fixed at construction, only
 * recordReturn()/extendDueDate() ever mutate anything), mirroring what the DB trigger enforces.
 */
class CustodyLogTest {

    private LibraryCustodyLog buildActiveLog() {
        BookCopy copy = BookCopy.builder().barcode("LIB-2026-000001").accessionNumber("2026-000001").build();
        Instant issuedAt = Instant.now().minus(20, ChronoUnit.DAYS);
        return LibraryCustodyLog.builder()
                .copy(copy)
                .borrowerUserId(UUID.randomUUID())
                .borrowerName("Arjun Kumar")
                .borrowerType(BorrowerType.STUDENT)
                .issuedByUserId(UUID.randomUUID())
                .issuedAt(issuedAt)
                .dueAt(issuedAt.plus(14, ChronoUnit.DAYS))
                .conditionOnIssue(CopyCondition.GOOD)
                .build();
    }

    @Test
    @DisplayName("recordReturn() — sets only the return fields, core fields untouched")
    void recordReturn_setsOnlyReturnFields() {
        // ARRANGE
        LibraryCustodyLog log = buildActiveLog();
        UUID originalBorrower = log.getBorrowerUserId();
        BookCopy originalCopy = log.getCopy();
        Instant originalIssuedAt = log.getIssuedAt();
        UUID returnedByUserId = UUID.randomUUID();
        Instant returnedAt = Instant.now();

        // ACT
        log.recordReturn(returnedByUserId, returnedAt, CopyCondition.GOOD, BigDecimal.valueOf(12.00));

        // ASSERT — return fields set
        assertThat(log.getReturnedByUserId()).isEqualTo(returnedByUserId);
        assertThat(log.getReturnedAt()).isEqualTo(returnedAt);
        assertThat(log.getConditionOnReturn()).isEqualTo(CopyCondition.GOOD);
        assertThat(log.getFineAmount()).isEqualByComparingTo("12.00");
        // ASSERT — core fields unchanged
        assertThat(log.getBorrowerUserId()).isEqualTo(originalBorrower);
        assertThat(log.getCopy()).isEqualTo(originalCopy);
        assertThat(log.getIssuedAt()).isEqualTo(originalIssuedAt);
    }

    @Test
    @DisplayName("extendDueDate() — only changes due_at, everything else including issuedAt is untouched")
    void extendDueDate_onlyChangesDueDate() {
        // ARRANGE
        LibraryCustodyLog log = buildActiveLog();
        Instant originalIssuedAt = log.getIssuedAt();
        Instant newDueAt = log.getDueAt().plus(14, ChronoUnit.DAYS);

        // ACT
        log.extendDueDate(newDueAt);

        // ASSERT
        assertThat(log.getDueAt()).isEqualTo(newDueAt);
        assertThat(log.getIssuedAt()).isEqualTo(originalIssuedAt);
    }

    @Test
    @DisplayName("isReturned() / isOverdue() — reflect returnedAt and dueAt correctly")
    void isReturnedAndIsOverdue() {
        // ARRANGE
        LibraryCustodyLog log = buildActiveLog(); // due 6 days ago (issued 20 days ago, 14-day loan)

        // ACT / ASSERT — not yet returned, and overdue as of now
        assertThat(log.isReturned()).isFalse();
        assertThat(log.isOverdue(Instant.now())).isTrue();

        // ACT — return it
        log.recordReturn(UUID.randomUUID(), Instant.now(), CopyCondition.GOOD, null);

        // ASSERT — no longer counted as overdue once returned
        assertThat(log.isReturned()).isTrue();
        assertThat(log.isOverdue(Instant.now())).isFalse();
    }
}
