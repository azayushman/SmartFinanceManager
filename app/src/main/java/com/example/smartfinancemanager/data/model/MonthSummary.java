package com.example.smartfinancemanager.data.model;

import com.example.smartfinancemanager.utils.FinanceCalculator;
import com.example.smartfinancemanager.utils.MoneyUtils;

/** Totals for one month, calculated from the stored transactions. */
public class MonthSummary {

    private final double income;
    private final double expense;
    private final double netCashFlow;
    private final Double savingsRatePercent; // null when income is zero

    public MonthSummary(double income, double expense) {
        this.income = MoneyUtils.round2(income);
        this.expense = MoneyUtils.round2(expense);
        this.netCashFlow = FinanceCalculator.netCashFlow(income, expense);
        this.savingsRatePercent = FinanceCalculator.savingsRatePercent(income, expense);
    }

    public double getIncome() { return income; }
    public double getExpense() { return expense; }
    public double getNetCashFlow() { return netCashFlow; }

    /** Null means "N/A" (no income recorded). */
    public Double getSavingsRatePercent() { return savingsRatePercent; }
}