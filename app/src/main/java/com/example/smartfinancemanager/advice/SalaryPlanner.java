package com.example.smartfinancemanager.advice;

import com.example.smartfinancemanager.utils.InputUtils;
import com.example.smartfinancemanager.utils.MoneyUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class SalaryPlanner {

    /** Percent of income for needs, wants and savings. */
    public enum Split {
        BALANCED(50, 30, 20),
        TIGHT(60, 30, 10);

        public final int needsPercent;
        public final int wantsPercent;
        public final int savingsPercent;

        Split(int needsPercent, int wantsPercent, int savingsPercent) {
            this.needsPercent = needsPercent;
            this.wantsPercent = wantsPercent;
            this.savingsPercent = savingsPercent;
        }
    }

    private SalaryPlanner() {}

    /**
     * Splits a salary by the chosen rule, then splits the savings into equity and debt using
     * the "100 minus age" rule of thumb. The savings amount absorbs any rounding difference,
     * so the three parts always add up to the salary.
     * @throws IllegalArgumentException for a non-positive salary or an unsupported age
     */
    public static SalaryPlan plan(double salary, int age, Split split) {
        if (salary <= 0) {
            throw new IllegalArgumentException("Salary must be greater than zero");
        }
        if (age < InputUtils.MIN_AGE || age > InputUtils.MAX_AGE) {
            throw new IllegalArgumentException("Age must be between "
                    + InputUtils.MIN_AGE + " and " + InputUtils.MAX_AGE);
        }

        double needs = percentOf(salary, split.needsPercent);
        double wants = percentOf(salary, split.wantsPercent);
        double savings = MoneyUtils.round2(salary - needs - wants);

        int equityPercent = 100 - age;
        int debtPercent = age;
        double equity = percentOf(savings, equityPercent);
        double debt = MoneyUtils.round2(savings - equity);

        double emergencyMin = MoneyUtils.round2(needs * AdviceEngine.EMERGENCY_MIN_MONTHS);
        double emergencyMax = MoneyUtils.round2(needs * AdviceEngine.EMERGENCY_MAX_MONTHS);

        return new SalaryPlan(salary, split.needsPercent, split.wantsPercent,
                split.savingsPercent, needs, wants, savings, equityPercent, debtPercent,
                equity, debt, emergencyMin, emergencyMax);
    }

    private static double percentOf(double amount, int percent) {
        return BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).doubleValue();
    }
}