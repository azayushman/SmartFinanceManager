package com.example.smartfinancemanager.ui.transactions;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfinancemanager.R;
import com.example.smartfinancemanager.data.TransactionDao;
import com.example.smartfinancemanager.data.model.Transaction;
import com.example.smartfinancemanager.data.model.TransactionFilter;
import com.example.smartfinancemanager.utils.MoneyUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class TransactionsFragment extends Fragment {

    private static final String[] SORT_LABELS =
            {"Newest first", "Oldest first", "Highest amount", "Lowest amount"};
    private static final TransactionFilter.Sort[] SORT_VALUES = {
            TransactionFilter.Sort.DATE_DESC, TransactionFilter.Sort.DATE_ASC,
            TransactionFilter.Sort.AMOUNT_DESC, TransactionFilter.Sort.AMOUNT_ASC};

    private final TransactionFilter filter = new TransactionFilter();

    private TransactionDao dao;
    private TransactionAdapter adapter;
    private TextView tvEmpty;
    private MaterialButton btnSort;

    public TransactionsFragment() {
        super(R.layout.fragment_transactions);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dao = new TransactionDao(requireContext());
        tvEmpty = view.findViewById(R.id.tv_empty);
        btnSort = view.findViewById(R.id.btn_sort);

        RecyclerView recycler = view.findViewById(R.id.rv_transactions);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TransactionAdapter(new TransactionAdapter.Listener() {
            @Override
            public void onClick(Transaction transaction) {
                openForm(transaction.getId());
            }

            @Override
            public void onLongClick(Transaction transaction) {
                confirmDelete(transaction);
            }
        });
        recycler.setAdapter(adapter);

        TextInputEditText search = view.findViewById(R.id.et_search);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filter.searchText = s.toString();
                reload();
            }
        });

        ChipGroup chips = view.findViewById(R.id.chip_group_type);
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checked = checkedIds.get(0);
            if (checked == R.id.chip_income) {
                filter.type = Transaction.TYPE_INCOME;
            } else if (checked == R.id.chip_expense) {
                filter.type = Transaction.TYPE_EXPENSE;
            } else {
                filter.type = null;
            }
            reload();
        });

        btnSort.setOnClickListener(v -> showSortDialog());
        view.findViewById(R.id.fab_add).setOnClickListener(v -> openForm(-1));
    }

    @Override
    public void onResume() {
        super.onResume();
        reload(); // picks up changes made in the form
    }

    private void reload() {
        List<Transaction> list = dao.search(filter);
        adapter.submit(list);

        boolean empty = list.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            boolean filtering = !filter.searchText.trim().isEmpty() || filter.type != null;
            tvEmpty.setText(filtering
                    ? "No matching transactions"
                    : "No transactions yet.\nTap Add to record your first one.");
        }
    }

    private void openForm(long transactionId) {
        Intent intent = new Intent(requireContext(), AddTransactionActivity.class);
        if (transactionId > 0) {
            intent.putExtra(AddTransactionActivity.EXTRA_TRANSACTION_ID, transactionId);
        }
        startActivity(intent);
    }

    private void showSortDialog() {
        int current = 0;
        for (int i = 0; i < SORT_VALUES.length; i++) {
            if (SORT_VALUES[i] == filter.sort) {
                current = i;
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sort by")
                .setSingleChoiceItems(SORT_LABELS, current, (dialog, which) -> {
                    filter.sort = SORT_VALUES[which];
                    btnSort.setText(SORT_LABELS[which]);
                    dialog.dismiss();
                    reload();
                })
                .show();
    }

    private void confirmDelete(Transaction transaction) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete transaction?")
                .setMessage(transaction.getDescription() + " • "
                        + MoneyUtils.format(transaction.getAmount()))
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (dao.delete(transaction.getId())) {
                        reload();
                        Snackbar.make(requireView(), "Transaction deleted",
                                Snackbar.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Could not delete",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
}