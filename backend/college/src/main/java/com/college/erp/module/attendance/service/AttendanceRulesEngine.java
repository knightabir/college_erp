package com.college.erp.module.attendance.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.college.erp.module.attendance.domain.AttendanceRecord;
import com.college.erp.module.attendance.domain.AttendanceStatus;
import com.college.erp.module.attendance.dto.AttendanceSummaryDto;

/**
 * Pure calculation logic — no repository access, easy to unit test in isolation
 * (07-coding-standards.md §13.3 "business rule calculations"). Callers supply the record list.
 *
 * Interpretation of 03-srs.md [ATT-004] ("present count / total sessions count"), since the doc
 * doesn't disambiguate every status: HOLIDAY sessions never count in the denominator (not a real
 * class); only PRESENT counts as "present"; LATE and EXCUSED count toward the denominator but
 * neither the present nor absent bucket (matching the four fields the API doc's response actually
 * has — there's no "late"/"excused" field to put them in).
 */
@Component
public class AttendanceRulesEngine {

    public static final int DEFAULT_MIN_ATTENDANCE_PCT = 75; // matches V005's column default

    public AttendanceSummaryDto summarize(UUID studentId, UUID offeringId, List<AttendanceRecord> records,
            int minAttendancePct) {
        List<AttendanceRecord> countable = records.stream()
                .filter(r -> r.getStatus() != AttendanceStatus.HOLIDAY)
                .toList();

        int total = countable.size();
        int present = (int) countable.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
        int absent = (int) countable.stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count();
        double percentage = total == 0 ? 100.0 : roundToOneDecimal(present * 100.0 / total);

        return AttendanceSummaryDto.builder()
                .studentId(studentId)
                .offeringId(offeringId)
                .totalClasses(total)
                .present(present)
                .absent(absent)
                .attendancePercentage(percentage)
                .eligibleForExam(percentage >= minAttendancePct)
                .build();
    }

    /**
     * Current unbroken absence streak, most-recent-session-first. Used for 03-srs.md [ATT-007]
     * parent alerts. HOLIDAY sessions are skipped (they neither extend nor break a streak).
     */
    public int currentConsecutiveAbsences(List<AttendanceRecord> recordsNewestFirst) {
        int streak = 0;
        for (AttendanceRecord record : recordsNewestFirst) {
            if (record.getStatus() == AttendanceStatus.HOLIDAY) {
                continue;
            }
            if (record.getStatus() != AttendanceStatus.ABSENT) {
                break;
            }
            streak++;
        }
        return streak;
    }

    public boolean exceedsConsecutiveAbsenceThreshold(List<AttendanceRecord> recordsNewestFirst, int threshold) {
        return currentConsecutiveAbsences(recordsNewestFirst) >= threshold;
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
