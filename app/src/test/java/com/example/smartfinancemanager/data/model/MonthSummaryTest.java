package com.example.smartfinancemanager.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MonthSummaryTest {

    @Test
    public void computesTotalsFromIncomeAndExpense() {
        MonthSummary summary = new MonthSummary(30000, 13000);
        assertEquals(30000.0, summary.getIncome(), 0.0);
        assertEquals(13000.0, summary.getExpense(), 0.0);
        assertEquals(17000.0, summary.getNetCashFlow(), 0.0);
        assertEquals(56.67, summary.getSavingsRatePercent().doubleValue(), 0.0);
    }

    @Test
    public void savingsRateIsNullWhenThereIsNoIncome() {
        MonthSummary summary = new MonthSummary(0, 500);
        assertEquals(-500.0, summary.getNetCashFlow(), 0.0);
        assertNull(summary.getSavingsRatePercent());
    }
}