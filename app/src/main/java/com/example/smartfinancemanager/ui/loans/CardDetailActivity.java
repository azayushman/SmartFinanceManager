package com.example.smartfinancemanager.ui.loans;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfinancemanager.R;
import com.example.smartfinancemanager.data.CreditCardDao;
import com.example.smartfinancemanager.data.model.CardStatement;
import com.example.smartfinancemanager.data.model.CreditCard;
import com.example.smartfinancemanager.utils.DateUtils;
import com.example.smartfinancemanager.utils.FinanceCalculator;
import com.example.smartfinancemanager.utils.MoneyUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;
import java.util.Locale;

public class CardDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CARD_ID = "card_id";

    private CreditCardDao dao;
    private StatementAdapter adapter;
    private long cardId;
    private CreditCard card;

    private TextView tvTitle, tvSub, tvLimit, tvOutstanding, tvAvailable, tvUtil, tvEmpty;
    private LinearProgressIndicator progress;
    private ColorStateList defaultUtilColors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_card_detail);

        View root = findViewById(R.id.card_detail_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        cardId = getIntent().getLongExtra(EXTRA_CARD_ID, -1);
        dao = new CreditCardDao(this);

        tvTitle = findViewById(R.id.tv_detail_card_title);
        tvSub = findViewById(R.id.tv_detail_card_sub);
        tvLimit = findViewById(R.id.tv_detail_card_limit);
        tvOutstanding = findViewById(R.id.tv_detail_card_outstanding);
        tvAvailable = findViewById(R.id.tv_detail_card_available);
        tvUtil = findViewById(R.id.tv_detail_card_util);
        tvEmpty = findViewById(R.id.tv_statements_empty);
        progress = findViewById(R.id.progress_detail_card);
        defaultUtilColors = tvUtil.getTextColors();

        RecyclerView recycler = findViewById(R.id.rv_statements);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StatementAdapter(this::showStatementOptions);
        recycler.setAdapter(adapter);

        findViewById(R.id.btn_add_statement).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddStatementActivity.class);
            intent.putExtra(AddStatementActivity.EXTRA_CARD_ID, cardId);
            startActivity(intent);
        });
        findViewById(R.id.btn_edit_card).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddCardActivity.class);
            intent.putExtra(AddCardActivity.EXTRA_CARD_ID, cardId);
            startActivity(intent);
        });
        findViewById(R.id.btn_delete_card).setOnClickListener(v -> confirmDeleteCard());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh(); // reload after returning from the edit or statement screens
    }

    private void refresh() {
        card = dao.getById(cardId);
        if (card == null) {
            Toast.makeText(this, "Card not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTitle.setText(card.getName());
        String bank = card.getBank() == null ? "Credit card" : card.getBank();
        tvSub.setText(bank + " • statement on day " + card.getStatementDay()
                + " • due on day " + card.getDueDay());
        tvLimit.setText("Credit limit: " + MoneyUtils.format(card.getCreditLimit()));
        tvOutstanding.setText("Outstanding: " + MoneyUtils.format(card.getOutstandingAmount()));
        tvAvailable.setText("Available credit: " + MoneyUtils.format(
                MoneyUtils.round2(card.getCreditLimit() - card.getOutstandingAmount())));

        Double utilization = FinanceCalculator.utilizationPercent(
                card.getOutstandingAmount(), card.getCreditLimit());
        progress.setMax(100);
        progress.setProgress(utilization == null
                ? 0 : (int) Math.min(100, Math.round(utilization)));
        if (utilization == null) {
            tvUtil.setText("Utilization: N/A");
            tvUtil.setTextColor(defaultUtilColors);
        } else {
            tvUtil.setText(String.format(Locale.getDefault(), "Utilization: %.2f%%", utilization));
            if (utilization > FinanceCalculator.UTILIZATION_WARNING_PERCENT) {
                tvUtil.setTextColor(ContextCompat.getColor(this, R.color.expense_red));
            } else {
                tvUtil.setTextColor(defaultUtilColors);
            }
        }

        List<CardStatement> statements = dao.getStatements(cardId);
        adapter.submit(statements);
        tvEmpty.setVisibility(statements.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showStatementOptions(CardStatement statement) {
        String title = "Statement " + DateUtils.formatDate(statement.getStatementDate())
                + " • " + MoneyUtils.format(statement.getTotalDue());

        if (statement.isPaid()) {
            String[] options = {"Mark unpaid", "Delete statement"};
            new MaterialAlertDialogBuilder(this)
                    .setTitle(title)
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            applyPaid(statement, false, false);
                        } else {
                            confirmDeleteStatement(statement);
                        }
                    })
                    .show();
        } else {
            String[] options = {
                    "Mark paid",
                    "Mark paid and reduce balance by " + MoneyUtils.format(statement.getTotalDue()),
                    "Delete statement"};
            new MaterialAlertDialogBuilder(this)
                    .setTitle(title)
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            applyPaid(statement, true, false);
                        } else if (which == 1) {
                            applyPaid(statement, true, true);
                        } else {
                            confirmDeleteStatement(statement);
                        }
                    })
                    .show();
        }
    }

    private void applyPaid(CardStatement statement, boolean paid, boolean reduceBalance) {
        if (dao.setStatementPaid(statement.getId(), paid, reduceBalance)) {
            refresh();
        } else {
            Toast.makeText(this, "Could not update the statement", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteStatement(CardStatement statement) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete this statement?")
                .setMessage("This does not change the card's outstanding balance.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (dao.deleteStatement(statement.getId())) {
                        refresh();
                    } else {
                        Toast.makeText(this, "Could not delete the statement",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void confirmDeleteCard() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete this card?")
                .setMessage("The card and all of its statements will be removed.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (dao.delete(cardId)) {
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this, "Could not delete the card",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
}