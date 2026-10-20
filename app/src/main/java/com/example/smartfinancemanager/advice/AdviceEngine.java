package com.example.smartfinancemanager.advice;

import com.example.smartfinancemanager.advice.AdviceItem.Severity;
import com.example.smartfinancemanager.data.model.BudgetUsage;
import com.example.smartfinancemanager.utils.BudgetCalculator;
import com.example.smartfinancemanager.utils.FinanceCalculator;
import com.example.smartfinancemanager.utils.MoneyUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class AdviceEngine {

    public static final String DISCLAIMER =
            "General guidance based on common rules of thumb. It is not personal financial advice.";

    public static final double SAVINGS_TARGET_PERCENT = 20.0;
    public static final double SAVINGS_LOW_PERCENT = 10.0;
    public static final double NEEDS_TARGET_PERCENT = 50.0;
    public static final double WANTS_TARGET_PERCENT = 30.0;
    public static final double EMI_LIMIT_PERCENT = 40.0;
    public static final double EMI_CAUTION_PERCENT = 30.0;
    public static final double EMERGENCY_MIN_MONTHS = 3.0;
    public static final double EMERGENCY_MAX_MONTHS = 6.0;

    private AdviceEngine() {}

    /** Returns the advice for the given data, most urgent first. */
    public static List<AdviceItem> generate(AdviceInput in) {
        List<AdviceItem> items = new ArrayList<>();

        if (in.monthlyIncome > 0) {
            addSavingsAdvice(items, in);
            addSplitAdvice(items, in);
            addEmiAdvice(items, in);
        } else {
            items.add(new AdviceItem(Severity.INFO, "No income recorded",
                    "Add this month's income to get savings and spending-split advice."));
        }
        addCardAdvice(items, in);
        addEmergencyFundAdvice(items, in);
        addBudgetAdvice(items, in);

        items.sort(Comparator.comparingInt((AdviceItem item) -> item.getSeverity().ordinal()));
        return items;
    }

    private static void addSavingsAdvice(List<AdviceItem> items, AdviceInput in) {
        double income = in.monthlyIncome;
        double living = in.needsSpent + in.wantsSpent;
        double saved = MoneyUtils.round2(income - living);
        double rate = FinanceCalculator.savingsRatePercent(income, living);
        double target = MoneyUtils.round2(income * SAVINGS_TARGET_PERCENT / 100.0);
        double starter = MoneyUtils.round2(income * SAVINGS_LOW_PERCENT / 100.0);

        if (rate >= SAVINGS_TARGET_PERCENT) {
            items.add(new AdviceItem(Severity.GOOD, "Savings on track",
                    "After needs and wants you are left with " + money(saved) + ", which is "
                            + pct(rate) + " of your income. The guideline is at least 20%."));
        } else if (rate >= SAVINGS_LOW_PERCENT) {
            double gap = MoneyUtils.round2(target - saved);
            items.add(new AdviceItem(Severity.WARNING, "Savings below the 20% guideline",
                    "After needs and wants you are left with " + money(saved) + " (" + pct(rate)
                            + "). The guideline is 20% (" + money(target) + "), which is "
                            + money(gap) + " more per month. Start from next month: move "
                            + money(target) + " into savings as soon as your income arrives, "
                            + "before spending on anything else."));
        } else if (saved < 0) {
            items.add(new AdviceItem(Severity.ALERT, "Savings are very low",
                    "You spent " + money(-saved) + " more than you earned on needs and wants "
                            + "this month. Start saving from next month: cut wants first, begin "
                            + "with 10% (" + money(starter) + ") and build up to 20% ("
                            + money(target) + ")."));
        } else {
            items.add(new AdviceItem(Severity.ALERT, "Savings are very low",
                    "After needs and wants you are left with only " + money(saved) + " ("
                            + pct(rate) + "). Start saving from next month: begin with 10% ("
                            + money(starter) + ") and build up to the 20% guideline ("
                            + money(target) + ")."));
        }
    }

    private static void addSplitAdvice(List<AdviceItem> items, AdviceInput in) {
        double income = in.monthlyIncome;
        double living = in.needsSpent + in.wantsSpent;
        double needsPct = percentOf(in.needsSpent, income);
        double wantsPct = percentOf(in.wantsSpent, income);
        double savingsPct = FinanceCalculator.savingsRatePercent(income, living);

        String lines = "Needs: " + pct(needsPct) + " (guideline 50%)\n"
                + "Wants: " + pct(wantsPct) + " (guideline 30%)\n"
                + "Savings: " + pct(savingsPct) + " (guideline 20%)";

        boolean needsHigh = needsPct > NEEDS_TARGET_PERCENT;
        boolean wantsHigh = wantsPct > WANTS_TARGET_PERCENT;
        if (needsHigh || wantsHigh) {
            StringBuilder message = new StringBuilder(lines).append("\n\n");
            if (needsHigh) {
                message.append("Needs take more than half of your income. ");
            }
            if (wantsHigh) {
                message.append("Wants are above 30% of your income. ");
            }
            message.append("If 50/30/20 is not realistic yet, 60/30/10 is a sensible "
                    + "stepping stone while you reduce costs.");
            items.add(new AdviceItem(Severity.WARNING,
                    "50/30/20 check: some areas are over", message.toString()));
        } else {
            items.add(new AdviceItem(Severity.GOOD, "50/30/20 check: balanced", lines));
        }
    }

    private static void addEmiAdvice(List<AdviceItem> items, AdviceInput in) {
        if (in.monthlyEmiTotal <= 0) {
            return;
        }
        double share = percentOf(in.monthlyEmiTotal, in.monthlyIncome);
        String base = "Your loan EMIs total " + money(in.monthlyEmiTotal)
                + " a month, which is " + pct(share) + " of your income.";

        if (share > EMI_LIMIT_PERCENT) {
            items.add(new AdviceItem(Severity.ALERT, "EMIs are above the 40% limit",
                    base + " Keep total EMIs at or below 40%. Avoid new loans and consider "
                            + "prepaying the most expensive one."));
        } else if (share >= EMI_CAUTION_PERCENT) {
            items.add(new AdviceItem(Severity.WARNING, "EMIs are close to the 40% limit",
                    base + " Keep some room under 40% before taking on new credit."));
        } else {
            items.add(new AdviceItem(Severity.GOOD, "EMIs are manageable",
                    base + " The usual limit is 40%."));
        }
    }

    private static void addCardAdvice(List<AdviceItem> items, AdviceInput in) {
        if (in.cardLimit <= 0) {
            return;
        }
        double usage = FinanceCalculator.utilizationPercent(in.cardOutstanding, in.cardLimit);
        String base = "You are using " + pct(usage) + " of your total card limit.";

        if (usage > FinanceCalculator.UTILIZATION_WARNING_PERCENT) {
            StringBuilder message = new StringBuilder(base);
            if (!in.highUtilizationCards.isEmpty()) {
                message.append(" Cards above 30%: ")
                        .append(String.join(", ", in.highUtilizationCards)).append(".");
            }
            message.append(" Paying balances down before the statement date lowers it.");
            items.add(new AdviceItem(Severity.WARNING,
                    "Card utilization is above 30%", message.toString()));
        } else {
            items.add(new AdviceItem(Severity.GOOD, "Card utilization is healthy",
                    base + " The guideline is to stay under 30%."));
        }
    }

    private static void addEmergencyFundAdvice(List<AdviceItem> items, AdviceInput in) {
        if (in.monthlyNeedsAverage <= 0) {
            return;
        }
        double months = MoneyUtils.round2(in.totalSaved / in.monthlyNeedsAverage);
        double minTarget = MoneyUtils.round2(in.monthlyNeedsAverage * EMERGENCY_MIN_MONTHS);
        double maxTarget = MoneyUtils.round2(in.monthlyNeedsAverage * EMERGENCY_MAX_MONTHS);
        String base = "Money logged under Savings & Investments covers about "
                + String.format(Locale.getDefault(), "%.1f", months)
                + " months of essential spending (" + money(in.monthlyNeedsAverage)
                + " a month). This is an estimate based only on what you have logged.";

        if (months < EMERGENCY_MIN_MONTHS) {
            items.add(new AdviceItem(Severity.WARNING, "Emergency fund is below 3 months",
                    base + " Aim for 3 to 6 months: " + money(minTarget) + " to "
                            + money(maxTarget) + ". Build this before taking investment risk."));
        } else if (months <= EMERGENCY_MAX_MONTHS) {
            items.add(new AdviceItem(Severity.GOOD, "Emergency fund is in the 3-6 month range",
                    base));
        } else {
            items.add(new AdviceItem(Severity.INFO, "Emergency fund is above 6 months",
                    base + " Savings beyond 6 months of essentials could be invested for growth."));
        }
    }

    private static void addBudgetAdvice(List<AdviceItem> items, AdviceInput in) {
        for (BudgetUsage u : in.budgets) {
            BudgetCalculator.Status status =
                    BudgetCalculator.status(u.getSpent(), u.getMonthlyLimit());
            if (status == BudgetCalculator.Status.NORMAL) {
                continue;
            }
            Double usage = BudgetCalculator.usagePercent(u.getSpent(), u.getMonthlyLimit());
            String summary = "Spent " + money(u.getSpent()) + " of " + money(u.getMonthlyLimit())
                    + " (" + (usage == null ? "N/A" : pct(usage)) + ").";
            String name = u.getCategoryName();

            if (u.getSpent() > u.getMonthlyLimit()) {
                double over = MoneyUtils.round2(u.getSpent() - u.getMonthlyLimit());
                items.add(new AdviceItem(Severity.ALERT, "Over budget: " + name,
                        summary + " You are " + money(over) + " over the limit this month."));
            } else if (status == BudgetCalculator.Status.OVER) {
                items.add(new AdviceItem(Severity.WARNING, "Budget limit reached: " + name,
                        summary + " Nothing is left for the rest of the month."));
            } else {
                double left = BudgetCalculator.remaining(u.getSpent(), u.getMonthlyLimit());
                items.add(new AdviceItem(Severity.WARNING, "Close to budget: " + name,
                        summary + " " + money(left) + " left."));
            }
        }
    }

    private static double percentOf(double part, double whole) {
        return MoneyUtils.round2(part / whole * 100.0);
    }

    private static String money(double value) {
        return MoneyUtils.format(value);
    }

    private static String pct(double value) {
        return String.format(Locale.getDefault(), "%.1f%%", value);
    }
}