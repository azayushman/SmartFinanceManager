package com.example.smartfinancemanager.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class InputUtilsTest {

    @Test
    public void parseRatePercent_acceptsValidRates() {
        assertEquals(10.5, InputUtils.parseRatePercent("10.5"), 0.0);
        assertEquals(0.0, InputUtils.parseRatePercent("0"), 0.0);
        assertEquals(100.0, InputUtils.parseRatePercent("100"), 0.0);
    }

    @Test
    public void parseRatePercent_rejectsBadInput() {
        String[] bad = {"", "  ", "abc", "-1", "100.01", "7.555"};
        for (String input : bad) {
            assertThrows("Should reject: " + input, IllegalArgumentException.class,
                    () -> InputUtils.parseRatePercent(input));
        }
        assertThrows(IllegalArgumentException.class, () -> InputUtils.parseRatePercent(null));
    }

    @Test
    public void parseMonths_rules() {
        assertEquals(60, InputUtils.parseMonths("60"));
        assertEquals(1, InputUtils.parseMonths("1"));
        assertEquals(480, InputUtils.parseMonths("480"));
        String[] bad = {"", "0", "481", "abc", "12.5", "-3"};
        for (String input : bad) {
            assertThrows("Should reject: " + input, IllegalArgumentException.class,
                    () -> InputUtils.parseMonths(input));
        }
    }

    @Test
    public void parseNonNegativeInt_rules() {
        assertEquals(0, InputUtils.parseNonNegativeInt("", 5));
        assertEquals(0, InputUtils.parseNonNegativeInt(null, 5));
        assertEquals(3, InputUtils.parseNonNegativeInt("3", 5));
        assertEquals(5, InputUtils.parseNonNegativeInt("5", 5));
        String[] bad = {"-1", "6", "x", "1.5"};
        for (String input : bad) {
            assertThrows("Should reject: " + input, IllegalArgumentException.class,
                    () -> InputUtils.parseNonNegativeInt(input, 5));
        }
    }

    @Test
    public void textValidation() {
        assertNotNull(InputUtils.validateRequiredText("", "the loan name", 40));
        assertNotNull(InputUtils.validateRequiredText("   ", "the loan name", 40));
        assertNull(InputUtils.validateRequiredText("Car loan", "the loan name", 40));
        String tooLong = new String(new char[41]).replace('\0', 'a');
        assertNotNull(InputUtils.validateRequiredText(tooLong, "the loan name", 40));
        assertNull(InputUtils.validateOptionalText(null, 40));
        assertNotNull(InputUtils.validateOptionalText(tooLong, 40));
    }

    @Test
    public void formatPercent_dropsUselessZeros() {
        assertEquals("10", InputUtils.formatPercent(10.0));
        assertEquals("8.5", InputUtils.formatPercent(8.5));
        assertEquals("0", InputUtils.formatPercent(0.0));
    }
}