package com.example.smartfinancemanager.advice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class SalaryPlannerTest {

    @Test
    public void balancedSplit_workedExample() {
        SalaryPlan plan = SalaryPlanner.plan(50000, 30, SalaryPlanner.Split.BALANCED);
        assertEquals(25000.0, plan.getNeedsAmount(), 0.0);
        assertEquals(15000.0, plan.getWantsAmount(), 0.0);
        assertEquals(10000.0, plan.getSavingsAmount(), 0.0);
        assertEquals(70, plan.getEquityPercent());
        assertEquals(30, plan.getDebtPercent());
        assertEquals(7000.0, plan.getEquityAmount(), 0.0);
        assertEquals(3000.0, plan.getDebtAmount(), 0.0);
        assertEquals(75000.0, plan.getEmergencyFundMin(), 0.0);
        assertEquals(150000.0, plan.getEmergencyFundMax(), 0.0);
    }

    @Test
    public void tightSplit_usesSixtyThirtyTen() {
        SalaryPlan plan = SalaryPlanner.plan(50000, 30, SalaryPlanner.Split.TIGHT);
        assertEquals(30000.0, plan.getNeedsAmount(), 0.0);
        assertEquals(15000.0, plan.getWantsAmount(), 0.0);
        assertEquals(5000.0, plan.getSavingsAmount(), 0.0);
        assertEquals(3500.0, plan.getEquityAmount(), 0.0);
        assertEquals(1500.0, plan.getDebtAmount(), 0.0);
    }

    @Test
    public void partsAlwaysAddUpDespiteRounding() {
        SalaryPlan plan = SalaryPlanner.plan(33333.33, 27, SalaryPlanner.Split.BALANCED);
        double total = plan.getNeedsAmount() + plan.getWantsAmount() + plan.getSavingsAmount();
        assertEquals(33333.33, total, 0.001);
        assertEquals(plan.getSavingsAmount(),
                plan.getEquityAmount() + plan.getDebtAmount(), 0.001);
    }

    @Test
    public void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class,
                () -> SalaryPlanner.plan(0, 30, SalaryPlanner.Split.BALANCED));
        assertThrows(IllegalArgumentException.class,
                () -> SalaryPlanner.plan(50000, 17, SalaryPlanner.Split.BALANCED));
        assertThrows(IllegalArgumentException.class,
                () -> SalaryPlanner.plan(50000, 81, SalaryPlanner.Split.BALANCED));
    }
}