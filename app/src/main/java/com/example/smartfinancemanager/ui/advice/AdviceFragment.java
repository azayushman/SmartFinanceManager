package com.example.smartfinancemanager.ui.advice;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.smartfinancemanager.R;
import com.example.smartfinancemanager.advice.AdviceDataLoader;
import com.example.smartfinancemanager.advice.AdviceEngine;
import com.example.smartfinancemanager.advice.AdviceInput;
import com.example.smartfinancemanager.advice.AdviceItem;
import com.example.smartfinancemanager.advice.SalaryPlan;
import com.example.smartfinancemanager.advice.SalaryPlanner;
import com.example.smartfinancemanager.utils.DateUtils;
import com.example.smartfinancemanager.utils.InputUtils;
import com.example.smartfinancemanager.utils.MoneyUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.YearMonth;

public class AdviceFragment extends Fragment {

    private AdviceDataLoader loader;
    private YearMonth month = DateUtils.currentMonth();

    private LinearLayout insightsContainer;
    private TextView tvMonth, tvPlanResult;
    private MaterialButton btnNext;
    private TextInputLayout tilSalary, tilAge;
    private TextInputEditText etSalary, etAge;
    private MaterialButtonToggleGroup toggleSplit;

    public AdviceFragment() {
        super(R.layout.fragment_advice);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loader = new AdviceDataLoader(requireContext());

        insightsContainer = view.findViewById(R.id.container_advice);
        tvMonth = view.findViewById(R.id.tv_advice_month);
        tvPlanResult = view.findViewById(R.id.tv_plan_result);
        btnNext = view.findViewById(R.id.btn_next_advice_month);
        tilSalary = view.findViewById(R.id.til_plan_salary);
        tilAge = view.findViewById(R.id.til_plan_age);
        etSalary = view.findViewById(R.id.et_plan_salary);
        etAge = view.findViewById(R.id.et_plan_age);
        toggleSplit = view.findViewById(R.id.toggle_split);
        ((TextView) view.findViewById(R.id.tv_disclaimer)).setText(AdviceEngine.DISCLAIMER);

        view.findViewById(R.id.btn_prev_advice_month).setOnClickListener(v -> {
            month = month.minusMonths(1);
            refresh();
        });
        btnNext.setOnClickListener(v -> {
            if (month.isBefore(DateUtils.currentMonth())) {
                month = month.plusMonths(1);
                refresh();
            }
        });
        view.findViewById(R.id.btn_plan).setOnClickListener(v -> planMoney());
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh(); // advice always reflects the latest data
    }

    private String text(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void refresh() {
        AdviceInput input = loader.load(month);

        tvMonth.setText(DateUtils.formatMonth(month));
        btnNext.setEnabled(month.isBefore(DateUtils.currentMonth()));

        insightsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (AdviceItem item : AdviceEngine.generate(input)) {
            View row = inflater.inflate(R.layout.item_advice, insightsContainer, false);
            TextView title = row.findViewById(R.id.tv_advice_title);
            TextView message = row.findViewById(R.id.tv_advice_message);
            title.setText(item.getTitle());
            message.setText(item.getMessage());

            Integer colorRes = colorFor(item.getSeverity());
            if (colorRes != null) {
                title.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
            }
            insightsContainer.addView(row);
        }

        // Offer this month's income as the salary to plan with
        if (input.monthlyIncome > 0 && text(etSalary).isEmpty()) {
            etSalary.setText(MoneyUtils.toPlainString(input.monthlyIncome));
        }
    }

    @Nullable
    @ColorRes
    private Integer colorFor(AdviceItem.Severity severity) {
        switch (severity) {
            case ALERT:
                return R.color.expense_red;
            case WARNING:
                return R.color.warning_orange;
            case GOOD:
                return R.color.income_green;
            case INFO:
            default:
                return null; // keep the default text colour
        }
    }

    private void planMoney() {
        boolean valid = true;

        double salary = 0;
        try {
            salary = MoneyUtils.parseAmount(text(etSalary));
            tilSalary.setError(null);
        } catch (IllegalArgumentException e) {
            tilSalary.setError(e.getMessage());
            valid = false;
        }

        int age = 0;
        try {
            age = InputUtils.parseAge(text(etAge));
            tilAge.setError(null);
        } catch (IllegalArgumentException e) {
            tilAge.setError(e.getMessage());
            valid = false;
        }

        if (!valid) return;

        SalaryPlanner.Split split = toggleSplit.getCheckedButtonId() == R.id.btn_split_tight
                ? SalaryPlanner.Split.TIGHT : SalaryPlanner.Split.BALANCED;
        SalaryPlan plan = SalaryPlanner.plan(salary, age, split);

        String result = "Needs (" + plan.getNeedsPercent() + "%): "
                + MoneyUtils.format(plan.getNeedsAmount())
                + "\nWants (" + plan.getWantsPercent() + "%): "
                + MoneyUtils.format(plan.getWantsAmount())
                + "\nSavings and investments (" + plan.getSavingsPercent() + "%): "
                + MoneyUtils.format(plan.getSavingsAmount())
                + "\n\nInvesting the savings (100 minus your age in equity):"
                + "\nEquity " + plan.getEquityPercent() + "%: "
                + MoneyUtils.format(plan.getEquityAmount())
                + "\nDebt " + plan.getDebtPercent() + "%: "
                + MoneyUtils.format(plan.getDebtAmount())
                + "\n\nEmergency fund target: " + MoneyUtils.format(plan.getEmergencyFundMin())
                + " to " + MoneyUtils.format(plan.getEmergencyFundMax())
                + " (3 to 6 months of needs). Build it first, and clear high-interest debt "
                + "before taking investment risk.";

        tvPlanResult.setText(result);
        tvPlanResult.setVisibility(View.VISIBLE);
    }
}