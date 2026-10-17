package com.example.smartfinancemanager.data.model;

/** One monthly instalment of a loan. */
public class EmiInstallment {

    private final long id;
    private final long loanId;
    private final int installmentNo;      // 1-based
    private final long dueDate;           // start of day, epoch milliseconds
    private final double emiAmount;
    private final double principalAmount; // part of the EMI that reduces the loan
    private final double interestAmount;
    private final double balanceAmount;   // principal still owed after this instalment
    private final boolean paid;
    private final long paidOn;            // 0 when not paid

    public EmiInstallment(long id, long loanId, int installmentNo, long dueDate,
                          double emiAmount, double principalAmount, double interestAmount,
                          double balanceAmount, boolean paid, long paidOn) {
        this.id = id;
        this.loanId = loanId;
        this.installmentNo = installmentNo;
        this.dueDate = dueDate;
        this.emiAmount = emiAmount;
        this.principalAmount = principalAmount;
        this.interestAmount = interestAmount;
        this.balanceAmount = balanceAmount;
        this.paid = paid;
        this.paidOn = paidOn;
    }

    public long getId() { return id; }
    public long getLoanId() { return loanId; }
    public int getInstallmentNo() { return installmentNo; }
    public long getDueDate() { return dueDate; }
    public double getEmiAmount() { return emiAmount; }
    public double getPrincipalAmount() { return principalAmount; }
    public double getInterestAmount() { return interestAmount; }
    public double getBalanceAmount() { return balanceAmount; }
    public boolean isPaid() { return paid; }
    public long getPaidOn() { return paidOn; }
}