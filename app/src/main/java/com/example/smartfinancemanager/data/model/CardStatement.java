package com.example.smartfinancemanager.data.model;

public class CardStatement {

    private final long id;              // 0 for a statement not yet saved
    private final long cardId;
    private final long statementDate;   // start of day, epoch milliseconds
    private final long dueDate;         // start of day, epoch milliseconds
    private final double totalDue;
    private final double minDue;
    private final boolean paid;

    public CardStatement(long id, long cardId, long statementDate, long dueDate,
                         double totalDue, double minDue, boolean paid) {
        this.id = id;
        this.cardId = cardId;
        this.statementDate = statementDate;
        this.dueDate = dueDate;
        this.totalDue = totalDue;
        this.minDue = minDue;
        this.paid = paid;
    }

    public long getId() { return id; }
    public long getCardId() { return cardId; }
    public long getStatementDate() { return statementDate; }
    public long getDueDate() { return dueDate; }
    public double getTotalDue() { return totalDue; }
    public double getMinDue() { return minDue; }
    public boolean isPaid() { return paid; }
}