package com.example.smartfinancemanager.utils;

import com.example.smartfinancemanager.data.model.EmiInstallment;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public final class EmiCalculator {

    public static final int MAX_MONTHS = 480;            // 40 years
    public static final double MAX_RATE_PERCENT = 100.0;

    private static final MathContext MC = MathContext.DECIMAL128;

    private EmiCalculator() {}

    /**
     * EMI = P * r * (1+r)^n / ((1+r)^n - 1), where r is the monthly rate.
     * With a 0% rate the EMI is simply P / n.
     * @throws IllegalArgumentException for values outside the supported range
     */
    public static double monthlyEmi(double principal, double annualRatePercent, int months) {
        validate(principal, annualRatePercent, months);
        BigDecimal p = BigDecimal.valueOf(principal);
        BigDecimal emi;
        if (annualRatePercent == 0) {
            emi = p.divide(BigDecimal.valueOf(months), MC);
        } else {
            BigDecimal r = monthlyRate(annualRatePercent);
            BigDecimal factor = BigDecimal.ONE.add(r).pow(months, MC);
            emi = p.multiply(r).multiply(factor)
                    .divide(factor.subtract(BigDecimal.ONE), MC);
        }
        return emi.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Builds the full month-by-month schedule. Instalment i is due i months after
     * the start date (same day of the month, or the last day if the month is shorter).
     */
    public static List<EmiInstallment> buildSchedule(double principal, double annualRatePercent,
                                                     int months, long startDateMillis) {
        double emi = monthlyEmi(principal, annualRatePercent, months); // also validates
        ZoneId zone = ZoneId.systemDefault();
        LocalDate start = Instant.ofEpochMilli(startDateMillis).atZone(zone).toLocalDate();

        BigDecimal monthlyRate = monthlyRate(annualRatePercent);
        BigDecimal emiAmount = BigDecimal.valueOf(emi);
        BigDecimal balance = BigDecimal.valueOf(principal).setScale(2, RoundingMode.HALF_UP);

        List<EmiInstallment> schedule = new ArrayList<>(months);
        for (int i = 1; i <= months; i++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPart = emiAmount.subtract(interest);
            BigDecimal payment = emiAmount;

            // Last instalment (or a rounding overshoot) clears whatever is left
            if (i == months || principalPart.compareTo(balance) > 0) {
                principalPart = balance;
                payment = principalPart.add(interest);
            }
            balance = balance.subtract(principalPart);

            long dueDate = start.plusMonths(i).atStartOfDay(zone).toInstant().toEpochMilli();
            schedule.add(new EmiInstallment(0, 0, i, dueDate,
                    payment.doubleValue(), principalPart.doubleValue(),
                    interest.doubleValue(), balance.doubleValue(), false, 0));
        }
        return schedule;
    }

    public static double totalInterest(List<EmiInstallment> schedule) {
        BigDecimal total = BigDecimal.ZERO;
        for (EmiInstallment item : schedule) {
            total = total.add(BigDecimal.valueOf(item.getInterestAmount()));
        }
        return total.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static BigDecimal monthlyRate(double annualRatePercent) {
        return BigDecimal.valueOf(annualRatePercent).divide(BigDecimal.valueOf(1200), MC);
    }

    private static void validate(double principal, double annualRatePercent, int months) {
        if (principal <= 0) {
            throw new IllegalArgumentException("Loan amount must be greater than zero");
        }
        if (annualRatePercent < 0 || annualRatePercent > MAX_RATE_PERCENT) {
            throw new IllegalArgumentException("Interest rate must be between 0 and 100");
        }
        if (months < 1 || months > MAX_MONTHS) {
            throw new IllegalArgumentException("Tenure must be between 1 and " + MAX_MONTHS + " months");
        }
    }
}