package com.example.smartfinancemanager.ui.loans;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfinancemanager.R;
import com.example.smartfinancemanager.data.LoanDao;
import com.example.smartfinancemanager.data.model.Loan;
import com.example.smartfinancemanager.utils.MoneyUtils;

import java.util.List;

public class LoansFragment extends Fragment {

    private LoanDao dao;
    private LoanAdapter adapter;
    private TextView tvTotalEmi;
    private TextView tvEmpty;

    public LoansFragment() {
        super(R.layout.fragment_loans);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dao = new LoanDao(requireContext());
        tvTotalEmi = view.findViewById(R.id.tv_total_emi);
        tvEmpty = view.findViewById(R.id.tv_loans_empty);

        RecyclerView recycler = view.findViewById(R.id.rv_loans);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LoanAdapter(loan -> {
            Intent intent = new Intent(requireContext(), LoanDetailActivity.class);
            intent.putExtra(LoanDetailActivity.EXTRA_LOAN_ID, loan.getId());
            startActivity(intent);
        });
        recycler.setAdapter(adapter);

        view.findViewById(R.id.fab_add_loan).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddLoanActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        reload(); // picks up changes made in the other screens
    }

    private void reload() {
        List<Loan> loans = dao.getAll();
        adapter.submit(loans);
        tvEmpty.setVisibility(loans.isEmpty() ? View.VISIBLE : View.GONE);
        tvTotalEmi.setText("Monthly EMIs (active loans): "
                + MoneyUtils.format(dao.totalActiveMonthlyEmi()));
    }
}