package com.example.smartfinancemanager.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class FinanceCalculatorTest {

    @Test
    public void netCashFlow_isIncomeMinusExpense() {
        assertEquals(17000.0, FinanceCalculator.netCashFlow(30000, 13000), 0.0);
    }

    @Test
    public void savingsRate_matchesWorkedExample() {
        Double rate = FinanceCalculator.savingsRatePercent(30000, 13000);
        assertEquals(56.67, rate.doubleValue(), 0.0);
    }

    @Test
    public void savingsRate_isNullWhenIncomeIsZero() {
        assertNull(FinanceCalculator.savingsRatePercent(0, 500));
    }

    @Test
    public void savingsRate_isNegativeWhenSpendingExceedsIncome() {
        Double rate = FinanceCalculator.savingsRatePercent(10000, 12000);
        assertEquals(-20.0, rate.doubleValue(), 0.0);
    }
}