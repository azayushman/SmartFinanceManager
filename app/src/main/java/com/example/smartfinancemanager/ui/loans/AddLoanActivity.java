package com.example.smartfinancemanager.ui.loans;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartfinancemanager.R;
import com.example.smartfinancemanager.data.LoanDao;
import com.example.smartfinancemanager.data.model.EmiInstallment;
import com.example.smartfinancemanager.data.model.Loan;
import com.example.smartfinancemanager.utils.DateUtils;
import com.example.smartfinancemanager.utils.EmiCalculator;
import com.example.smartfinancemanager.utils.InputUtils;
import com.example.smartfinancemanager.utils.MoneyUtils;
import com.example.smartfinancemanager.utils.ValidationUtils;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

public class AddLoanActivity extends AppCompatActivity {

    private static final int MAX_NAME_LENGTH = 40;

    private LoanDao dao;
    private TextInputLayout tilName, tilLender, tilPrincipal, tilRate, tilTenure, tilStart, tilPaid;
    private TextInputEditText etName, etLender, etPrincipal, etRate, etTenure, etStart, etPaid;
    private TextView tvPreview;
    private long startDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_loan);

        View root = findViewById(R.id.add_loan_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        dao = new LoanDao(this);
        bindViews();

        startDate = DateUtils.startOfDay(System.currentTimeMillis());
        etStart.setText(DateUtils.formatDate(startDate));
        etStart.setOnClickListener(v -> showDatePicker());

        TextWatcher previewWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updatePreview();
            }
        };
        etPrincipal.addTextChangedListener(previewWatcher);
        etRate.addTextChangedListener(previewWatcher);
        etTenure.addTextChangedListener(previewWatcher);
        updatePreview();

        findViewById(R.id.btn_cancel_loan).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save_loan).setOnClickListener(v -> save());
    }

    private void bindViews() {
        tilName = findViewById(R.id.til_loan_name);
        tilLender = findViewById(R.id.til_lender);
        tilPrincipal = findViewById(R.id.til_principal);
        tilRate = findViewById(R.id.til_rate);
        tilTenure = findViewById(R.id.til_tenure);
        tilStart = findViewById(R.id.til_start);
        tilPaid = findViewById(R.id.til_paid);
        etName = findViewById(R.id.et_loan_name);
        etLender = findViewById(R.id.et_lender);
        etPrincipal = findViewById(R.id.et_principal);
        etRate = findViewById(R.id.et_rate);
        etTenure = findViewById(R.id.et_tenure);
        etStart = findViewById(R.id.et_start);
        etPaid = findViewById(R.id.et_paid);
        tvPreview = findViewById(R.id.tv_preview);
    }

    private String text(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void showDatePicker() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .build();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Loan start date")
                .setSelection(DateUtils.toPickerUtc(startDate))
                .setCalendarConstraints(constraints)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            startDate = DateUtils.fromPickerUtc(selection);
            etStart.setText(DateUtils.formatDate(startDate));
            tilStart.setError(null);
        });
        picker.show(getSupportFragmentManager(), "loan_start_picker");
    }

    /** Shows the EMI as soon as amount, rate and tenure are all valid. */
    private void updatePreview() {
        try {
            double principal = MoneyUtils.parseAmount(text(etPrincipal));
            double rate = InputUtils.parseRatePercent(text(etRate));
            int months = InputUtils.parseMonths(text(etTenure));

            List<EmiInstallment> schedule =
                    EmiCalculator.buildSchedule(principal, rate, months, startDate);
            double emi = EmiCalculator.monthlyEmi(principal, rate, months);
            double interest = EmiCalculator.totalInterest(schedule);

            tvPreview.setText("Monthly EMI: " + MoneyUtils.format(emi)
                    + "\nTotal interest: " + MoneyUtils.format(interest)
                    + "\nTotal payable: " + MoneyUtils.format(MoneyUtils.round2(principal + interest)));
        } catch (IllegalArgumentException e) {
            tvPreview.setText("Enter the amount, rate and tenure to see the EMI.");
        }
    }

    private void save() {
        boolean valid = true;

        String name = text(etName);
        String nameError = InputUtils.validateRequiredText(name, "the loan name", MAX_NAME_LENGTH);
        tilName.setError(nameError);
        if (nameError != null) valid = false;

        String lender = text(etLender);
        String lenderError = InputUtils.validateOptionalText(lender, MAX_NAME_LENGTH);
        tilLender.setError(lenderError);
        if (lenderError != null) valid = false;

        double principal = 0;
        try {
            principal = MoneyUtils.parseAmount(text(etPrincipal));
            tilPrincipal.setError(null);
        } catch (IllegalArgumentException e) {
            tilPrincipal.setError(e.getMessage());
            valid = false;
        }

        double rate = 0;
        try {
            rate = InputUtils.parseRatePercent(text(etRate));
            tilRate.setError(null);
        } catch (IllegalArgumentException e) {
            tilRate.setError(e.getMessage());
            valid = false;
        }

        int months = 0;
        boolean tenureValid = true;
        try {
            months = InputUtils.parseMonths(text(etTenure));
            tilTenure.setError(null);
        } catch (IllegalArgumentException e) {
            tilTenure.setError(e.getMessage());
            tenureValid = false;
            valid = false;
        }

        String dateError = ValidationUtils.validateDate(startDate, System.currentTimeMillis());
        tilStart.setError(dateError);
        if (dateError != null) valid = false;

        int alreadyPaid = 0;
        try {
            alreadyPaid = InputUtils.parseNonNegativeInt(text(etPaid),
                    tenureValid ? months : EmiCalculator.MAX_MONTHS);
            tilPaid.setError(null);
        } catch (IllegalArgumentException e) {
            tilPaid.setError(e.getMessage());
            valid = false;
        }

        if (!valid) return;

        double emi = EmiCalculator.monthlyEmi(principal, rate, months);
        List<EmiInstallment> schedule =
                EmiCalculator.buildSchedule(principal, rate, months, startDate);
        Loan loan = new Loan(0, name, lender.isEmpty() ? null : lender, principal, rate,
                months, emi, startDate, Loan.STATUS_ACTIVE, 0, principal, 0);

        long id = dao.insertWithSchedule(loan, schedule, alreadyPaid);
        if (id > 0) {
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Could not save the loan. Please try again.",
                    Toast.LENGTH_LONG).show();
        }
    }
}