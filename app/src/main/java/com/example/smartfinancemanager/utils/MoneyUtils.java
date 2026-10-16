package com.example.smartfinancemanager.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyUtils {

    private static final BigDecimal MAX_RUPEES = new BigDecimal("100000000"); // 10 crore

    private MoneyUtils() {}

    /**
     * Converts user input like "1,250.50" to paise (125050).
     * @throws IllegalArgumentException with a message you can show under the field
     */
    public static long parseRupeesToPaise(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter an amount");
        }
        BigDecimal value;
        try {
            value = new BigDecimal(input.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a valid number");
        }
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Use at most 2 decimal places");
        }
        if (value.compareTo(MAX_RUPEES) > 0) {
            throw new IllegalArgumentException("Amount is too large");
        }
        return value.movePointRight(2).longValueExact();
    }

    /** 125050 -> "₹1,250.50" (Indian digit grouping) */
    public static String formatPaise(long paise) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "₹" + nf.format(BigDecimal.valueOf(paise, 2));
    }
    /** 1250.5 -> "1250.50" (plain text for filling an input field) */
    public static String toPlainString(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}