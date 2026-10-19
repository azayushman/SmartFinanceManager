package com.example.smartfinancemanager.utils;

public final class FinanceCalculator {

    private FinanceCalculator() {}

    public static double netCashFlow(double income, double expense) {
        return MoneyUtils.round2(income - expense);
    }

    /** Returns null when income is zero, so the screen can show "N/A" instead of dividing by zero. */
    public static Double savingsRatePercent(double income, double expense) {
        if (income <= 0) {
            return null;
        }
        return MoneyUtils.round2((income - expense) / income * 100.0);
    }
    /** Cards above this share of their limit are flagged (a common rule of thumb). */
    public static final double UTILIZATION_WARNING_PERCENT = 30.0;

    /** Share of the credit limit in use. Returns null when the limit is zero. */
    public static Double utilizationPercent(double outstanding, double creditLimit) {
        if (creditLimit <= 0) {
            return null;
        }
        return MoneyUtils.round2(outstanding / creditLimit * 100.0);
    }
}