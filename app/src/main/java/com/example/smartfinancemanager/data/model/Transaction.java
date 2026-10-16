package com.example.smartfinancemanager.data.model;

public class Transaction {

    public static final String TYPE_INCOME = "INCOME";
    public static final String TYPE_EXPENSE = "EXPENSE";

    private final long id;              // 0 for a transaction not yet saved
    private final double amount;        // rupees, always greater than 0
    private final String type;          // TYPE_INCOME or TYPE_EXPENSE
    private final long categoryId;
    private final String categoryName;  // filled when read from the database
    private final String description;
    private final long date;            // start of the day, epoch milliseconds
    private final String paymentMethod;
    private final String note;          // may be null
    private final long createdAt;

    public Transaction(long id, double amount, String type, long categoryId,
                       String categoryName, String description, long date,
                       String paymentMethod, String note, long createdAt) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.description = description;
        this.date = date;
        this.paymentMethod = paymentMethod;
        this.note = note;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public double getAmount() { return amount; }
    public String getType() { return type; }
    public long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getDescription() { return description; }
    public long getDate() { return date; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getNote() { return note; }
    public long getCreatedAt() { return createdAt; }
}