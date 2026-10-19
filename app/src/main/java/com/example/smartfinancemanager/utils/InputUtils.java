package com.example.smartfinancemanager.utils;

import java.math.BigDecimal;

public final class InputUtils {

    private InputUtils() {}

    /** "10.5" -> 10.5. Accepts 0 to 100 with at most 2 decimal places. */
    public static double parseRatePercent(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter the interest rate");
        }
        BigDecimal value;
        try {
            value = new BigDecimal(input.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a valid number");
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Rate cannot be negative");
        }
        if (value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Use at most 2 decimal places");
        }
        if (value.compareTo(BigDecimal.valueOf(EmiCalculator.MAX_RATE_PERCENT)) > 0) {
            throw new IllegalArgumentException("Rate cannot be more than 100%");
        }
        return value.doubleValue();
    }

    /** Whole number of months from 1 to EmiCalculator.MAX_MONTHS. */
    public static int parseMonths(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter the tenure in months");
        }
        int value;
        try {
            value = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a whole number of months");
        }
        if (value < 1 || value > EmiCalculator.MAX_MONTHS) {
            throw new IllegalArgumentException(
                    "Tenure must be between 1 and " + EmiCalculator.MAX_MONTHS + " months");
        }
        return value;
    }

    /** A blank field means 0. Otherwise a whole number from 0 to max. */
    public static int parseNonNegativeInt(String input, int max) {
        if (input == null || input.trim().isEmpty()) {
            return 0;
        }
        int value;
        try {
            value = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a whole number");
        }
        if (value < 0) {
            throw new IllegalArgumentException("Cannot be negative");
        }
        if (value > max) {
            throw new IllegalArgumentException("Cannot be more than " + max);
        }
        return value;
    }

    /** Returns an error message, or null if the text is acceptable. */
    public static String validateRequiredText(String value, String fieldLabel, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            return "Enter " + fieldLabel;
        }
        if (value.trim().length() > maxLength) {
            return "Keep it under " + maxLength + " characters";
        }
        return null;
    }

    /** Returns an error message, or null if the optional text is acceptable. */
    public static String validateOptionalText(String value, int maxLength) {
        if (value != null && value.trim().length() > maxLength) {
            return "Keep it under " + maxLength + " characters";
        }
        return null;
    }

    /** 10.0 -> "10", 8.5 -> "8.5" */
    public static String formatPercent(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
    /** Day of the month from 1 to 28 (28 keeps the date valid in every month). */
    public static int parseDayOfMonth(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter a day of the month");
        }
        int value;
        try {
            value = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a whole number");
        }
        if (value < 1 || value > 28) {
            throw new IllegalArgumentException("Enter a day between 1 and 28");
        }
        return value;
    }

    /** Like MoneyUtils.parseAmount, but a blank field or 0 is allowed and means 0. */
    public static double parseNonNegativeAmount(String input) {
        if (input == null || input.trim().isEmpty()) {
            return 0;
        }
        BigDecimal value;
        try {
            value = new BigDecimal(input.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a valid number");
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (value.signum() == 0) {
            return 0;
        }
        return MoneyUtils.parseAmount(input);
    }
}