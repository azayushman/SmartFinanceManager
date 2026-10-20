package com.example.smartfinancemanager.ui.loans;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartfinancemanager.R;
import com.example.smartfinancemanager.data.CreditCardDao;
import com.example.smartfinancemanager.data.model.CardStatement;
import com.example.smartfinancemanager.data.model.CreditCard;
import com.example.smartfinancemanager.utils.CardDateUtils;
import com.example.smartfinancemanager.utils.DateUtils;
import com.example.smartfinancemanager.utils.MoneyUtils;
import com.example.smartfinancemanager.utils.ValidationUtils;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AddStatementActivity extends AppCompatActivity {

    public static final String EXTRA_CARD_ID = "card_id";

    private CreditCardDao dao;
    private CreditCard card;

    private TextInputLayout tilDate, tilTotal, tilMin;
    private TextInputEditText etDate, etTotal, etMin;
    private TextView tvDueInfo;

    private long statementDate;
    private long dueDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_statement);

        View root = findViewById(R.id.add_statement_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        dao = new CreditCardDao(this);
        card = dao.getById(getIntent().getLongExtra(EXTRA_CARD_ID, -1));
        if (card == null) {
            Toast.makeText(this, "Card not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tilDate = findViewById(R.id.til_statement_date);
        tilTotal = findViewById(R.id.til_total_due);
        tilMin = findViewById(R.id.til_min_due);
        etDate = findViewById(R.id.et_statement_date);
        etTotal = findViewById(R.id.et_total_due);
        etMin = findViewById(R.id.et_min_due);
        tvDueInfo = findViewById(R.id.tv_due_info);

        ((TextView) findViewById(R.id.tv_statement_card)).setText(card.getName());

        // Default: the latest statement date for this card, then its due date
        setStatementDate(CardDateUtils.latestOnOrBefore(
                card.getStatementDay(), System.currentTimeMillis()));

        etDate.setOnClickListener(v -> showDatePicker());
        findViewById(R.id.btn_cancel_statement).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save_statement).setOnClickListener(v -> save());
    }

    private void setStatementDate(long millis) {
        statementDate = millis;
        dueDate = CardDateUtils.firstAfter(card.getDueDay(), statementDate);
        etDate.setText(DateUtils.formatDate(statementDate));
        tvDueInfo.setText("Payment due date: " + DateUtils.formatDate(dueDate)
                + " (from the card's due day)");
        tilDate.setError(null);
    }

    private void showDatePicker() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .build();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Statement date")
                .setSelection(DateUtils.toPickerUtc(statementDate))
                .setCalendarConstraints(constraints)
                .build();
        picker.addOnPositiveButtonClickListener(selection ->
                setStatementDate(DateUtils.fromPickerUtc(selection)));
        picker.show(getSupportFragmentManager(), "statement_date_picker");
    }

    private String text(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void save() {
        boolean valid = true;

        String dateError = ValidationUtils.validateDate(statementDate, System.currentTimeMillis());
        tilDate.setError(dateError);
        if (dateError != null) valid = false;

        double total = 0;
        boolean totalValid = true;
        try {
            total = MoneyUtils.parseAmount(text(etTotal));
            tilTotal.setError(null);
        } catch (IllegalArgumentException e) {
            tilTotal.setError(e.getMessage());
            totalValid = false;
            valid = false;
        }

        double min = 0;
        try {
            min = MoneyUtils.parseAmount(text(etMin));
            if (totalValid && min > total) {
                tilMin.setError("Minimum due cannot be more than the total due");
                valid = false;
            } else {
                tilMin.setError(null);
            }
        } catch (IllegalArgumentException e) {
            tilMin.setError(e.getMessage());
            valid = false;
        }

        if (!valid) return;

        CardStatement statement = new CardStatement(0, card.getId(), statementDate,
                dueDate, total, min, false);
        if (dao.insertStatement(statement) > 0) {
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Could not save the statement. Please try again.",
                    Toast.LENGTH_LONG).show();
        }
    }
}