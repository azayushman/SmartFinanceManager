package com.example.smartfinancemanager.ui.transactions;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfinancemanager.R;
import com.example.smartfinancemanager.data.model.Transaction;
import com.example.smartfinancemanager.utils.DateUtils;
import com.example.smartfinancemanager.utils.MoneyUtils;

import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    public interface Listener {
        void onClick(Transaction transaction);
        void onLongClick(Transaction transaction);
    }

    private final List<Transaction> items = new ArrayList<>();
    private final Listener listener;

    public TransactionAdapter(Listener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void submit(List<Transaction> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction t = items.get(position);
        boolean income = Transaction.TYPE_INCOME.equals(t.getType());

        holder.description.setText(t.getDescription());

        StringBuilder meta = new StringBuilder(t.getCategoryName())
                .append(" • ").append(DateUtils.formatDate(t.getDate()));
        if (t.getPaymentMethod() != null) {
            meta.append(" • ").append(t.getPaymentMethod());
        }
        holder.meta.setText(meta);

        holder.amount.setText((income ? "+" : "-") + MoneyUtils.format(t.getAmount()));
        holder.amount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                income ? R.color.income_green : R.color.expense_red));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(t);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener == null) return false;
            listener.onLongClick(t);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView description;
        final TextView meta;
        final TextView amount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            description = itemView.findViewById(R.id.tv_description);
            meta = itemView.findViewById(R.id.tv_meta);
            amount = itemView.findViewById(R.id.tv_amount);
        }
    }
}