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
import com.example.smartfinancemanager.data.model.EmiInstallment;
import com.example.smartfinancemanager.utils.DateUtils;
import com.example.smartfinancemanager.utils.MoneyUtils;

import java.util.ArrayList;
import java.util.List;

public class InstallmentAdapter extends RecyclerView.Adapter<InstallmentAdapter.ViewHolder> {

    private final List<EmiInstallment> items = new ArrayList<>();

    @SuppressLint("NotifyDataSetChanged")
    public void submit(List<EmiInstallment> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_installment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmiInstallment item = items.get(position);
        long today = DateUtils.startOfDay(System.currentTimeMillis());

        holder.title.setText("#" + item.getInstallmentNo() + " • "
                + DateUtils.formatDate(item.getDueDate()));
        holder.detail.setText("Principal " + MoneyUtils.format(item.getPrincipalAmount())
                + " • Interest " + MoneyUtils.format(item.getInterestAmount())
                + " • Balance " + MoneyUtils.format(item.getBalanceAmount()));
        holder.emi.setText(MoneyUtils.format(item.getEmiAmount()));

        if (item.isPaid()) {
            holder.status.setText("Paid");
            holder.status.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.income_green));
        } else if (item.getDueDate() < today) {
            holder.status.setText("Overdue");
            holder.status.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.expense_red));
        } else {
            holder.status.setText("Upcoming");
            holder.status.setTextColor(holder.defaultStatusColors);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title, detail, emi, status;
        final ColorStateList defaultStatusColors;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_inst_title);
            detail = itemView.findViewById(R.id.tv_inst_detail);
            emi = itemView.findViewById(R.id.tv_inst_emi);
            status = itemView.findViewById(R.id.tv_inst_status);
            defaultStatusColors = status.getTextColors();
        }
    }
}