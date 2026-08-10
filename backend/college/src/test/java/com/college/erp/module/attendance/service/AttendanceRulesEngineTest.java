package com.college.erp.module.attendance.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.college.erp.module.attendance.domain.AttendanceRecord;
import com.college.erp.module.attendance.domain.AttendanceSession;
import com.college.erp.module.attendance.domain.AttendanceStatus;
import com.college.erp.module.attendance.dto.AttendanceSummaryDto;

import static org.assertj.core.api.Assertions.assertThat;

class AttendanceRulesEngineTest {

    private final AttendanceRulesEngine rulesEngine = new AttendanceRulesEngine();

    private AttendanceRecord recordWithStatus(AttendanceStatus status) {
        return recordWithStatus(status, LocalDate.now());
    }

    private AttendanceRecord recordWithStatus(AttendanceStatus status, LocalDate date) {
        AttendanceSession session = AttendanceSession.builder().sessionDate(date).build();
        return AttendanceRecord.builder().session(session).status(status).build();
    }

    @Test
    @DisplayName("summarize() — matches the worked example in 05-api-documentation.md §5 (38/45 = 84.4%)")
    void summarize_matchesDocumentedExample() {
        // ARRANGE — 38 present, 7 absent = 45 total
        List<AttendanceRecord> records = new java.util.ArrayList<>();
        for (int i = 0; i < 38; i++) {
            records.add(recordWithStatus(AttendanceStatus.PRESENT));
        }
        for (int i = 0; i < 7; i++) {
            records.add(recordWithStatus(AttendanceStatus.ABSENT));
        }
        UUID studentId = UUID.randomUUID();
        UUID offeringId = UUID.randomUUID();

        // ACT
        AttendanceSummaryDto summary = rulesEngine.summarize(studentId, offeringId, records, 75);

        // ASSERT
        assertThat(summary.getTotalClasses()).isEqualTo(45);
        assertThat(summary.getPresent()).isEqualTo(38);
        assertThat(summary.getAbsent()).isEqualTo(7);
        assertThat(summary.getAttendancePercentage()).isEqualTo(84.4);
        assertThat(summary.isEligibleForExam()).isTrue();
    }

    @Test
    @DisplayName("summarize() — HOLIDAY records are excluded from the denominator entirely")
    void summarize_excludesHolidays() {
        // ARRANGE
        List<AttendanceRecord> records = List.of(
                recordWithStatus(AttendanceStatus.PRESENT),
                recordWithStatus(AttendanceStatus.PRESENT),
                recordWithStatus(AttendanceStatus.HOLIDAY),
                recordWithStatus(AttendanceStatus.HOLIDAY));

        // ACT
        AttendanceSummaryDto summary = rulesEngine.summarize(UUID.randomUUID(), UUID.randomUUID(), records, 75);

        // ASSERT
        assertThat(summary.getTotalClasses()).isEqualTo(2);
        assertThat(summary.getAttendancePercentage()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("summarize() — below the minimum percentage is not eligible for exam")
    void summarize_belowMinimum_notEligible() {
        // ARRANGE — 60/100 = 60%, below default 75%
        List<AttendanceRecord> records = new java.util.ArrayList<>();
        for (int i = 0; i < 60; i++) {
            records.add(recordWithStatus(AttendanceStatus.PRESENT));
        }
        for (int i = 0; i < 40; i++) {
            records.add(recordWithStatus(AttendanceStatus.ABSENT));
        }

        // ACT
        AttendanceSummaryDto summary = rulesEngine.summarize(UUID.randomUUID(), UUID.randomUUID(), records, 75);

        // ASSERT
        assertThat(summary.getAttendancePercentage()).isEqualTo(60.0);
        assertThat(summary.isEligibleForExam()).isFalse();
    }

    @Test
    @DisplayName("summarize() — no records at all defaults to 100% (nothing to be absent from yet)")
    void summarize_noRecords_defaultsToFullAttendance() {
        AttendanceSummaryDto summary = rulesEngine.summarize(UUID.randomUUID(), UUID.randomUUID(), List.of(), 75);

        assertThat(summary.getTotalClasses()).isZero();
        assertThat(summary.getAttendancePercentage()).isEqualTo(100.0);
        assertThat(summary.isEligibleForExam()).isTrue();
    }

    @Test
    @DisplayName("currentConsecutiveAbsences() — counts the unbroken streak from the most recent session")
    void currentConsecutiveAbsences_countsFromMostRecent() {
        // ARRANGE — newest first: ABSENT, ABSENT, ABSENT, PRESENT, ABSENT
        List<AttendanceRecord> newestFirst = List.of(
                recordWithStatus(AttendanceStatus.ABSENT),
                recordWithStatus(AttendanceStatus.ABSENT),
                recordWithStatus(AttendanceStatus.ABSENT),
                recordWithStatus(AttendanceStatus.PRESENT),
                recordWithStatus(AttendanceStatus.ABSENT));

        // ACT / ASSERT
        assertThat(rulesEngine.currentConsecutiveAbsences(newestFirst)).isEqualTo(3);
    }

    @Test
    @DisplayName("currentConsecutiveAbsences() — a HOLIDAY in the middle of a streak doesn't break it")
    void currentConsecutiveAbsences_holidayDoesNotBreakStreak() {
        List<AttendanceRecord> newestFirst = List.of(
                recordWithStatus(AttendanceStatus.ABSENT),
                recordWithStatus(AttendanceStatus.HOLIDAY),
                recordWithStatus(AttendanceStatus.ABSENT),
                recordWithStatus(AttendanceStatus.PRESENT));

        assertThat(rulesEngine.currentConsecutiveAbsences(newestFirst)).isEqualTo(2);
    }

    @Test
    @DisplayName("exceedsConsecutiveAbsenceThreshold() — reaching exactly the threshold triggers it")
    void exceedsConsecutiveAbsenceThreshold_atThreshold_triggers() {
        List<AttendanceRecord> newestFirst = List.of(
                recordWithStatus(AttendanceStatus.ABSENT),
                recordWithStatus(AttendanceStatus.ABSENT),
                recordWithStatus(AttendanceStatus.ABSENT));

        assertThat(rulesEngine.exceedsConsecutiveAbsenceThreshold(newestFirst, 3)).isTrue();
    }

    @Test
    @DisplayName("exceedsConsecutiveAbsenceThreshold() — below the threshold does not trigger")
    void exceedsConsecutiveAbsenceThreshold_belowThreshold_doesNotTrigger() {
        List<AttendanceRecord> newestFirst = List.of(
                recordWithStatus(AttendanceStatus.ABSENT),
                recordWithStatus(AttendanceStatus.ABSENT),
                recordWithStatus(AttendanceStatus.PRESENT));

        assertThat(rulesEngine.exceedsConsecutiveAbsenceThreshold(newestFirst, 3)).isFalse();
    }
}
