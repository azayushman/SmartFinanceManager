package com.example.smartfinancemanager.ui.loans;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartfinancemanager.R;
import com.example.smartfinancemanager.data.CreditCardDao;
import com.example.smartfinancemanager.data.model.CreditCard;
import com.example.smartfinancemanager.utils.InputUtils;
import com.example.smartfinancemanager.utils.MoneyUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AddCardActivity extends AppCompatActivity {

    /** Pass a card id to edit it; leave out to add a new card. */
    public static final String EXTRA_CARD_ID = "card_id";

    private static final int MAX_TEXT_LENGTH = 40;

    private CreditCardDao dao;
    private long editingId = -1;

    private TextInputLayout tilName, tilBank, tilLimit, tilOutstanding, tilStatementDay, tilDueDay;
    private TextInputEditText etName, etBank, etLimit, etOutstanding, etStatementDay, etDueDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_card);

        View root = findViewById(R.id.add_card_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        dao = new CreditCardDao(this);
        bindViews();

        editingId = getIntent().getLongExtra(EXTRA_CARD_ID, -1);
        if (editingId > 0) {
            CreditCard existing = dao.getById(editingId);
            if (existing == null) {
                Toast.makeText(this, "Card not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            ((MaterialToolbar) findViewById(R.id.toolbar_card)).setTitle("Edit credit card");
            prefill(existing);
        }

        findViewById(R.id.btn_cancel_card).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save_card).setOnClickListener(v -> save());
    }

    private void bindViews() {
        tilName = findViewById(R.id.til_card_name);
        tilBank = findViewById(R.id.til_card_bank);
        tilLimit = findViewById(R.id.til_card_limit);
        tilOutstanding = findViewById(R.id.til_card_outstanding);
        tilStatementDay = findViewById(R.id.til_statement_day);
        tilDueDay = findViewById(R.id.til_due_day);
        etName = findViewById(R.id.et_card_name);
        etBank = findViewById(R.id.et_card_bank);
        etLimit = findViewById(R.id.et_card_limit);
        etOutstanding = findViewById(R.id.et_card_outstanding);
        etStatementDay = findViewById(R.id.et_statement_day);
        etDueDay = findViewById(R.id.et_due_day);
    }

    private void prefill(CreditCard card) {
        etName.setText(card.getName());
        if (card.getBank() != null) {
            etBank.setText(card.getBank());
        }
        etLimit.setText(MoneyUtils.toPlainString(card.getCreditLimit()));
        etOutstanding.setText(MoneyUtils.toPlainString(card.getOutstandingAmount()));
        etStatementDay.setText(String.valueOf(card.getStatementDay()));
        etDueDay.setText(String.valueOf(card.getDueDay()));
    }

    private String text(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void save() {
        boolean valid = true;

        String name = text(etName);
        String nameError = InputUtils.validateRequiredText(name, "the card name", MAX_TEXT_LENGTH);
        tilName.setError(nameError);
        if (nameError != null) valid = false;

        String bank = text(etBank);
        String bankError = InputUtils.validateOptionalText(bank, MAX_TEXT_LENGTH);
        tilBank.setError(bankError);
        if (bankError != null) valid = false;

        double limit = 0;
        try {
            limit = MoneyUtils.parseAmount(text(etLimit));
            tilLimit.setError(null);
        } catch (IllegalArgumentException e) {
            tilLimit.setError(e.getMessage());
            valid = false;
        }

        double outstanding = 0;
        try {
            outstanding = InputUtils.parseNonNegativeAmount(text(etOutstanding));
            tilOutstanding.setError(null);
        } catch (IllegalArgumentException e) {
            tilOutstanding.setError(e.getMessage());
            valid = false;
        }

        int statementDay = 0;
        try {
            statementDay = InputUtils.parseDayOfMonth(text(etStatementDay));
            tilStatementDay.setError(null);
        } catch (IllegalArgumentException e) {
            tilStatementDay.setError(e.getMessage());
            valid = false;
        }

        int dueDay = 0;
        try {
            dueDay = InputUtils.parseDayOfMonth(text(etDueDay));
            tilDueDay.setError(null);
        } catch (IllegalArgumentException e) {
            tilDueDay.setError(e.getMessage());
            valid = false;
        }

        if (!valid) return;

        CreditCard card = new CreditCard(editingId > 0 ? editingId : 0, name,
                bank.isEmpty() ? null : bank, limit, outstanding, statementDay, dueDay,
                0, 0, 0);

        boolean saved = editingId > 0 ? dao.update(card) : dao.insert(card) > 0;
        if (saved) {
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Could not save the card. Please try again.",
                    Toast.LENGTH_LONG).show();
        }
    }
}