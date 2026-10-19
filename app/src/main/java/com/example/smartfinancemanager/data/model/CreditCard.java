package com.example.smartfinancemanager.data.model;

public class CreditCard {

    private final long id;                 // 0 for a card not yet saved
    private final String name;
    private final String bank;             // may be null
    private final double creditLimit;
    private final double outstandingAmount;
    private final int statementDay;        // 1-28
    private final int dueDay;              // 1-28

    // Worked out from the statements when the card is read from the database
    private final int unpaidStatements;
    private final long nextDueDate;        // 0 when nothing is due
    private final double nextDueAmount;

    public CreditCard(long id, String name, String bank, double creditLimit,
                      double outstandingAmount, int statementDay, int dueDay,
                      int unpaidStatements, long nextDueDate, double nextDueAmount) {
        this.id = id;
        this.name = name;
        this.bank = bank;
        this.creditLimit = creditLimit;
        this.outstandingAmount = outstandingAmount;
        this.statementDay = statementDay;
        this.dueDay = dueDay;
        this.unpaidStatements = unpaidStatements;
        this.nextDueDate = nextDueDate;
        this.nextDueAmount = nextDueAmount;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getBank() { return bank; }
    public double getCreditLimit() { return creditLimit; }
    public double getOutstandingAmount() { return outstandingAmount; }
    public int getStatementDay() { return statementDay; }
    public int getDueDay() { return dueDay; }
    public int getUnpaidStatements() { return unpaidStatements; }
    public long getNextDueDate() { return nextDueDate; }
    public double getNextDueAmount() { return nextDueAmount; }
}