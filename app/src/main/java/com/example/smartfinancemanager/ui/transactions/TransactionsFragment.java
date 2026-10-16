package com.example.smartfinancemanager.ui.transactions;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartfinancemanager.R;

public class TransactionsFragment extends Fragment {

    public TransactionsFragment() {
        super(R.layout.fragment_transactions);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.placeholder_text).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddTransactionActivity.class)));
    }
}