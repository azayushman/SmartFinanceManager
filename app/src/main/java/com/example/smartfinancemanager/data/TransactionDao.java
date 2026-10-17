package com.example.smartfinancemanager.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.smartfinancemanager.data.model.Transaction;
import com.example.smartfinancemanager.data.model.TransactionFilter;
import com.example.smartfinancemanager.utils.MoneyUtils;

import java.util.ArrayList;
import java.util.List;

public class TransactionDao {

    private static final String SELECT_JOINED =
            "SELECT t.id, t.amount, t.type, t.category_id, c.name, t.description, "
                    + "t.date, t.payment_method, t.note, t.created_at "
                    + "FROM " + DatabaseHelper.T_TRANSACTIONS + " t "
                    + "JOIN " + DatabaseHelper.T_CATEGORIES + " c ON c.id = t.category_id ";

    private static final String ORDER_NEWEST = "ORDER BY t.date DESC, t.id DESC ";

    private final DatabaseHelper helper;

    public TransactionDao(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    /** Returns the new row id, or -1 if the insert failed. */
    public long insert(Transaction t) {
        ContentValues values = toValues(t);
        values.put("created_at", System.currentTimeMillis());
        return helper.getWritableDatabase()
                .insert(DatabaseHelper.T_TRANSACTIONS, null, values);
    }

    /** Updates the existing row. Returns true if exactly one row changed. */
    public boolean update(Transaction t) {
        int rows = helper.getWritableDatabase().update(
                DatabaseHelper.T_TRANSACTIONS, toValues(t),
                "id = ?", new String[]{String.valueOf(t.getId())});
        return rows == 1;
    }

    public boolean delete(long id) {
        int rows = helper.getWritableDatabase().delete(
                DatabaseHelper.T_TRANSACTIONS,
                "id = ?", new String[]{String.valueOf(id)});
        return rows == 1;
    }

    /** Returns null if no transaction has this id. */
    public Transaction getById(long id) {
        List<Transaction> list = query("WHERE t.id = ? ",
                new String[]{String.valueOf(id)}, ORDER_NEWEST, "");
        return list.isEmpty() ? null : list.get(0);
    }

    /** Newest first. */
    public List<Transaction> getAll() {
        return search(new TransactionFilter());
    }

    /** Newest first, at most 'limit' rows. */
    public List<Transaction> getRecent(int limit) {
        return query("", null, ORDER_NEWEST, "LIMIT " + limit);
    }

    /** Search, filter and sort. All filter fields are optional. */
    public List<Transaction> search(TransactionFilter filter) {
        List<String> conditions = new ArrayList<>();
        List<String> args = new ArrayList<>();

        String text = filter.searchText == null ? "" : filter.searchText.trim();
        if (!text.isEmpty()) {
            conditions.add("t.description LIKE ? ESCAPE '\\'");
            args.add("%" + escapeLike(text) + "%");
        }
        if (filter.type != null) {
            conditions.add("t.type = ?");
            args.add(filter.type);
        }
        if (filter.categoryId > 0) {
            conditions.add("t.category_id = ?");
            args.add(String.valueOf(filter.categoryId));
        }
        if (filter.fromDate > 0) {
            conditions.add("t.date >= ?");
            args.add(String.valueOf(filter.fromDate));
        }
        if (filter.toDate > 0) {
            conditions.add("t.date < ?");
            args.add(String.valueOf(filter.toDate));
        }

        String where = conditions.isEmpty()
                ? "" : "WHERE " + String.join(" AND ", conditions) + " ";
        return query(where, args.toArray(new String[0]), orderBy(filter.sort), "");
    }

    /**
     * Total of all trannsactions of one type with fromInclusive <= date < toExclusive.
     * Use DateUtils.startOfMonth / startOfNextMonth for a month, or 0 and Long.MAX_VALUE for all time.
     */
    public double sumByType(String type, long fromInclusive, long toExclusive) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM " + DatabaseHelper.T_TRANSACTIONS
                + " WHERE type = ? AND date >= ? AND date < ?";
        try (Cursor c = db.rawQuery(sql, new String[]{
                type, String.valueOf(fromInclusive), String.valueOf(toExclusive)})) {
            double sum = c.moveToFirst() ? c.getDouble(0) : 0;
            return MoneyUtils.round2(sum);
        }
    }

    private static String orderBy(TransactionFilter.Sort sort) {
        if (sort == null) {
            return ORDER_NEWEST;
        }
        switch (sort) {
            case DATE_ASC:
                return "ORDER BY t.date ASC, t.id ASC ";
            case AMOUNT_DESC:
                return "ORDER BY t.amount DESC, t.id DESC ";
            case AMOUNT_ASC:
                return "ORDER BY t.amount ASC, t.id ASC ";
            case DATE_DESC:
            default:
                return ORDER_NEWEST;
        }
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private List<Transaction> query(String where, String[] args, String orderBy, String limitClause) {
        String sql = SELECT_JOINED + where + orderBy + limitClause;
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(sql, args)) {
            while (c.moveToNext()) {
                list.add(new Transaction(
                        c.getLong(0),
                        c.getDouble(1),
                        c.getString(2),
                        c.getLong(3),
                        c.getString(4),
                        c.getString(5),
                        c.getLong(6),
                        c.isNull(7) ? null : c.getString(7),
                        c.isNull(8) ? null : c.getString(8),
                        c.getLong(9)));
            }
        }
        return list;
    }
    /** Makes %, _ and \ in the user's text match literally inside a LIKE pattern. */
    private ContentValues toValues(Transaction t) {
        ContentValues values = new ContentValues();
        values.put("amount", t.getAmount());
        values.put("type", t.getType());
        values.put("category_id", t.getCategoryId());
        values.put("description", t.getDescription());
        values.put("date", t.getDate());
        values.put("payment_method", t.getPaymentMethod());
        values.put("note", t.getNote());
        return values;
    }
}