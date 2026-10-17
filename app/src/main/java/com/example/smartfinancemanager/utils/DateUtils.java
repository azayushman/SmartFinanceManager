package com.example.smartfinancemanager.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DateUtils {

    private DateUtils() {}

    /** Midnight (local time) at the start of the day containing millis. */
    public static long startOfDay(long millis) {
        ZoneId zone = ZoneId.systemDefault();
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                .atStartOfDay(zone).toInstant().toEpochMilli();
    }

    public static long startOfMonth(YearMonth month) {
        return month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /** Exclusive upper bound for a month: use as "date < startOfNextMonth". */
    public static long startOfNextMonth(YearMonth month) {
        return startOfMonth(month.plusMonths(1));
    }

    public static YearMonth currentMonth() {
        return YearMonth.now();
    }

    /** "2026-10" */
    public static String monthKey(YearMonth month) {
        return month.toString();
    }

    /** "17 Oct 2026" */
    public static String formatDate(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()));
    }

    /** Our local day -> the UTC value MaterialDatePicker expects. */
    public static long toPickerUtc(long localMillis) {
        LocalDate day = Instant.ofEpochMilli(localMillis)
                .atZone(ZoneId.systemDefault()).toLocalDate();
        return day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    /** UTC value returned by MaterialDatePicker -> our local start of day. */
    public static long fromPickerUtc(long utcMillis) {
        LocalDate day = Instant.ofEpochMilli(utcMillis)
                .atZone(ZoneOffset.UTC).toLocalDate();
        return day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    /** "October 2026" */
    /** "October 2026" */
    public static String formatMonth(YearMonth month) {
        return month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()));
    }
}