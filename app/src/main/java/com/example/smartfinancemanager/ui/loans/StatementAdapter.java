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
import com.example.smartfinancemanager.data.model.CardStatement;
import com.example.smartfinancemanager.utils.DateUtils;
import com.example.smartfinancemanager.utils.MoneyUtils;

import java.util.ArrayList;
import java.util.List;

public class StatementAdapter extends RecyclerView.Adapter<StatementAdapter.ViewHolder> {

    public interface Listener {
        void onClick(CardStatement statement);
    }

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    private final List<CardStatement> items = new ArrayList<>();
    private final Listener listener;

    public StatementAdapter(Listener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void submit(List<CardStatement> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_statement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CardStatement s = items.get(position);
        long today = DateUtils.startOfDay(System.currentTimeMillis());

        holder.title.setText("Statement " + DateUtils.formatDate(s.getStatementDate()));
        holder.detail.setText("Due " + DateUtils.formatDate(s.getDueDate())
                + " • Minimum " + MoneyUtils.format(s.getMinDue()));
        holder.total.setText(MoneyUtils.format(s.getTotalDue()));

        if (s.isPaid()) {
            holder.status.setText("Paid");
            holder.status.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.income_green));
        } else if (s.getDueDate() < today) {
            holder.status.setText("Overdue");
            holder.status.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.expense_red));
        } else if (s.getDueDate() <= today + 7 * DAY_MILLIS) {
            holder.status.setText("Due soon");
            holder.status.setTextColor(holder.defaultStatusColors);
        } else {
            holder.status.setText("Upcoming");
            holder.status.setTextColor(holder.defaultStatusColors);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(s);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title, detail, total, status;
        final ColorStateList defaultStatusColors;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_stmt_title);
            detail = itemView.findViewById(R.id.tv_stmt_detail);
            total = itemView.findViewById(R.id.tv_stmt_total);
            status = itemView.findViewById(R.id.tv_stmt_status);
            defaultStatusColors = status.getTextColors();
        }
    }
}