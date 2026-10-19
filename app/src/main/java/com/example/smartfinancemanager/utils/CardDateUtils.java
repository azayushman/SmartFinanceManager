package com.example.smartfinancemanager.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Works out card statement and due dates from a day of the month (1-28). */
public final class CardDateUtils {

    private CardDateUtils() {}

    /** The most recent date on or before 'millis' that falls on dayOfMonth. */
    public static long latestOnOrBefore(int dayOfMonth, long millis) {
        checkDay(dayOfMonth);
        ZoneId zone = ZoneId.systemDefault();
        LocalDate date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate();
        LocalDate candidate = date.withDayOfMonth(dayOfMonth);
        if (candidate.isAfter(date)) {
            candidate = date.minusMonths(1).withDayOfMonth(dayOfMonth);
        }
        return candidate.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    /** The first date strictly after 'millis' that falls on dayOfMonth. */
    public static long firstAfter(int dayOfMonth, long millis) {
        checkDay(dayOfMonth);
        ZoneId zone = ZoneId.systemDefault();
        LocalDate date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate();
        LocalDate candidate = date.withDayOfMonth(dayOfMonth);
        if (!candidate.isAfter(date)) {
            candidate = date.plusMonths(1).withDayOfMonth(dayOfMonth);
        }
        return candidate.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    private static void checkDay(int dayOfMonth) {
        if (dayOfMonth < 1 || dayOfMonth > 28) {
            throw new IllegalArgumentException("Day of month must be between 1 and 28");
        }
    }
}