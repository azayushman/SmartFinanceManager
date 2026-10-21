package com.example.smartfinancemanager.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.smartfinancemanager.data.model.BudgetUsage;
import com.example.smartfinancemanager.utils.DateUtils;
import com.example.smartfinancemanager.utils.MoneyUtils;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class BudgetDao {

    private final DatabaseHelper helper;

    public BudgetDao(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    /** Creates the budget, or replaces the existing one for this category and month. */
    public boolean setBudget(long categoryId, YearMonth month, double limit) {
        ContentValues v = new ContentValues();
        v.put("category_id", categoryId);
        v.put("monthly_limit", limit);
        v.put("month_key", DateUtils.monthKey(month));
        long id = helper.getWritableDatabase().insertWithOnConflict(
                DatabaseHelper.T_BUDGETS, null, v, SQLiteDatabase.CONFLICT_REPLACE);
        return id != -1;
    }

    public boolean deleteBudget(long categoryId, YearMonth month) {
        int rows = helper.getWritableDatabase().delete(DatabaseHelper.T_BUDGETS,
                "category_id = ? AND month_key = ?",
                new String[]{String.valueOf(categoryId), DateUtils.monthKey(month)});
        return rows == 1;
    }

    /** All budgets of the month with the amount spent so far, ordered by category name. */
    public List<BudgetUsage> getUsage(YearMonth month) {
        long from = DateUtils.startOfMonth(month);
        long to = DateUtils.startOfNextMonth(month);
        String sql = "SELECT b.category_id, c.name, b.monthly_limit, "
                + "COALESCE((SELECT SUM(t.amount) FROM " + DatabaseHelper.T_TRANSACTIONS
                + " t WHERE t.category_id = b.category_id AND t.type = 'EXPENSE' "
                + "AND t.date >= ? AND t.date < ?), 0) "
                + "FROM " + DatabaseHelper.T_BUDGETS + " b JOIN " + DatabaseHelper.T_CATEGORIES
                + " c ON c.id = b.category_id WHERE b.month_key = ? ORDER BY c.name ASC";

        List<BudgetUsage> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(sql, new String[]{
                String.valueOf(from), String.valueOf(to), DateUtils.monthKey(month)})) {
            while (c.moveToNext()) {
                list.add(new BudgetUsage(c.getLong(0), c.getString(1),
                        c.getDouble(2), MoneyUtils.round2(c.getDouble(3))));
            }
        }
        return list;
    }

    /**
     * Copies the previous month's budgets into this month, skipping categories that already
     * have one. Returns how many were copied, or -1 if something failed.
     */
    public int copyFromPreviousMonth(YearMonth month) {
        String fromKey = DateUtils.monthKey(month.minusMonths(1));
        String toKey = DateUtils.monthKey(month);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            List<ContentValues> rows = new ArrayList<>();
            String sql = "SELECT category_id, monthly_limit FROM " + DatabaseHelper.T_BUDGETS
                    + " WHERE month_key = ?";
            try (Cursor c = db.rawQuery(sql, new String[]{fromKey})) {
                while (c.moveToNext()) {
                    ContentValues v = new ContentValues();
                    v.put("category_id", c.getLong(0));
                    v.put("monthly_limit", c.getDouble(1));
                    v.put("month_key", toKey);
                    rows.add(v);
                }
            }
            int copied = 0;
            for (ContentValues v : rows) {
                long id = db.insertWithOnConflict(DatabaseHelper.T_BUDGETS, null, v,
                        SQLiteDatabase.CONFLICT_IGNORE);
                if (id != -1) {
                    copied++;
                }
            }
            db.setTransactionSuccessful();
            return copied;
        } finally {
            db.endTransaction();
        }
    }
}