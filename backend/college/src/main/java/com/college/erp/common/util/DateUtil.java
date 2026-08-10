package com.college.erp.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Written once, imported everywhere a date needs formatting/parsing — never re-implemented
 * per-service. See 07-coding-standards.md §1.1.
 */
public final class DateUtil {

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DateUtil() {
    }

    public static String formatDisplayDate(LocalDate date) {
        return date == null ? null : date.format(DISPLAY_DATE);
    }

    public static String formatDisplayDateTime(Instant instant) {
        return instant == null ? null
                : DISPLAY_DATE_TIME.withZone(ZoneOffset.UTC).format(instant);
    }

    public static boolean isPast(Instant instant) {
        return instant != null && instant.isBefore(Instant.now());
    }

    public static boolean isFuture(Instant instant) {
        return instant != null && instant.isAfter(Instant.now());
    }
}
