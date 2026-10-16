package com.example.smartfinancemanager.ui.transactions;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartfinancemanager.R;
import com.example.smartfinancemanager.data.CategoryDao;
import com.example.smartfinancemanager.data.TransactionDao;
import com.example.smartfinancemanager.data.model.Category;
import com.example.smartfinancemanager.data.model.Transaction;
import com.example.smartfinancemanager.utils.DateUtils;
import com.example.smartfinancemanager.utils.MoneyUtils;
import com.example.smartfinancemanager.utils.ValidationUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

public class AddTransactionActivity extends AppCompatActivity {

    /** Pass a transaction id to edit it; leave out to add a new one. */
    public static final String EXTRA_TRANSACTION_ID = "transaction_id";
    /** Optional: Transaction.TYPE_INCOME or TYPE_EXPENSE to preselect the type. */
    public static final String EXTRA_TYPE = "type";

    private static final String[] PAYMENT_METHODS =
            {"Cash", "UPI", "Card", "Net banking", "Other"};

    private TransactionDao transactionDao;
    private CategoryDao categoryDao;

    private TextInputLayout tilAmount, tilCategory, tilDescription, tilDate, tilPayment, tilNote;
    private TextInputEditText etAmount, etDescription, etDate, etNote;
    private MaterialAutoCompleteTextView actvCategory, actvPayment;
    private MaterialButtonToggleGroup toggleType;

    private String type = Transaction.TYPE_EXPENSE;
    private long selectedDate;
    private Category selectedCategory;
    private long editingId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_transaction);

        View root = findViewById(R.id.add_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        transactionDao = new TransactionDao(this);
        categoryDao = new CategoryDao(this);
        bindViews();

        // Decide add vs edit before the listeners are attached
        Transaction existing = null;
        editingId = getIntent().getLongExtra(EXTRA_TRANSACTION_ID, -1);
        if (editingId > 0) {
            existing = transactionDao.getById(editingId);
            if (existing == null) {
                Toast.makeText(this, "Transaction not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            type = existing.getType();
            ((MaterialToolbar) findViewById(R.id.toolbar)).setTitle("Edit transaction");
        } else {
            String requested = getIntent().getStringExtra(EXTRA_TYPE);
            if (Transaction.TYPE_INCOME.equals(requested)) {
                type = Transaction.TYPE_INCOME;
            }
        }

        toggleType.check(Transaction.TYPE_INCOME.equals(type) ? R.id.btn_income : R.id.btn_expense);
        toggleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            type = (checkedId == R.id.btn_income)
                    ? Transaction.TYPE_INCOME : Transaction.TYPE_EXPENSE;
            selectedCategory = null;
            actvCategory.setText("", false);
            tilCategory.setError(null);
            loadCategories();
        });
        loadCategories();

        actvPayment.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, PAYMENT_METHODS));

        selectedDate = DateUtils.startOfDay(System.currentTimeMillis());
        actvPayment.setText(PAYMENT_METHODS[0], false);

        if (existing != null) {
            prefill(existing);
        }
        etDate.setText(DateUtils.formatDate(selectedDate));

        etDate.setOnClickListener(v -> showDatePicker());
        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save).setOnClickListener(v -> save());
    }

    private void bindViews() {
        tilAmount = findViewById(R.id.til_amount);
        tilCategory = findViewById(R.id.til_category);
        tilDescription = findViewById(R.id.til_description);
        tilDate = findViewById(R.id.til_date);
        tilPayment = findViewById(R.id.til_payment);
        tilNote = findViewById(R.id.til_note);
        etAmount = findViewById(R.id.et_amount);
        etDescription = findViewById(R.id.et_description);
        etDate = findViewById(R.id.et_date);
        etNote = findViewById(R.id.et_note);
        actvCategory = findViewById(R.id.actv_category);
        actvPayment = findViewById(R.id.actv_payment);
        toggleType = findViewById(R.id.toggle_type);
    }

    private void loadCategories() {
        List<Category> categories = categoryDao.getByType(type);
        actvCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categories));
        actvCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategory = (Category) parent.getItemAtPosition(position);
            tilCategory.setError(null);
        });
    }

    private void prefill(Transaction t) {
        etAmount.setText(MoneyUtils.toPlainString(t.getAmount()));
        etDescription.setText(t.getDescription());
        selectedDate = t.getDate();
        if (t.getPaymentMethod() != null) {
            actvPayment.setText(t.getPaymentMethod(), false);
        }
        if (t.getNote() != null) {
            etNote.setText(t.getNote());
        }
        for (Category c : categoryDao.getByType(type)) {
            if (c.getId() == t.getCategoryId()) {
                selectedCategory = c;
                actvCategory.setText(c.getName(), false);
                break;
            }
        }
    }

    private void showDatePicker() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .build();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(DateUtils.toPickerUtc(selectedDate))
                .setCalendarConstraints(constraints)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            selectedDate = DateUtils.fromPickerUtc(selection);
            etDate.setText(DateUtils.formatDate(selectedDate));
            tilDate.setError(null);
        });
        picker.show(getSupportFragmentManager(), "date_picker");
    }

    private String text(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void save() {
        boolean valid = true;

        double amount = 0;
        try {
            amount = MoneyUtils.parseAmount(text(etAmount));
            tilAmount.setError(null);
        } catch (IllegalArgumentException e) {
            tilAmount.setError(e.getMessage());
            valid = false;
        }

        if (selectedCategory == null || !type.equals(selectedCategory.getType())) {
            tilCategory.setError("Select a category");
            valid = false;
        } else {
            tilCategory.setError(null);
        }

        String description = text(etDescription);
        String descriptionError = ValidationUtils.validateDescription(description);
        tilDescription.setError(descriptionError);
        if (descriptionError != null) valid = false;

        String dateError = ValidationUtils.validateDate(selectedDate, System.currentTimeMillis());
        tilDate.setError(dateError);
        if (dateError != null) valid = false;

        String payment = actvPayment.getText() == null
                ? "" : actvPayment.getText().toString().trim();
        if (payment.isEmpty()) {
            tilPayment.setError("Select a payment method");
            valid = false;
        } else {
            tilPayment.setError(null);
        }

        String note = text(etNote);
        String noteError = ValidationUtils.validateNote(note);
        tilNote.setError(noteError);
        if (noteError != null) valid = false;

        if (!valid) return;

        Transaction transaction = new Transaction(
                editingId > 0 ? editingId : 0,
                amount,
                type,
                selectedCategory.getId(),
                selectedCategory.getName(),
                description,
                selectedDate,
                payment,
                note.isEmpty() ? null : note,
                0);

        boolean saved = editingId > 0
                ? transactionDao.update(transaction)
                : transactionDao.insert(transaction) > 0;

        if (saved) {
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Could not save. Please try again.", Toast.LENGTH_LONG).show();
        }
    }
}