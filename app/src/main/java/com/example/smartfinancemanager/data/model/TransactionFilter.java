package com.example.smartfinancemanager.data.model;

/** Describes which transactions to load and in what order. Defaults mean "everything, newest first". */
public class TransactionFilter {

    public enum Sort { DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC }

    public String searchText = "";   // matches the description
    public String type = null;       // Transaction.TYPE_INCOME, TYPE_EXPENSE, or null for both
    public long categoryId = 0;      // 0 = any category
    public long fromDate = 0;        // inclusive start-of-day, 0 = no lower bound
    public long toDate = 0;          // exclusive, 0 = no upper bound
    public Sort sort = Sort.DATE_DESC;
}