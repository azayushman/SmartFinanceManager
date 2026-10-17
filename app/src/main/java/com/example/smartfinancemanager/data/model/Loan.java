package com.example.smartfinancemanager.data.model;

public class Loan {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CLOSED = "CLOSED";

    private final long id;                 // 0 for a loan not yet saved
    private final String name;
    private final String lender;           // may be null
    private final double principal;
    private final double annualRatePercent;
    private final int tenureMonths;
    private final double emiAmount;
    private final long startDate;          // start of day, epoch milliseconds
    private final String status;

    // Worked out from the schedule when the loan is read from the database
    private final int paidInstallments;
    private final double outstandingAmount;
    private final long nextDueDate;        // 0 when fully repaid

    public Loan(long id, String name, String lender, double principal,
                double annualRatePercent, int tenureMonths, double emiAmount,
                long startDate, String status, int paidInstallments,
                double outstandingAmount, long nextDueDate) {
        this.id = id;
        this.name = name;
        this.lender = lender;
        this.principal = principal;
        this.annualRatePercent = annualRatePercent;
        this.tenureMonths = tenureMonths;
        this.emiAmount = emiAmount;
        this.startDate = startDate;
        this.status = status;
        this.paidInstallments = paidInstallments;
        this.outstandingAmount = outstandingAmount;
        this.nextDueDate = nextDueDate;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getLender() { return lender; }
    public double getPrincipal() { return principal; }
    public double getAnnualRatePercent() { return annualRatePercent; }
    public int getTenureMonths() { return tenureMonths; }
    public double getEmiAmount() { return emiAmount; }
    public long getStartDate() { return startDate; }
    public String getStatus() { return status; }
    public int getPaidInstallments() { return paidInstallments; }
    public double getOutstandingAmount() { return outstandingAmount; }
    public long getNextDueDate() { return nextDueDate; }

    public boolean isClosed() { return STATUS_CLOSED.equals(status); }
}