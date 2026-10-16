package com.example.smartfinancemanager.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.YearMonth;

public class DateUtilsTest {

    @Test
    public void pickerConversion_roundTripKeepsTheSameDay() {
        long local = DateUtils.startOfDay(System.currentTimeMillis());
        assertEquals(local, DateUtils.fromPickerUtc(DateUtils.toPickerUtc(local)));
    }

    @Test
    public void startOfDay_isIdempotent() {
        long once = DateUtils.startOfDay(System.currentTimeMillis());
        assertEquals(once, DateUtils.startOfDay(once));
    }

    @Test
    public void monthBounds_areConsecutive() {
        YearMonth october = YearMonth.of(2026, 10);
        assertEquals(DateUtils.startOfMonth(YearMonth.of(2026, 11)),
                DateUtils.startOfNextMonth(october));
        assertTrue(DateUtils.startOfNextMonth(october) > DateUtils.startOfMonth(october));
    }

    @Test
    public void monthKey_hasYearDashMonth() {
        assertEquals("2026-10", DateUtils.monthKey(YearMonth.of(2026, 10)));
    }
}