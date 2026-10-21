package com.example.smartfinancemanager.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DEFAULT_DB_NAME = "smart_finance.db";
    private static String dbName = DEFAULT_DB_NAME;
    private static final int DB_VERSION = 2;

    public static final String T_CATEGORIES = "categories";
    public static final String T_TRANSACTIONS = "transactions";
    public static final String T_BUDGETS = "budgets";
    public static final String T_LOANS = "loans";
    public static final String T_EMI = "emi_schedule";
    public static final String T_CARDS = "credit_cards";
    public static final String T_STATEMENTS = "card_statements";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        /** For tests only: switches to a separate, freshly created database file. */
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }
    public static synchronized void useTestDatabase(Context context, String name) {
        closeAndForget();
        context.getApplicationContext().deleteDatabase(name);
        dbName = name;
    }

    /** For tests only: goes back to the real database. */
    public static synchronized void useDefaultDatabase() {
        closeAndForget();
        dbName = DEFAULT_DB_NAME;
    }

    private static void closeAndForget() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }

    private DatabaseHelper(Context context) {
        super(context, dbName, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_CATEGORIES + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "type TEXT NOT NULL CHECK(type IN ('INCOME','EXPENSE')), "
                + "bucket TEXT CHECK(bucket IN ('NEED','WANT','SAVING')), "
                + "UNIQUE(name, type))");

        db.execSQL("CREATE TABLE " + T_TRANSACTIONS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "amount REAL NOT NULL CHECK(amount > 0), "
                + "type TEXT NOT NULL CHECK(type IN ('INCOME','EXPENSE')), "
                + "category_id INTEGER NOT NULL REFERENCES " + T_CATEGORIES + "(id), "
                + "description TEXT NOT NULL, "
                + "date INTEGER NOT NULL, "
                + "payment_method TEXT, "
                + "note TEXT, "
                + "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_tx_date ON " + T_TRANSACTIONS + "(date)");
        db.execSQL("CREATE INDEX idx_tx_category ON " + T_TRANSACTIONS + "(category_id)");

        db.execSQL("CREATE TABLE " + T_BUDGETS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "category_id INTEGER NOT NULL REFERENCES " + T_CATEGORIES + "(id) ON DELETE CASCADE, "
                + "monthly_limit REAL NOT NULL CHECK(monthly_limit > 0), "
                + "month_key TEXT NOT NULL, "
                + "UNIQUE(category_id, month_key))");

        db.execSQL("CREATE TABLE " + T_LOANS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "lender TEXT, "
                + "principal REAL NOT NULL, "
                + "annual_rate_percent REAL NOT NULL, "
                + "tenure_months INTEGER NOT NULL, "
                + "emi_amount REAL NOT NULL, "
                + "start_date INTEGER NOT NULL, "
                + "status TEXT NOT NULL DEFAULT 'ACTIVE')");

        db.execSQL("CREATE TABLE " + T_EMI + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "loan_id INTEGER NOT NULL REFERENCES " + T_LOANS + "(id) ON DELETE CASCADE, "
                + "installment_no INTEGER NOT NULL, "
                + "due_date INTEGER NOT NULL, "
                + "emi_amount REAL NOT NULL, "
                + "principal_amount REAL NOT NULL, "
                + "interest_amount REAL NOT NULL, "
                + "balance_amount REAL NOT NULL, "
                + "is_paid INTEGER NOT NULL DEFAULT 0, "
                + "paid_on INTEGER, "
                + "UNIQUE(loan_id, installment_no))");

        db.execSQL("CREATE TABLE " + T_CARDS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "bank TEXT, "
                + "credit_limit REAL NOT NULL, "
                + "outstanding_amount REAL NOT NULL DEFAULT 0, "
                + "statement_day INTEGER, "
                + "due_day INTEGER)");

        db.execSQL("CREATE TABLE " + T_STATEMENTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "card_id INTEGER NOT NULL REFERENCES " + T_CARDS + "(id) ON DELETE CASCADE, "
                + "statement_date INTEGER NOT NULL, "
                + "due_date INTEGER NOT NULL, "
                + "total_due REAL NOT NULL, "
                + "min_due REAL NOT NULL, "
                + "is_paid INTEGER NOT NULL DEFAULT 0)");

        seedCategories(db);
    }

    private void seedCategories(SQLiteDatabase db) {
        // Expense categories with their 50/30/20 bucket
        addCategory(db, "Food", "EXPENSE", "NEED");
        addCategory(db, "Transport", "EXPENSE", "NEED");
        addCategory(db, "Rent", "EXPENSE", "NEED");
        addCategory(db, "Education", "EXPENSE", "NEED");
        addCategory(db, "Health", "EXPENSE", "NEED");
        addCategory(db, "Bills & Utilities", "EXPENSE", "NEED");
        addCategory(db, "EMI & Loans", "EXPENSE", "NEED");
        addCategory(db, "Entertainment", "EXPENSE", "WANT");
        addCategory(db, "Shopping", "EXPENSE", "WANT");
        addCategory(db, "Other", "EXPENSE", "WANT");
        addCategory(db, "Savings & Investments", "EXPENSE", "SAVING");
        // Income categories
        addCategory(db, "Salary", "INCOME", null);
        addCategory(db, "Freelance", "INCOME", null);
        addCategory(db, "Investment", "INCOME", null);
        addCategory(db, "Other Income", "INCOME", null);
    }

    private void addCategory(SQLiteDatabase db, String name, String type, String bucket) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("type", type);
        if (bucket != null) cv.put("bucket", bucket);
        db.insert(T_CATEGORIES, null, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Development only: rebuild everything (children first because of foreign keys)
        db.execSQL("DROP TABLE IF EXISTS " + T_STATEMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + T_CARDS);
        db.execSQL("DROP TABLE IF EXISTS " + T_EMI);
        db.execSQL("DROP TABLE IF EXISTS " + T_LOANS);
        db.execSQL("DROP TABLE IF EXISTS " + T_BUDGETS);
        db.execSQL("DROP TABLE IF EXISTS " + T_TRANSACTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + T_CATEGORIES);
        onCreate(db);
    }
}