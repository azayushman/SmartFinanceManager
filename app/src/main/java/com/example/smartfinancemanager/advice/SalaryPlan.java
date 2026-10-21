package com.example.smartfinancemanager.advice;

/** The result of splitting a salary. All amounts are in rupees. */
public class SalaryPlan {

    private final double salary;
    private final int needsPercent;
    private final int wantsPercent;
    private final int savingsPercent;
    private final double needsAmount;
    private final double wantsAmount;
    private final double savingsAmount;
    private final int equityPercent;
    private final int debtPercent;
    private final double equityAmount;
    private final double debtAmount;
    private final double emergencyFundMin;
    private final double emergencyFundMax;

    public SalaryPlan(double salary, int needsPercent, int wantsPercent, int savingsPercent,
                      double needsAmount, double wantsAmount, double savingsAmount,
                      int equityPercent, int debtPercent, double equityAmount, double debtAmount,
                      double emergencyFundMin, double emergencyFundMax) {
        this.salary = salary;
        this.needsPercent = needsPercent;
        this.wantsPercent = wantsPercent;
        this.savingsPercent = savingsPercent;
        this.needsAmount = needsAmount;
        this.wantsAmount = wantsAmount;
        this.savingsAmount = savingsAmount;
        this.equityPercent = equityPercent;
        this.debtPercent = debtPercent;
        this.equityAmount = equityAmount;
        this.debtAmount = debtAmount;
        this.emergencyFundMin = emergencyFundMin;
        this.emergencyFundMax = emergencyFundMax;
    }

    public double getSalary() { return salary; }
    public int getNeedsPercent() { return needsPercent; }
    public int getWantsPercent() { return wantsPercent; }
    public int getSavingsPercent() { return savingsPercent; }
    public double getNeedsAmount() { return needsAmount; }
    public double getWantsAmount() { return wantsAmount; }
    public double getSavingsAmount() { return savingsAmount; }
    public int getEquityPercent() { return equityPercent; }
    public int getDebtPercent() { return debtPercent; }
    public double getEquityAmount() { return equityAmount; }
    public double getDebtAmount() { return debtAmount; }
    public double getEmergencyFundMin() { return emergencyFundMin; }
    public double getEmergencyFundMax() { return emergencyFundMax; }
}