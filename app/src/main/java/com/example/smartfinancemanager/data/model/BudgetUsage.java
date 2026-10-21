package com.example.smartfinancemanager.data.model;

/** A category's monthly budget together with what has been spent against it. */
public class BudgetUsage {

    private final long categoryId;
    private final String categoryName;
    private final double monthlyLimit;
    private final double spent;

    public BudgetUsage(long categoryId, String categoryName, double monthlyLimit, double spent) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.monthlyLimit = monthlyLimit;
        this.spent = spent;
    }

    public long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public double getMonthlyLimit() { return monthlyLimit; }
    public double getSpent() { return spent; }
}