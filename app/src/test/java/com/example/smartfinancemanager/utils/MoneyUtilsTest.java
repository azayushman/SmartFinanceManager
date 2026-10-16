package com.example.smartfinancemanager.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class MoneyUtilsTest {

    @Test
    public void parseAmount_acceptsValidInput() {
        assertEquals(1250.50, MoneyUtils.parseAmount("1250.50"), 0.0);
        assertEquals(1250.0, MoneyUtils.parseAmount("1,250"), 0.0);
        assertEquals(0.5, MoneyUtils.parseAmount("0.5"), 0.0);
        assertEquals(100000000.0, MoneyUtils.parseAmount("100000000"), 0.0);
    }

    @Test
    public void parseAmount_rejectsBadInput() {
        String[] bad = {"", "   ", "abc", "0", "-5", "10.555", "100000001"};
        for (String input : bad) {
            assertThrows("Should reject: " + input, IllegalArgumentException.class,
                    () -> MoneyUtils.parseAmount(input));
        }
        assertThrows(IllegalArgumentException.class, () -> MoneyUtils.parseAmount(null));
    }

    @Test
    public void round2_removesFloatingPointNoise() {
        assertEquals(0.3, MoneyUtils.round2(0.1 + 0.2), 0.0);
        assertEquals(2.68, MoneyUtils.round2(2.675), 0.0);
    }

    @Test
    public void format_usesRupeeSymbolAndTwoDecimals() {
        assertEquals("\u20B91,250.50", MoneyUtils.format(1250.5));
        assertEquals("\u20B90.00", MoneyUtils.format(0));
    }

    @Test
    public void toPlainString_alwaysHasTwoDecimals() {
        assertEquals("1250.50", MoneyUtils.toPlainString(1250.5));
        assertEquals("0.30", MoneyUtils.toPlainString(0.1 + 0.2));
    }
}