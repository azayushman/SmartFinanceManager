package com.example.smartfinancemanager.advice;

import android.content.Context;

import com.example.smartfinancemanager.data.BudgetDao;
import com.example.smartfinancemanager.data.CreditCardDao;
import com.example.smartfinancemanager.data.LoanDao;
import com.example.smartfinancemanager.data.TransactionDao;
import com.example.smartfinancemanager.data.model.CreditCard;
import com.example.smartfinancemanager.data.model.Transaction;
import com.example.smartfinancemanager.utils.DateUtils;
import com.example.smartfinancemanager.utils.FinanceCalculator;
import com.example.smartfinancemanager.utils.MoneyUtils;

import java.time.YearMonth;

public class AdviceDataLoader {

    private static final String NEED = "NEED";
    private static final String WANT = "WANT";
    private static final String SAVING = "SAVING";

    private final TransactionDao transactionDao;
    private final LoanDao loanDao;
    private final CreditCardDao cardDao;
    private final BudgetDao budgetDao;

    public AdviceDataLoader(Context context) {
        transactionDao = new TransactionDao(context);
        loanDao = new LoanDao(context);
        cardDao = new CreditCardDao(context);
        budgetDao = new BudgetDao(context);
    }

    public AdviceInput load(YearMonth month) {
        long from = DateUtils.startOfMonth(month);
        long to = DateUtils.startOfNextMonth(month);

        AdviceInput in = new AdviceInput();
        in.monthlyIncome = transactionDao.sumByType(Transaction.TYPE_INCOME, from, to);
        in.needsSpent = transactionDao.sumByBucket(NEED, from, to);
        in.wantsSpent = transactionDao.sumByBucket(WANT, from, to);

        // Average essential spending over the last 3 months that actually have any
        double needsTotal = 0;
        int monthsWithNeeds = 0;
        for (int i = 0; i < 3; i++) {
            YearMonth m = month.minusMonths(i);
            double needs = transactionDao.sumByBucket(NEED,
                    DateUtils.startOfMonth(m), DateUtils.startOfNextMonth(m));
            if (needs > 0) {
                needsTotal += needs;
                monthsWithNeeds++;
            }
        }
        in.monthlyNeedsAverage = monthsWithNeeds == 0
                ? 0 : MoneyUtils.round2(needsTotal / monthsWithNeeds);

        in.totalSaved = transactionDao.sumByBucket(SAVING, 0, Long.MAX_VALUE);
        in.monthlyEmiTotal = loanDao.totalActiveMonthlyEmi();
        in.cardOutstanding = cardDao.totalOutstanding();
        in.cardLimit = cardDao.totalCreditLimit();

        for (CreditCard card : cardDao.getAll()) {
            Double usage = FinanceCalculator.utilizationPercent(
                    card.getOutstandingAmount(), card.getCreditLimit());
            if (usage != null && usage > FinanceCalculator.UTILIZATION_WARNING_PERCENT) {
                in.highUtilizationCards.add(card.getName());
            }
        }

        in.budgets = budgetDao.getUsage(month);
        return in;
    }
}