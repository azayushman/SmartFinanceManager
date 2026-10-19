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
import com.example.smartfinancemanager.data.CreditCardDao;
import com.example.smartfinancemanager.data.LoanDao;
import com.example.smartfinancemanager.data.model.CreditCard;
import com.example.smartfinancemanager.data.model.Loan;
import com.example.smartfinancemanager.utils.FinanceCalculator;
import com.example.smartfinancemanager.utils.MoneyUtils;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;
import java.util.Locale;

public class LoansFragment extends Fragment {

    private LoanDao loanDao;
    private CreditCardDao cardDao;
    private LoanAdapter loanAdapter;
    private CardAdapter cardAdapter;

    private TextView tvSummary;
    private TextView tvEmpty;
    private RecyclerView rvLoans;
    private RecyclerView rvCards;
    private ExtendedFloatingActionButton fab;

    private boolean showingCards = false;

    public LoansFragment() {
        super(R.layout.fragment_loans);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loanDao = new LoanDao(requireContext());
        cardDao = new CreditCardDao(requireContext());

        tvSummary = view.findViewById(R.id.tv_total_emi);
        tvEmpty = view.findViewById(R.id.tv_loans_empty);
        rvLoans = view.findViewById(R.id.rv_loans);
        rvCards = view.findViewById(R.id.rv_cards);
        fab = view.findViewById(R.id.fab_add_loan);

        rvLoans.setLayoutManager(new LinearLayoutManager(requireContext()));
        loanAdapter = new LoanAdapter(loan -> {
            Intent intent = new Intent(requireContext(), LoanDetailActivity.class);
            intent.putExtra(LoanDetailActivity.EXTRA_LOAN_ID, loan.getId());
            startActivity(intent);
        });
        rvLoans.setAdapter(loanAdapter);

        rvCards.setLayoutManager(new LinearLayoutManager(requireContext()));
        cardAdapter = new CardAdapter(card -> {
            Intent intent = new Intent(requireContext(), CardDetailActivity.class);
            intent.putExtra(CardDetailActivity.EXTRA_CARD_ID, card.getId());
            startActivity(intent);
        });
        rvCards.setAdapter(cardAdapter);

        MaterialButtonToggleGroup toggle = view.findViewById(R.id.toggle_debt);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            showingCards = (checkedId == R.id.btn_show_cards);
            reload();
        });

        fab.setOnClickListener(v -> {
            Class<?> target = showingCards ? AddCardActivity.class : AddLoanActivity.class;
            startActivity(new Intent(requireContext(), target));
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        reload(); // picks up changes made in the other screens
    }

    private void reload() {
        if (showingCards) {
            showCards();
        } else {
            showLoans();
        }
    }

    private void showLoans() {
        rvCards.setVisibility(View.GONE);
        rvLoans.setVisibility(View.VISIBLE);
        fab.setText("Add loan");

        List<Loan> loans = loanDao.getAll();
        loanAdapter.submit(loans);
        tvSummary.setText("Monthly EMIs (active loans): "
                + MoneyUtils.format(loanDao.totalActiveMonthlyEmi()));
        tvEmpty.setText("No loans yet.\nTap Add loan to track an EMI.");
        tvEmpty.setVisibility(loans.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showCards() {
        rvLoans.setVisibility(View.GONE);
        rvCards.setVisibility(View.VISIBLE);
        fab.setText("Add card");

        List<CreditCard> cards = cardDao.getAll();
        cardAdapter.submit(cards);

        Double overall = FinanceCalculator.utilizationPercent(
                cardDao.totalOutstanding(), cardDao.totalCreditLimit());
        tvSummary.setText(overall == null
                ? "Overall card utilization: N/A"
                : String.format(Locale.getDefault(),
                "Overall card utilization: %.2f%%", overall));
        tvEmpty.setText("No credit cards yet.\nTap Add card to start tracking.");
        tvEmpty.setVisibility(cards.isEmpty() ? View.VISIBLE : View.GONE);
    }
}