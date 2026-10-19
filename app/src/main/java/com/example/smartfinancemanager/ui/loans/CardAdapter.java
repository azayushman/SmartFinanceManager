package com.example.smartfinancemanager.ui.loans;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfinancemanager.R;
import com.example.smartfinancemanager.data.model.CreditCard;
import com.example.smartfinancemanager.utils.DateUtils;
import com.example.smartfinancemanager.utils.FinanceCalculator;
import com.example.smartfinancemanager.utils.MoneyUtils;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {

    public interface Listener {
        void onClick(CreditCard card);
    }

    private final List<CreditCard> items = new ArrayList<>();
    private final Listener listener;

    public CardAdapter(Listener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void submit(List<CreditCard> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CreditCard card = items.get(position);

        holder.name.setText(card.getName());
        holder.bank.setText(card.getBank() == null ? "Credit card" : card.getBank());
        holder.balance.setText("Outstanding " + MoneyUtils.format(card.getOutstandingAmount())
                + " of " + MoneyUtils.format(card.getCreditLimit()));

        Double utilization = FinanceCalculator.utilizationPercent(
                card.getOutstandingAmount(), card.getCreditLimit());
        holder.progress.setMax(100);
        holder.progress.setProgress(utilization == null
                ? 0 : (int) Math.min(100, Math.round(utilization)));

        if (utilization == null) {
            holder.util.setText("Utilization: N/A");
            holder.util.setTextColor(holder.defaultUtilColors);
        } else {
            holder.util.setText(String.format(Locale.getDefault(),
                    "Utilization: %.2f%%", utilization));
            if (utilization > FinanceCalculator.UTILIZATION_WARNING_PERCENT) {
                holder.util.setTextColor(ContextCompat.getColor(
                        holder.itemView.getContext(), R.color.expense_red));
            } else {
                holder.util.setTextColor(holder.defaultUtilColors);
            }
        }

        if (card.getNextDueDate() > 0) {
            holder.due.setText("Next payment due " + DateUtils.formatDate(card.getNextDueDate())
                    + " • " + MoneyUtils.format(card.getNextDueAmount()));
        } else {
            holder.due.setText("No statement due");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(card);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name, bank, balance, util, due;
        final LinearProgressIndicator progress;
        final ColorStateList defaultUtilColors;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_card_name);
            bank = itemView.findViewById(R.id.tv_card_bank);
            balance = itemView.findViewById(R.id.tv_card_balance);
            util = itemView.findViewById(R.id.tv_card_util);
            due = itemView.findViewById(R.id.tv_card_due);
            progress = itemView.findViewById(R.id.progress_card);
            defaultUtilColors = util.getTextColors();
        }
    }
}