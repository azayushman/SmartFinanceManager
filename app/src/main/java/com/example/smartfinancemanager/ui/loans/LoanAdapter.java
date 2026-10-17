package com.example.smartfinancemanager.ui.loans;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfinancemanager.R;
import com.example.smartfinancemanager.data.model.Loan;
import com.example.smartfinancemanager.utils.DateUtils;
import com.example.smartfinancemanager.utils.InputUtils;
import com.example.smartfinancemanager.utils.MoneyUtils;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class LoanAdapter extends RecyclerView.Adapter<LoanAdapter.ViewHolder> {

    public interface Listener {
        void onClick(Loan loan);
    }

    private final List<Loan> items = new ArrayList<>();
    private final Listener listener;

    public LoanAdapter(Listener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void submit(List<Loan> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_loan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Loan loan = items.get(position);

        holder.name.setText(loan.getName());
        holder.emi.setText(MoneyUtils.format(loan.getEmiAmount()) + " / mo");

        StringBuilder sub = new StringBuilder();
        if (loan.getLender() != null) {
            sub.append(loan.getLender()).append(" • ");
        }
        sub.append(InputUtils.formatPercent(loan.getAnnualRatePercent())).append("% • ")
                .append(loan.getTenureMonths()).append(" months");
        holder.sub.setText(sub);

        holder.progress.setMax(loan.getTenureMonths());
        holder.progress.setProgress(loan.getPaidInstallments());
        holder.progressText.setText(loan.getPaidInstallments() + " of "
                + loan.getTenureMonths() + " EMIs paid");

        if (loan.isClosed()) {
            holder.status.setText("Fully repaid");
        } else {
            holder.status.setText("Outstanding " + MoneyUtils.format(loan.getOutstandingAmount())
                    + " • Next EMI " + DateUtils.formatDate(loan.getNextDueDate()));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(loan);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name, emi, sub, progressText, status;
        final LinearProgressIndicator progress;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_loan_name);
            emi = itemView.findViewById(R.id.tv_loan_emi);
            sub = itemView.findViewById(R.id.tv_loan_sub);
            progressText = itemView.findViewById(R.id.tv_loan_progress);
            status = itemView.findViewById(R.id.tv_loan_status);
            progress = itemView.findViewById(R.id.progress_loan);
        }
    }
}