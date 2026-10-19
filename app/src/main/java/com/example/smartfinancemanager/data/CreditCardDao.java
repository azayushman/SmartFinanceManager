package com.example.smartfinancemanager.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.smartfinancemanager.data.model.CardStatement;
import com.example.smartfinancemanager.data.model.CreditCard;
import com.example.smartfinancemanager.utils.MoneyUtils;

import java.util.ArrayList;
import java.util.List;

public class CreditCardDao {

    private static final String SELECT_CARD =
            "SELECT c.id, c.name, c.bank, c.credit_limit, c.outstanding_amount, "
                    + "c.statement_day, c.due_day, "
                    + "(SELECT COUNT(*) FROM " + DatabaseHelper.T_STATEMENTS
                    + " s WHERE s.card_id = c.id AND s.is_paid = 0), "
                    + "(SELECT COALESCE(MIN(s.due_date), 0) FROM " + DatabaseHelper.T_STATEMENTS
                    + " s WHERE s.card_id = c.id AND s.is_paid = 0), "
                    + "COALESCE((SELECT s.total_due FROM " + DatabaseHelper.T_STATEMENTS
                    + " s WHERE s.card_id = c.id AND s.is_paid = 0 "
                    + "ORDER BY s.due_date ASC, s.id ASC LIMIT 1), 0) "
                    + "FROM " + DatabaseHelper.T_CARDS + " c ";

    private final DatabaseHelper helper;

    public CreditCardDao(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    /** Returns the new card id, or -1 if the insert failed. */
    public long insert(CreditCard card) {
        return helper.getWritableDatabase()
                .insert(DatabaseHelper.T_CARDS, null, toValues(card));
    }

    /** Updates the card's own fields. Returns true if exactly one row changed. */
    public boolean update(CreditCard card) {
        int rows = helper.getWritableDatabase().update(DatabaseHelper.T_CARDS,
                toValues(card), "id = ?", new String[]{String.valueOf(card.getId())});
        return rows == 1;
    }

    /** Deletes the card; its statements are removed automatically (ON DELETE CASCADE). */
    public boolean delete(long cardId) {
        int rows = helper.getWritableDatabase().delete(DatabaseHelper.T_CARDS,
                "id = ?", new String[]{String.valueOf(cardId)});
        return rows == 1;
    }

    /** Newest card first. */
    public List<CreditCard> getAll() {
        return queryCards("", null);
    }

    /** Returns null if there is no card with this id. */
    public CreditCard getById(long cardId) {
        List<CreditCard> list = queryCards("WHERE c.id = ? ", new String[]{String.valueOf(cardId)});
        return list.isEmpty() ? null : list.get(0);
    }

    /** Returns the new statement id, or -1 if the insert failed. */
    public long insertStatement(CardStatement statement) {
        ContentValues v = new ContentValues();
        v.put("card_id", statement.getCardId());
        v.put("statement_date", statement.getStatementDate());
        v.put("due_date", statement.getDueDate());
        v.put("total_due", statement.getTotalDue());
        v.put("min_due", statement.getMinDue());
        v.put("is_paid", 0);
        return helper.getWritableDatabase().insert(DatabaseHelper.T_STATEMENTS, null, v);
    }

    /** Newest statement first. */
    public List<CardStatement> getStatements(long cardId) {
        List<CardStatement> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT id, card_id, statement_date, due_date, total_due, min_due, is_paid FROM "
                + DatabaseHelper.T_STATEMENTS
                + " WHERE card_id = ? ORDER BY statement_date DESC, id DESC";
        try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(cardId)})) {
            while (c.moveToNext()) {
                list.add(new CardStatement(c.getLong(0), c.getLong(1), c.getLong(2),
                        c.getLong(3), c.getDouble(4), c.getDouble(5), c.getInt(6) == 1));
            }
        }
        return list;
    }

    /**
     * Marks a statement paid or unpaid. When marking paid, reduceOutstanding also lowers the
     * card's outstanding balance by the statement's total (never below zero), in the same
     * transaction. Marking unpaid never changes the balance.
     * Returns false if the statement is missing or already in the requested state.
     */
    public boolean setStatementPaid(long statementId, boolean paid, boolean reduceOutstanding) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long cardId = -1;
            double totalDue = 0;
            boolean currentlyPaid = false;
            double outstanding = 0;

            String sql = "SELECT s.card_id, s.total_due, s.is_paid, c.outstanding_amount FROM "
                    + DatabaseHelper.T_STATEMENTS + " s JOIN " + DatabaseHelper.T_CARDS
                    + " c ON c.id = s.card_id WHERE s.id = ?";
            try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(statementId)})) {
                if (!c.moveToFirst()) {
                    return false;
                }
                cardId = c.getLong(0);
                totalDue = c.getDouble(1);
                currentlyPaid = c.getInt(2) == 1;
                outstanding = c.getDouble(3);
            }
            if (currentlyPaid == paid) {
                return false;
            }

            ContentValues statementValues = new ContentValues();
            statementValues.put("is_paid", paid ? 1 : 0);
            int rows = db.update(DatabaseHelper.T_STATEMENTS, statementValues,
                    "id = ?", new String[]{String.valueOf(statementId)});
            if (rows != 1) {
                return false;
            }

            if (paid && reduceOutstanding) {
                ContentValues cardValues = new ContentValues();
                cardValues.put("outstanding_amount",
                        MoneyUtils.round2(Math.max(0, outstanding - totalDue)));
                int cardRows = db.update(DatabaseHelper.T_CARDS, cardValues,
                        "id = ?", new String[]{String.valueOf(cardId)});
                if (cardRows != 1) {
                    return false;
                }
            }

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public boolean deleteStatement(long statementId) {
        int rows = helper.getWritableDatabase().delete(DatabaseHelper.T_STATEMENTS,
                "id = ?", new String[]{String.valueOf(statementId)});
        return rows == 1;
    }

    /** Total outstanding balance across all cards (used by the advice rules on Day 4). */
    public double totalOutstanding() {
        return sum("outstanding_amount");
    }

    /** Total credit limit across all cards. */
    public double totalCreditLimit() {
        return sum("credit_limit");
    }

    private double sum(String column) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT COALESCE(SUM(" + column + "), 0) FROM " + DatabaseHelper.T_CARDS;
        try (Cursor c = db.rawQuery(sql, null)) {
            double total = c.moveToFirst() ? c.getDouble(0) : 0;
            return MoneyUtils.round2(total);
        }
    }

    private List<CreditCard> queryCards(String where, String[] args) {
        List<CreditCard> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(SELECT_CARD + where + "ORDER BY c.id DESC", args)) {
            while (c.moveToNext()) {
                list.add(new CreditCard(
                        c.getLong(0),
                        c.getString(1),
                        c.isNull(2) ? null : c.getString(2),
                        c.getDouble(3),
                        c.getDouble(4),
                        c.getInt(5),
                        c.getInt(6),
                        c.getInt(7),
                        c.getLong(8),
                        c.getDouble(9)));
            }
        }
        return list;
    }

    private ContentValues toValues(CreditCard card) {
        ContentValues v = new ContentValues();
        v.put("name", card.getName());
        v.put("bank", card.getBank());
        v.put("credit_limit", card.getCreditLimit());
        v.put("outstanding_amount", card.getOutstandingAmount());
        v.put("statement_day", card.getStatementDay());
        v.put("due_day", card.getDueDay());
        return v;
    }
}