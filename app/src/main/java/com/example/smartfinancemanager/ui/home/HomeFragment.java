package com.example.smartfinancemanager.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfinancemanager.R;
import com.example.smartfinancemanager.data.TransactionDao;
import com.example.smartfinancemanager.data.model.MonthSummary;
import com.example.smartfinancemanager.data.model.Transaction;
import com.example.smartfinancemanager.ui.transactions.AddTransactionActivity;
import com.example.smartfinancemanager.ui.transactions.TransactionAdapter;
import com.example.smartfinancemanager.utils.DateUtils;
import com.example.smartfinancemanager.utils.MoneyUtils;
import com.google.android.material.button.MaterialButton;

import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TransactionDao dao;
    private TransactionAdapter recentAdapter;
    private YearMonth month = DateUtils.currentMonth();

    private TextView tvMonth, tvNet, tvSavingsRate, tvBalance, tvIncome, tvExpense, tvRecentEmpty;
    private MaterialButton btnPrev, btnNext;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dao = new TransactionDao(requireContext());

        tvMonth = view.findViewById(R.id.tv_month);
        tvNet = view.findViewById(R.id.tv_net);
        tvSavingsRate = view.findViewById(R.id.tv_savings_rate);
        tvBalance = view.findViewById(R.id.tv_balance);
        tvIncome = view.findViewById(R.id.tv_income);
        tvExpense = view.findViewById(R.id.tv_expense);
        tvRecentEmpty = view.findViewById(R.id.tv_recent_empty);
        btnPrev = view.findViewById(R.id.btn_prev_month);
        btnNext = view.findViewById(R.id.btn_next_month);

        RecyclerView recent = view.findViewById(R.id.rv_recent);
        recent.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentAdapter = new TransactionAdapter(new TransactionAdapter.Listener() {
            @Override
            public void onClick(Transaction transaction) {
                Intent intent = new Intent(requireContext(), AddTransactionActivity.class);
                intent.putExtra(AddTransactionActivity.EXTRA_TRANSACTION_ID, transaction.getId());
                startActivity(intent);
            }

            @Override
            public void onLongClick(Transaction transaction) {
                // Deleting is done from the Transactions tab
            }
        });
        recent.setAdapter(recentAdapter);

        btnPrev.setOnClickListener(v -> {
            month = month.minusMonths(1);
            refresh();
        });
        btnNext.setOnClickListener(v -> {
            if (month.isBefore(DateUtils.currentMonth())) {
                month = month.plusMonths(1);
                refresh();
            }
        });
        view.findViewById(R.id.btn_add_income).setOnClickListener(v ->
                openForm(Transaction.TYPE_INCOME));
        view.findViewById(R.id.btn_add_expense).setOnClickListener(v ->
                openForm(Transaction.TYPE_EXPENSE));
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh(); // reload after returning from the form
    }

    private void openForm(String type) {
        Intent intent = new Intent(requireContext(), AddTransactionActivity.class);
        intent.putExtra(AddTransactionActivity.EXTRA_TYPE, type);
        startActivity(intent);
    }

    private void refresh() {
        long from = DateUtils.startOfMonth(month);
        long to = DateUtils.startOfNextMonth(month);

        double income = dao.sumByType(Transaction.TYPE_INCOME, from, to);
        double expense = dao.sumByType(Transaction.TYPE_EXPENSE, from, to);
        MonthSummary summary = new MonthSummary(income, expense);

        tvMonth.setText(DateUtils.formatMonth(month));
        tvIncome.setText(MoneyUtils.format(summary.getIncome()));
        tvExpense.setText(MoneyUtils.format(summary.getExpense()));

        tvNet.setText(MoneyUtils.format(summary.getNetCashFlow()));
        tvNet.setTextColor(ContextCompat.getColor(requireContext(),
                summary.getNetCashFlow() >= 0 ? R.color.income_green : R.color.expense_red));

        Double rate = summary.getSavingsRatePercent();
        tvSavingsRate.setText(rate == null
                ? "Savings rate: N/A"
                : String.format(Locale.getDefault(), "Savings rate: %.2f%%", rate));

        double allIncome = dao.sumByType(Transaction.TYPE_INCOME, 0, Long.MAX_VALUE);
        double allExpense = dao.sumByType(Transaction.TYPE_EXPENSE, 0, Long.MAX_VALUE);
        tvBalance.setText("All-time balance: "
                + MoneyUtils.format(MoneyUtils.round2(allIncome - allExpense)));

        btnNext.setEnabled(month.isBefore(DateUtils.currentMonth()));

        List<Transaction> recent = dao.getRecent(5);
        recentAdapter.submit(recent);
        tvRecentEmpty.setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
    }
}