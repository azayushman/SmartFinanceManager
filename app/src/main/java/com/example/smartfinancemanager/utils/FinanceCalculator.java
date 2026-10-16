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
}