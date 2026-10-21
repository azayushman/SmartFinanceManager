package com.example.smartfinancemanager.ui.budgets;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartfinancemanager.R;
import com.example.smartfinancemanager.data.BudgetDao;
import com.example.smartfinancemanager.data.CategoryDao;
import com.example.smartfinancemanager.data.model.Category;
import com.example.smartfinancemanager.data.model.Transaction;
import com.example.smartfinancemanager.utils.DateUtils;
import com.example.smartfinancemanager.utils.MoneyUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class SetBudgetActivity extends AppCompatActivity {

    /** Month as "2026-10". */
    public static final String EXTRA_MONTH = "month";
    /** Optional: pass the category id (and current limit) to edit an existing budget. */
    public static final String EXTRA_CATEGORY_ID = "category_id";
    public static final String EXTRA_LIMIT = "limit";

    private BudgetDao dao;
    private YearMonth month;
    private final List<Category> categories = new ArrayList<>();
    private Category selected;

    private TextInputLayout tilCategory, tilLimit;
    private MaterialAutoCompleteTextView actvCategory;
    private TextInputEditText etLimit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_budget);

        View root = findViewById(R.id.set_budget_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        dao = new BudgetDao(this);
        month = parseMonth(getIntent().getStringExtra(EXTRA_MONTH));

        tilCategory = findViewById(R.id.til_budget_category);
        tilLimit = findViewById(R.id.til_budget_limit);
        actvCategory = findViewById(R.id.actv_budget_category);
        etLimit = findViewById(R.id.et_budget_limit);
        ((TextView) findViewById(R.id.tv_budget_for))
                .setText("Budget for " + DateUtils.formatMonth(month));

        // Savings are not a spending category, so they are left out
        for (Category c : new CategoryDao(this).getByType(Transaction.TYPE_EXPENSE)) {
            if (!"SAVING".equals(c.getBucket())) {
                categories.add(c);
            }
        }
        actvCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categories));
        actvCategory.setOnItemClickListener((parent, view, position, id) -> {
            selected = (Category) parent.getItemAtPosition(position);
            tilCategory.setError(null);
        });

        long editCategoryId = getIntent().getLongExtra(EXTRA_CATEGORY_ID, -1);
        if (editCategoryId > 0) {
            ((MaterialToolbar) findViewById(R.id.toolbar_budget)).setTitle("Edit budget");
            for (Category c : categories) {
                if (c.getId() == editCategoryId) {
                    selected = c;
                    actvCategory.setText(c.getName(), false);
                    break;
                }
            }
            tilCategory.setEnabled(false); // the category of an existing budget is fixed
            double limit = getIntent().getDoubleExtra(EXTRA_LIMIT, 0);
            if (limit > 0) {
                etLimit.setText(MoneyUtils.toPlainString(limit));
            }
        }

        findViewById(R.id.btn_cancel_budget).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save_budget).setOnClickListener(v -> save());
    }

    private static YearMonth parseMonth(String value) {
        if (value != null) {
            try {
                return YearMonth.parse(value);
            } catch (DateTimeParseException ignored) {
                // fall through to the current month
            }
        }
        return DateUtils.currentMonth();
    }

    private void save() {
        boolean valid = true;

        if (selected == null) {
            tilCategory.setError("Select a category");
            valid = false;
        } else {
            tilCategory.setError(null);
        }

        double limit = 0;
        try {
            limit = MoneyUtils.parseAmount(
                    etLimit.getText() == null ? "" : etLimit.getText().toString().trim());
            tilLimit.setError(null);
        } catch (IllegalArgumentException e) {
            tilLimit.setError(e.getMessage());
            valid = false;
        }

        if (!valid) return;

        if (dao.setBudget(selected.getId(), month, limit)) {
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Could not save the budget. Please try again.",
                    Toast.LENGTH_LONG).show();
        }
    }
}