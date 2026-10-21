package com.example.smartfinancemanager.ui.budgets;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfinancemanager.R;
import com.example.smartfinancemanager.data.BudgetDao;
import com.example.smartfinancemanager.data.model.BudgetUsage;
import com.example.smartfinancemanager.utils.DateUtils;
import com.example.smartfinancemanager.utils.MoneyUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.time.YearMonth;
import java.util.List;

public class BudgetsFragment extends Fragment {

    private BudgetDao dao;
    private BudgetAdapter adapter;
    private YearMonth month = DateUtils.currentMonth();

    private TextView tvMonth, tvSummary;
    private View emptyLayout;
    private MaterialButton btnNext;

    public BudgetsFragment() {
        super(R.layout.fragment_budgets);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dao = new BudgetDao(requireContext());
        tvMonth = view.findViewById(R.id.tv_budget_month);
        tvSummary = view.findViewById(R.id.tv_budget_summary);
        emptyLayout = view.findViewById(R.id.layout_budget_empty);
        btnNext = view.findViewById(R.id.btn_next_budget_month);

        RecyclerView recycler = view.findViewById(R.id.rv_budgets);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BudgetAdapter(new BudgetAdapter.Listener() {
            @Override
            public void onClick(BudgetUsage usage) {
                openForm(usage);
            }

            @Override
            public void onLongClick(BudgetUsage usage) {
                confirmDelete(usage);
            }
        });
        recycler.setAdapter(adapter);

        view.findViewById(R.id.btn_prev_budget_month).setOnClickListener(v -> {
            month = month.minusMonths(1);
            reload();
        });
        btnNext.setOnClickListener(v -> {
            if (canGoNext()) {
                month = month.plusMonths(1);
                reload();
            }
        });
        view.findViewById(R.id.btn_copy_budgets).setOnClickListener(v -> copyFromPrevious());
        view.findViewById(R.id.fab_set_budget).setOnClickListener(v -> openForm(null));
    }

    @Override
    public void onResume() {
        super.onResume();
        reload(); // picks up changes made in the form and new transactions
    }

    /** You can plan one month ahead, but no further. */
    private boolean canGoNext() {
        return month.isBefore(DateUtils.currentMonth().plusMonths(1));
    }

    private void reload() {
        List<BudgetUsage> list = dao.getUsage(month);
        adapter.submit(list);

        double budgeted = 0;
        double spent = 0;
        for (BudgetUsage u : list) {
            budgeted += u.getMonthlyLimit();
            spent += u.getSpent();
        }

        tvMonth.setText(DateUtils.formatMonth(month));
        tvSummary.setText(list.isEmpty()
                ? "No budgets set for this month"
                : "Budgeted " + MoneyUtils.format(MoneyUtils.round2(budgeted))
                  + " • Spent " + MoneyUtils.format(MoneyUtils.round2(spent)));
        emptyLayout.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        btnNext.setEnabled(canGoNext());
    }

    /** Pass null to add a new budget, or an existing one to edit it. */
    private void openForm(@Nullable BudgetUsage usage) {
        Intent intent = new Intent(requireContext(), SetBudgetActivity.class);
        intent.putExtra(SetBudgetActivity.EXTRA_MONTH, DateUtils.monthKey(month));
        if (usage != null) {
            intent.putExtra(SetBudgetActivity.EXTRA_CATEGORY_ID, usage.getCategoryId());
            intent.putExtra(SetBudgetActivity.EXTRA_LIMIT, usage.getMonthlyLimit());
        }
        startActivity(intent);
    }

    private void copyFromPrevious() {
        int copied = dao.copyFromPreviousMonth(month);
        if (copied < 0) {
            Toast.makeText(requireContext(), "Could not copy the budgets",
                    Toast.LENGTH_SHORT).show();
        } else if (copied == 0) {
            Toast.makeText(requireContext(), "No budgets to copy from "
                    + DateUtils.formatMonth(month.minusMonths(1)), Toast.LENGTH_SHORT).show();
        } else {
            reload();
            Snackbar.make(requireView(), copied + " budget(s) copied",
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete(BudgetUsage usage) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete budget?")
                .setMessage(usage.getCategoryName() + " • "
                        + MoneyUtils.format(usage.getMonthlyLimit()) + " for "
                        + DateUtils.formatMonth(month))
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (dao.deleteBudget(usage.getCategoryId(), month)) {
                        reload();
                    } else {
                        Toast.makeText(requireContext(), "Could not delete the budget",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
}