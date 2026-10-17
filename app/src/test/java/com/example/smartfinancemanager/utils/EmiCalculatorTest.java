package com.example.smartfinancemanager.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.example.smartfinancemanager.data.model.EmiInstallment;

import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class EmiCalculatorTest {

    private static long millisOf(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    @Test
    public void monthlyEmi_matchesKnownExample() {
        // 5,00,000 at 10% for 60 months
        assertEquals(10623.52, EmiCalculator.monthlyEmi(500000, 10, 60), 0.0);
    }

    @Test
    public void monthlyEmi_zeroRateIsPrincipalDividedByMonths() {
        assertEquals(1000.0, EmiCalculator.monthlyEmi(12000, 0, 12), 0.0);
    }

    @Test
    public void monthlyEmi_rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> EmiCalculator.monthlyEmi(0, 10, 12));
        assertThrows(IllegalArgumentException.class, () -> EmiCalculator.monthlyEmi(1000, -1, 12));
        assertThrows(IllegalArgumentException.class, () -> EmiCalculator.monthlyEmi(1000, 101, 12));
        assertThrows(IllegalArgumentException.class, () -> EmiCalculator.monthlyEmi(1000, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> EmiCalculator.monthlyEmi(1000, 10, 481));
    }

    @Test
    public void schedule_firstInstalmentsMatchHandCalculation() {
        long start = millisOf(LocalDate.of(2026, 1, 15));
        List<EmiInstallment> schedule = EmiCalculator.buildSchedule(500000, 10, 60, start);

        assertEquals(60, schedule.size());
        EmiInstallment first = schedule.get(0);
        assertEquals(1, first.getInstallmentNo());
        assertEquals(10623.52, first.getEmiAmount(), 0.0);
        assertEquals(4166.67, first.getInterestAmount(), 0.0);
        assertEquals(6456.85, first.getPrincipalAmount(), 0.0);
        assertEquals(493543.15, first.getBalanceAmount(), 0.0);

        assertEquals(480467.57, schedule.get(2).getBalanceAmount(), 0.0);
    }

    @Test
    public void schedule_alwaysRepaysTheExactPrincipal() {
        long start = millisOf(LocalDate.of(2026, 1, 15));
        List<EmiInstallment> schedule = EmiCalculator.buildSchedule(500000, 10, 60, start);

        BigDecimal principalPaid = BigDecimal.ZERO;
        for (EmiInstallment item : schedule) {
            principalPaid = principalPaid.add(BigDecimal.valueOf(item.getPrincipalAmount()));
        }
        assertEquals(0, principalPaid.compareTo(new BigDecimal("500000")));
        assertEquals(0.0, schedule.get(59).getBalanceAmount(), 0.0);
        assertTrue(EmiCalculator.totalInterest(schedule) > 0);
    }

    @Test
    public void schedule_lastInstalmentAbsorbsRounding() {
        long start = millisOf(LocalDate.of(2026, 1, 15));
        List<EmiInstallment> schedule = EmiCalculator.buildSchedule(10000, 0, 3, start);

        assertEquals(3333.33, schedule.get(0).getEmiAmount(), 0.0);
        assertEquals(3333.33, schedule.get(1).getEmiAmount(), 0.0);
        assertEquals(3333.34, schedule.get(2).getEmiAmount(), 0.0);
        assertEquals(0.0, schedule.get(2).getBalanceAmount(), 0.0);
        assertEquals(0.0, EmiCalculator.totalInterest(schedule), 0.0);
    }

    @Test
    public void schedule_dueDatesClampToShortMonths() {
        long start = millisOf(LocalDate.of(2026, 1, 31));
        List<EmiInstallment> schedule = EmiCalculator.buildSchedule(12000, 0, 3, start);

        assertEquals(millisOf(LocalDate.of(2026, 2, 28)), schedule.get(0).getDueDate());
        assertEquals(millisOf(LocalDate.of(2026, 3, 31)), schedule.get(1).getDueDate());
        assertEquals(millisOf(LocalDate.of(2026, 4, 30)), schedule.get(2).getDueDate());
    }
}