package com.example.smartfinancemanager.utils;

import java.math.BigDecimal;

public final class BudgetCalculator {

    public static final double NEAR_LIMIT_PERCENT = 80.0;
    public static final double OVER_LIMIT_PERCENT = 100.0;

    public enum Status { NORMAL, NEAR_LIMIT, OVER }

    private BudgetCalculator() {}

    /** Percent of the limit used, rounded to 2 decimals. Null when the limit is not positive. */
    public static Double usagePercent(double spent, double limit) {
        if (limit <= 0) {
            return null;
        }
        return MoneyUtils.round2(spent / limit * 100.0);
    }

    /** Money left in the budget. Negative when overspent. */
    public static double remaining(double spent, double limit) {
        return MoneyUtils.round2(limit - spent);
    }

    /** NORMAL below 80%, NEAR_LIMIT from 80% up to 100%, OVER at 100% or more. */
    public static Status status(double spent, double limit) {
        if (limit <= 0) {
            return spent > 0 ? Status.OVER : Status.NORMAL;
        }
        BigDecimal used = BigDecimal.valueOf(spent).multiply(BigDecimal.valueOf(100));
        BigDecimal limitValue = BigDecimal.valueOf(limit);
        if (used.compareTo(limitValue.multiply(BigDecimal.valueOf(OVER_LIMIT_PERCENT))) >= 0) {
            return Status.OVER;
        }
        if (used.compareTo(limitValue.multiply(BigDecimal.valueOf(NEAR_LIMIT_PERCENT))) >= 0) {
            return Status.NEAR_LIMIT;
        }
        return Status.NORMAL;
    }
}