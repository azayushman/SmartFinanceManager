package com.example.smartfinancemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.smartfinancemanager.advice.AdviceDataLoader;
import com.example.smartfinancemanager.advice.AdviceEngine;
import com.example.smartfinancemanager.advice.AdviceInput;
import com.example.smartfinancemanager.advice.AdviceItem;
import com.example.smartfinancemanager.data.BudgetDao;
import com.example.smartfinancemanager.data.CategoryDao;
import com.example.smartfinancemanager.data.CreditCardDao;
import com.example.smartfinancemanager.data.DatabaseHelper;
import com.example.smartfinancemanager.data.LoanDao;
import com.example.smartfinancemanager.data.TransactionDao;
import com.example.smartfinancemanager.data.model.BudgetUsage;
import com.example.smartfinancemanager.data.model.CardStatement;
import com.example.smartfinancemanager.data.model.Category;
import com.example.smartfinancemanager.data.model.CreditCard;
import com.example.smartfinancemanager.data.model.EmiInstallment;
import com.example.smartfinancemanager.data.model.Loan;
import com.example.smartfinancemanager.data.model.MonthSummary;
import com.example.smartfinancemanager.data.model.Transaction;
import com.example.smartfinancemanager.data.model.TransactionFilter;
import com.example.smartfinancemanager.utils.DateUtils;
import com.example.smartfinancemanager.utils.EmiCalculator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.YearMonth;
import java.util.List;

/** Runs against a separate test database, so the real app data is never touched. */
@RunWith(AndroidJUnit4.class)
public class FinanceDaoTest {

    private static final String TEST_DB = "test_finance.db";
    private static final String INCOME = Transaction.TYPE_INCOME;
    private static final String EXPENSE = Transaction.TYPE_EXPENSE;
    private static final long DAY = 24L * 60 * 60 * 1000;

    private Context context;
    private CategoryDao categories;
    private TransactionDao transactions;
    private LoanDao loans;
    private CreditCardDao cards;
    private BudgetDao budgets;

    private long today;
    private YearMonth thisMonth;
    private long monthStart;
    private long monthEnd;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DatabaseHelper.useTestDatabase(context, TEST_DB);
        categories = new CategoryDao(context);
        transactions = new TransactionDao(context);
        loans = new LoanDao(context);
        cards = new CreditCardDao(context);
        budgets = new BudgetDao(context);

        today = DateUtils.startOfDay(System.currentTimeMillis());
        thisMonth = YearMonth.now();
        monthStart = DateUtils.startOfMonth(thisMonth);
        monthEnd = DateUtils.startOfNextMonth(thisMonth);
    }

    @After
    public void tearDown() {
        DatabaseHelper.useDefaultDatabase();
        context.deleteDatabase(TEST_DB);
    }

    private Category category(String name, String type) {
        for (Category c : categories.getByType(type)) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        throw new AssertionError("Missing category: " + name);
    }

    private long add(String type, String categoryName, double amount, String description, long date) {
        Category c = category(categoryName, type);
        return transactions.insert(new Transaction(0, amount, type, c.getId(), c.getName(),
                description, date, "Cash", null, 0));
    }

    @Test
    public void seedsFifteenCategories() {
        assertEquals(15, categories.getAll().size());
        assertEquals(4, categories.getByType(INCOME).size());
        assertEquals(11, categories.getByType(EXPENSE).size());
    }

    @Test
    public void foreignKeysAreEnforced() {
        Transaction bad = new Transaction(0, 100, EXPENSE, 9999, "Missing",
                "Bad category", today, "Cash", null, 0);
        assertEquals(-1, transactions.insert(bad));
    }

    @Test
    public void monthlyTotalsMatchTheWorkedExample() {
        add(INCOME, "Salary", 30000, "Salary", today);
        add(EXPENSE, "Rent", 8000, "Rent", today);
        long foodId = add(EXPENSE, "Food", 4000, "Groceries", today);
        add(EXPENSE, "Transport", 1000, "Bus pass", today);
        // An expense from last month must not leak into this month
        add(EXPENSE, "Food", 500, "Old bill", DateUtils.startOfMonth(thisMonth.minusMonths(1)));

        double income = transactions.sumByType(INCOME, monthStart, monthEnd);
        double expense = transactions.sumByType(EXPENSE, monthStart, monthEnd);
        MonthSummary summary = new MonthSummary(income, expense);
        assertEquals(30000.0, summary.getIncome(), 0.0);
        assertEquals(13000.0, summary.getExpense(), 0.0);
        assertEquals(17000.0, summary.getNetCashFlow(), 0.0);
        assertEquals(56.67, summary.getSavingsRatePercent().doubleValue(), 0.0);

        // Editing the food expense updates the totals
        Transaction food = transactions.getById(foodId);
        Transaction edited = new Transaction(food.getId(), 3000, food.getType(),
                food.getCategoryId(), food.getCategoryName(), food.getDescription(),
                food.getDate(), food.getPaymentMethod(), food.getNote(), food.getCreatedAt());
        assertTrue(transactions.update(edited));
        assertEquals(12000.0, transactions.sumByType(EXPENSE, monthStart, monthEnd), 0.0);

        // Deleting it removes it from the totals
        assertTrue(transactions.delete(foodId));
        assertNull(transactions.getById(foodId));
        assertEquals(9000.0, transactions.sumByType(EXPENSE, monthStart, monthEnd), 0.0);
    }

    @Test
    public void searchFiltersAndSorts() {
        add(INCOME, "Salary", 30000, "Salary", today);
        add(EXPENSE, "Rent", 8000, "Monthly rent", today);
        add(EXPENSE, "Food", 4000, "Groceries", today);

        TransactionFilter filter = new TransactionFilter();
        filter.searchText = "ren";
        List<Transaction> found = transactions.search(filter);
        assertEquals(1, found.size());
        assertEquals("Monthly rent", found.get(0).getDescription());

        filter.searchText = "";
        filter.type = EXPENSE;
        assertEquals(2, transactions.search(filter).size());

        filter.sort = TransactionFilter.Sort.AMOUNT_DESC;
        assertEquals(8000.0, transactions.search(filter).get(0).getAmount(), 0.0);

        // % is matched literally, not as a wildcard
        filter.type = null;
        filter.searchText = "%";
        assertEquals(0, transactions.search(filter).size());
    }

    @Test
    public void bucketSumsSeparateNeedsWantsAndSavings() {
        add(EXPENSE, "Food", 1000, "Food", today);                       // NEED
        add(EXPENSE, "Shopping", 500, "Shirt", today);                   // WANT
        add(EXPENSE, "Savings & Investments", 700, "SIP", today);        // SAVING

        assertEquals(1000.0, transactions.sumByBucket("NEED", monthStart, monthEnd), 0.0);
        assertEquals(500.0, transactions.sumByBucket("WANT", monthStart, monthEnd), 0.0);
        assertEquals(700.0, transactions.sumByBucket("SAVING", monthStart, monthEnd), 0.0);
        assertEquals(1500.0, transactions.sumSpending(monthStart, monthEnd), 0.0);
    }

    @Test
    public void dataSurvivesClosingTheDatabase() {
        long id = add(INCOME, "Salary", 1234.56, "Salary", today);
        DatabaseHelper.getInstance(context).close();
        Transaction loaded = transactions.getById(id);
        assertEquals(1234.56, loaded.getAmount(), 0.0);
    }

    @Test
    public void loanScheduleAndPayments() {
        double emi = EmiCalculator.monthlyEmi(12000, 12, 12);
        List<EmiInstallment> schedule = EmiCalculator.buildSchedule(12000, 12, 12, today);
        Loan loan = new Loan(0, "Test loan", null, 12000, 12, 12, emi, today,
                Loan.STATUS_ACTIVE, 0, 12000, 0);

        long id = loans.insertWithSchedule(loan, schedule, 3);
        assertTrue(id > 0);

        Loan saved = loans.getById(id);
        assertEquals(3, saved.getPaidInstallments());
        assertEquals(schedule.get(2).getBalanceAmount(), saved.getOutstandingAmount(), 0.001);
        assertEquals(schedule.get(3).getDueDate(), saved.getNextDueDate());
        assertEquals(12, loans.getSchedule(id).size());
        assertEquals(emi, loans.totalActiveMonthlyEmi(), 0.0);

        assertTrue(loans.markNextInstallmentPaid(id, today));
        assertEquals(4, loans.getById(id).getPaidInstallments());
        assertTrue(loans.undoLastPayment(id));
        assertEquals(3, loans.getById(id).getPaidInstallments());

        for (int i = 0; i < 9; i++) {
            assertTrue(loans.markNextInstallmentPaid(id, today));
        }
        assertFalse(loans.markNextInstallmentPaid(id, today));
        assertTrue(loans.getById(id).isClosed());
        assertEquals(0.0, loans.getById(id).getOutstandingAmount(), 0.001);
        assertEquals(0.0, loans.totalActiveMonthlyEmi(), 0.0);

        assertTrue(loans.undoLastPayment(id));
        assertFalse(loans.getById(id).isClosed());

        assertTrue(loans.delete(id));
        assertNull(loans.getById(id));
        assertEquals(0, loans.getSchedule(id).size());
    }

    @Test
    public void cardStatementsAndBalanceReduction() {
        long cardId = cards.insert(new CreditCard(0, "Test card", "Test bank",
                100000, 25000, 15, 5, 0, 0, 0));
        assertTrue(cardId > 0);
        long statementId = cards.insertStatement(new CardStatement(0, cardId,
                today, today + 20 * DAY, 25000, 1250, false));
        assertTrue(statementId > 0);

        CreditCard loaded = cards.getById(cardId);
        assertEquals(1, loaded.getUnpaidStatements());
        assertEquals(25000.0, loaded.getNextDueAmount(), 0.0);

        assertTrue(cards.setStatementPaid(statementId, true, true));
        loaded = cards.getById(cardId);
        assertEquals(0, loaded.getUnpaidStatements());
        assertEquals(0.0, loaded.getOutstandingAmount(), 0.0);

        assertFalse(cards.setStatementPaid(statementId, true, true)); // already paid

        // Marking it unpaid again leaves the balance alone
        assertTrue(cards.setStatementPaid(statementId, false, false));
        assertEquals(0.0, cards.getById(cardId).getOutstandingAmount(), 0.0);
        assertEquals(1, cards.getById(cardId).getUnpaidStatements());

        assertEquals(100000.0, cards.totalCreditLimit(), 0.0);
        assertTrue(cards.delete(cardId));
        assertEquals(0, cards.getStatements(cardId).size());
    }

    @Test
    public void payingAStatementNeverMakesTheBalanceNegative() {
        long small = cards.insert(new CreditCard(0, "Small", null, 50000, 1000, 15, 5, 0, 0, 0));
        long s1 = cards.insertStatement(new CardStatement(0, small,
                today, today + 20 * DAY, 2000, 100, false));
        assertTrue(cards.setStatementPaid(s1, true, true));
        assertEquals(0.0, cards.getById(small).getOutstandingAmount(), 0.0);

        long keep = cards.insert(new CreditCard(0, "Keep", null, 50000, 5000, 15, 5, 0, 0, 0));
        long s2 = cards.insertStatement(new CardStatement(0, keep,
                today, today + 20 * DAY, 2000, 100, false));
        assertTrue(cards.setStatementPaid(s2, true, false));
        assertEquals(5000.0, cards.getById(keep).getOutstandingAmount(), 0.0);
    }

    @Test
    public void budgetUsageReplaceAndCopy() {
        Category food = category("Food", EXPENSE);
        assertTrue(budgets.setBudget(food.getId(), thisMonth, 5000));
        add(EXPENSE, "Food", 4200, "Groceries", today);

        List<BudgetUsage> usage = budgets.getUsage(thisMonth);
        assertEquals(1, usage.size());
        assertEquals(4200.0, usage.get(0).getSpent(), 0.0);
        assertEquals(5000.0, usage.get(0).getMonthlyLimit(), 0.0);

        // Setting it again replaces the budget instead of duplicating it
        assertTrue(budgets.setBudget(food.getId(), thisMonth, 6000));
        usage = budgets.getUsage(thisMonth);
        assertEquals(1, usage.size());
        assertEquals(6000.0, usage.get(0).getMonthlyLimit(), 0.0);

        YearMonth next = thisMonth.plusMonths(1);
        assertEquals(1, budgets.copyFromPreviousMonth(next));
        assertEquals(0, budgets.copyFromPreviousMonth(next)); // nothing new to copy
        assertEquals(0.0, budgets.getUsage(next).get(0).getSpent(), 0.0);

        assertTrue(budgets.deleteBudget(food.getId(), thisMonth));
        assertEquals(0, budgets.getUsage(thisMonth).size());
    }

    @Test
    public void adviceReflectsStoredData() {
        add(INCOME, "Salary", 30000, "Salary", today);
        add(EXPENSE, "Rent", 8000, "Rent", today);
        add(EXPENSE, "Food", 4000, "Food", today);
        add(EXPENSE, "Transport", 1000, "Bus", today);

        AdviceInput input = new AdviceDataLoader(context).load(thisMonth);
        assertEquals(30000.0, input.monthlyIncome, 0.0);
        assertEquals(13000.0, input.needsSpent, 0.0);
        assertEquals(0.0, input.wantsSpent, 0.0);

        boolean onTrack = false;
        for (AdviceItem item : AdviceEngine.generate(input)) {
            if (item.getTitle().equals("Savings on track")) {
                onTrack = true;
            }
        }
        assertTrue(onTrack);
    }
}