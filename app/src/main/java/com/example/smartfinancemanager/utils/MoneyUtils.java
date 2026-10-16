package com.example.smartfinancemanager.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyUtils {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000000"); // 10 crore

    private MoneyUtils() {}

    /**
     * Validates user input like "1,250.50" and returns it as rupees (1250.5).
     * @throws IllegalArgumentException with a message you can show under the field
     */
    public static double parseAmount(String input) {
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
        if (value.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount is too large");
        }
        return value.doubleValue();
    }

    /** Rounds to 2 decimals. Use on every total before showing or comparing it. */
    public static double round2(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** 125050.5 -> "₹1,25,050.50" (Indian digit grouping) */
    public static String format(double amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "₹" + nf.format(BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP));
    }

    /** 1250.5 -> "1250.50" (plain text for filling an input field) */
    public static String toPlainString(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}