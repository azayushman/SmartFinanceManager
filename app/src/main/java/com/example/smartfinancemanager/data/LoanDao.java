package com.example.smartfinancemanager.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.smartfinancemanager.data.model.EmiInstallment;
import com.example.smartfinancemanager.data.model.Loan;
import com.example.smartfinancemanager.utils.MoneyUtils;

import java.util.ArrayList;
import java.util.List;

public class LoanDao {

    private static final String SELECT_LOAN =
            "SELECT l.id, l.name, l.lender, l.principal, l.annual_rate_percent, "
                    + "l.tenure_months, l.emi_amount, l.start_date, l.status, "
                    + "(SELECT COUNT(*) FROM " + DatabaseHelper.T_EMI
                    + " e WHERE e.loan_id = l.id AND e.is_paid = 1), "
                    + "(SELECT COALESCE(SUM(e.principal_amount), 0) FROM " + DatabaseHelper.T_EMI
                    + " e WHERE e.loan_id = l.id AND e.is_paid = 1), "
                    + "(SELECT COALESCE(MIN(e.due_date), 0) FROM " + DatabaseHelper.T_EMI
                    + " e WHERE e.loan_id = l.id AND e.is_paid = 0) "
                    + "FROM " + DatabaseHelper.T_LOANS + " l ";

    private final DatabaseHelper helper;

    public LoanDao(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    /**
     * Saves the loan and its whole schedule in one transaction. The first alreadyPaid
     * instalments are saved as paid (paid on their due date).
     * Returns the new loan id, or -1 if anything failed (nothing is saved in that case).
     */
    public long insertWithSchedule(Loan loan, List<EmiInstallment> schedule, int alreadyPaid) {
        if (schedule.isEmpty() || alreadyPaid < 0 || alreadyPaid > schedule.size()) {
            return -1;
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues loanValues = new ContentValues();
            loanValues.put("name", loan.getName());
            loanValues.put("lender", loan.getLender());
            loanValues.put("principal", loan.getPrincipal());
            loanValues.put("annual_rate_percent", loan.getAnnualRatePercent());
            loanValues.put("tenure_months", loan.getTenureMonths());
            loanValues.put("emi_amount", loan.getEmiAmount());
            loanValues.put("start_date", loan.getStartDate());
            loanValues.put("status", Loan.STATUS_ACTIVE);
            long loanId = db.insert(DatabaseHelper.T_LOANS, null, loanValues);
            if (loanId == -1) {
                return -1;
            }

            for (EmiInstallment item : schedule) {
                boolean paid = item.getInstallmentNo() <= alreadyPaid;
                ContentValues v = new ContentValues();
                v.put("loan_id", loanId);
                v.put("installment_no", item.getInstallmentNo());
                v.put("due_date", item.getDueDate());
                v.put("emi_amount", item.getEmiAmount());
                v.put("principal_amount", item.getPrincipalAmount());
                v.put("interest_amount", item.getInterestAmount());
                v.put("balance_amount", item.getBalanceAmount());
                v.put("is_paid", paid ? 1 : 0);
                if (paid) {
                    v.put("paid_on", item.getDueDate());
                }
                if (db.insert(DatabaseHelper.T_EMI, null, v) == -1) {
                    return -1;
                }
            }

            refreshStatus(db, loanId);
            db.setTransactionSuccessful();
            return loanId;
        } finally {
            db.endTransaction();
        }
    }

    /** Newest loan first. */
    public List<Loan> getAll() {
        return query("", null);
    }

    /** Returns null if there is no loan with this id. */
    public Loan getById(long loanId) {
        List<Loan> list = query("WHERE l.id = ? ", new String[]{String.valueOf(loanId)});
        return list.isEmpty() ? null : list.get(0);
    }

    public List<EmiInstallment> getSchedule(long loanId) {
        List<EmiInstallment> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT id, loan_id, installment_no, due_date, emi_amount, principal_amount, "
                + "interest_amount, balance_amount, is_paid, paid_on FROM " + DatabaseHelper.T_EMI
                + " WHERE loan_id = ? ORDER BY installment_no ASC";
        try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(loanId)})) {
            while (c.moveToNext()) {
                list.add(new EmiInstallment(
                        c.getLong(0), c.getLong(1), c.getInt(2), c.getLong(3),
                        c.getDouble(4), c.getDouble(5), c.getDouble(6), c.getDouble(7),
                        c.getInt(8) == 1, c.getLong(9)));
            }
        }
        return list;
    }

    /**
     * Marks the earliest unpaid instalment as paid on paidOn.
     * Returns false if every instalment is already paid.
     */
    public boolean markNextInstallmentPaid(long loanId, long paidOn) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long installmentId = findInstallmentId(db, loanId,
                    "is_paid = 0", "installment_no ASC");
            if (installmentId < 0) {
                return false;
            }
            ContentValues v = new ContentValues();
            v.put("is_paid", 1);
            v.put("paid_on", paidOn);
            int rows = db.update(DatabaseHelper.T_EMI, v,
                    "id = ?", new String[]{String.valueOf(installmentId)});
            if (rows != 1) {
                return false;
            }
            refreshStatus(db, loanId);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    /** Un-pays the most recently paid instalment. Returns false if none are paid. */
    public boolean undoLastPayment(long loanId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long installmentId = findInstallmentId(db, loanId,
                    "is_paid = 1", "installment_no DESC");
            if (installmentId < 0) {
                return false;
            }
            ContentValues v = new ContentValues();
            v.put("is_paid", 0);
            v.putNull("paid_on");
            int rows = db.update(DatabaseHelper.T_EMI, v,
                    "id = ?", new String[]{String.valueOf(installmentId)});
            if (rows != 1) {
                return false;
            }
            refreshStatus(db, loanId);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    /** Deletes the loan; its schedule is removed automatically (ON DELETE CASCADE). */
    public boolean delete(long loanId) {
        int rows = helper.getWritableDatabase().delete(DatabaseHelper.T_LOANS,
                "id = ?", new String[]{String.valueOf(loanId)});
        return rows == 1;
    }

    /** Sum of the monthly EMIs of all active loans (used by the advice rules on Day 4). */
    public double totalActiveMonthlyEmi() {
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT COALESCE(SUM(emi_amount), 0) FROM " + DatabaseHelper.T_LOANS
                + " WHERE status = ?";
        try (Cursor c = db.rawQuery(sql, new String[]{Loan.STATUS_ACTIVE})) {
            double sum = c.moveToFirst() ? c.getDouble(0) : 0;
            return MoneyUtils.round2(sum);
        }
    }

    private long findInstallmentId(SQLiteDatabase db, long loanId, String condition, String order) {
        String sql = "SELECT id FROM " + DatabaseHelper.T_EMI + " WHERE loan_id = ? AND "
                + condition + " ORDER BY " + order + " LIMIT 1";
        try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(loanId)})) {
            return c.moveToFirst() ? c.getLong(0) : -1;
        }
    }

    /** Sets the loan to CLOSED when nothing is left to pay, otherwise ACTIVE. */
    private void refreshStatus(SQLiteDatabase db, long loanId) {
        int unpaid = 0;
        String sql = "SELECT COUNT(*) FROM " + DatabaseHelper.T_EMI
                + " WHERE loan_id = ? AND is_paid = 0";
        try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(loanId)})) {
            if (c.moveToFirst()) {
                unpaid = c.getInt(0);
            }
        }
        ContentValues v = new ContentValues();
        v.put("status", unpaid == 0 ? Loan.STATUS_CLOSED : Loan.STATUS_ACTIVE);
        db.update(DatabaseHelper.T_LOANS, v, "id = ?", new String[]{String.valueOf(loanId)});
    }

    private List<Loan> query(String where, String[] args) {
        List<Loan> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(SELECT_LOAN + where + "ORDER BY l.id DESC", args)) {
            while (c.moveToNext()) {
                list.add(new Loan(
                        c.getLong(0),
                        c.getString(1),
                        c.isNull(2) ? null : c.getString(2),
                        c.getDouble(3),
                        c.getDouble(4),
                        c.getInt(5),
                        c.getDouble(6),
                        c.getLong(7),
                        c.getString(8),
                        c.getInt(9),
                        MoneyUtils.round2(c.getDouble(3) - c.getDouble(10)),
                        c.getLong(11)));
            }
        }
        return list;
    }
}