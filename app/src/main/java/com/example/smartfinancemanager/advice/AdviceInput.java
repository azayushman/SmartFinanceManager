package com.example.smartfinancemanager.advice;

import com.example.smartfinancemanager.data.model.BudgetUsage;

import java.util.ArrayList;
import java.util.List;

/** Everything the advice rules need, gathered from the database for one month. */
public class AdviceInput {

    public double monthlyIncome;
    public double needsSpent;           // spending in NEED categories this month
    public double wantsSpent;           // spending in WANT categories this month
    public double monthlyNeedsAverage;  // average monthly NEED spending over the last 3 months
    public double totalSaved;           // everything logged under Savings & Investments
    public double monthlyEmiTotal;      // sum of EMIs of active loans
    public double cardOutstanding;
    public double cardLimit;
    public List<String> highUtilizationCards = new ArrayList<>();
    public List<BudgetUsage> budgets = new ArrayList<>();
}