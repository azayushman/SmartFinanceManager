package com.example.smartfinancemanager.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;

public class CardDateUtilsTest {

    private static long millisOf(int year, int month, int day) {
        return LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli();
    }

    @Test
    public void latestOnOrBefore_sameMonth() {
        assertEquals(millisOf(2026, 10, 15),
                CardDateUtils.latestOnOrBefore(15, millisOf(2026, 10, 20)));
    }

    @Test
    public void latestOnOrBefore_onTheDayItself() {
        assertEquals(millisOf(2026, 10, 15),
                CardDateUtils.latestOnOrBefore(15, millisOf(2026, 10, 15)));
    }

    @Test
    public void latestOnOrBefore_fallsBackToPreviousMonth() {
        assertEquals(millisOf(2026, 9, 15),
                CardDateUtils.latestOnOrBefore(15, millisOf(2026, 10, 10)));
        assertEquals(millisOf(2025, 12, 28),
                CardDateUtils.latestOnOrBefore(28, millisOf(2026, 1, 5)));
    }

    @Test
    public void firstAfter_laterInSameMonth() {
        assertEquals(millisOf(2026, 10, 25),
                CardDateUtils.firstAfter(25, millisOf(2026, 10, 15)));
    }

    @Test
    public void firstAfter_rollsIntoNextMonth() {
        assertEquals(millisOf(2026, 11, 5),
                CardDateUtils.firstAfter(5, millisOf(2026, 10, 15)));
        assertEquals(millisOf(2026, 11, 15),
                CardDateUtils.firstAfter(15, millisOf(2026, 10, 15)));
        assertEquals(millisOf(2027, 1, 5),
                CardDateUtils.firstAfter(5, millisOf(2026, 12, 20)));
    }

    @Test
    public void rejectsDaysOutsideOneToTwentyEight() {
        assertThrows(IllegalArgumentException.class,
                () -> CardDateUtils.latestOnOrBefore(0, millisOf(2026, 10, 15)));
        assertThrows(IllegalArgumentException.class,
                () -> CardDateUtils.firstAfter(29, millisOf(2026, 10, 15)));
    }
}