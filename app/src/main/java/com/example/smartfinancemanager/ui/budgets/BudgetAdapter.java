package com.example.smartfinancemanager.ui.budgets;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfinancemanager.R;
import com.example.smartfinancemanager.data.model.BudgetUsage;
import com.example.smartfinancemanager.utils.BudgetCalculator;
import com.example.smartfinancemanager.utils.MoneyUtils;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.ViewHolder> {

    public interface Listener {
        void onClick(BudgetUsage usage);
        void onLongClick(BudgetUsage usage);
    }

    private final List<BudgetUsage> items = new ArrayList<>();
    private final Listener listener;

    public BudgetAdapter(Listener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void submit(List<BudgetUsage> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BudgetUsage u = items.get(position);
        BudgetCalculator.Status status = BudgetCalculator.status(u.getSpent(), u.getMonthlyLimit());
        Double percent = BudgetCalculator.usagePercent(u.getSpent(), u.getMonthlyLimit());
        double remaining = BudgetCalculator.remaining(u.getSpent(), u.getMonthlyLimit());

        @ColorRes int colorRes;
        String statusText;
        if (status == BudgetCalculator.Status.OVER) {
            colorRes = R.color.expense_red;
            statusText = remaining < 0
                    ? "Over budget by " + MoneyUtils.format(-remaining)
                    : "Limit reached";
        } else if (status == BudgetCalculator.Status.NEAR_LIMIT) {
            colorRes = R.color.warning_orange;
            statusText = "Near limit • " + MoneyUtils.format(remaining) + " left";
        } else {
            colorRes = R.color.income_green;
            statusText = "On track • " + MoneyUtils.format(remaining) + " left";
        }
        int color = ContextCompat.getColor(holder.itemView.getContext(), colorRes);

        holder.category.setText(u.getCategoryName());
        holder.percent.setText(percent == null
                ? "N/A" : String.format(Locale.getDefault(), "%.0f%%", percent));
        holder.amounts.setText(MoneyUtils.format(u.getSpent())
                + " of " + MoneyUtils.format(u.getMonthlyLimit()));

        holder.progress.setMax(100);
        holder.progress.setProgress(percent == null ? 0 : (int) Math.min(100, Math.round(percent)));
        holder.progress.setIndicatorColor(color);

        holder.status.setText(statusText);
        holder.status.setTextColor(color);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(u);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener == null) return false;
            listener.onLongClick(u);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView category, percent, amounts, status;
        final LinearProgressIndicator progress;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            category = itemView.findViewById(R.id.tv_budget_category);
            percent = itemView.findViewById(R.id.tv_budget_percent);
            amounts = itemView.findViewById(R.id.tv_budget_amounts);
            status = itemView.findViewById(R.id.tv_budget_status);
            progress = itemView.findViewById(R.id.progress_budget);
        }
    }
}