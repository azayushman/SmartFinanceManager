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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfinancemanager.R;
import com.example.smartfinancemanager.data.LoanDao;
import com.example.smartfinancemanager.data.model.Loan;
import com.example.smartfinancemanager.utils.DateUtils;
import com.example.smartfinancemanager.utils.InputUtils;
import com.example.smartfinancemanager.utils.MoneyUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class LoanDetailActivity extends AppCompatActivity {

    public static final String EXTRA_LOAN_ID = "loan_id";

    private LoanDao dao;
    private InstallmentAdapter adapter;
    private long loanId;
    private Loan loan;

    private TextView tvTitle, tvSub, tvEmi, tvOutstanding, tvNext, tvProgress;
    private LinearProgressIndicator progress;
    private MaterialButton btnPay, btnUndo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_loan_detail);

        View root = findViewById(R.id.loan_detail_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        loanId = getIntent().getLongExtra(EXTRA_LOAN_ID, -1);
        dao = new LoanDao(this);

        tvTitle = findViewById(R.id.tv_detail_title);
        tvSub = findViewById(R.id.tv_detail_sub);
        tvEmi = findViewById(R.id.tv_detail_emi);
        tvOutstanding = findViewById(R.id.tv_detail_outstanding);
        tvNext = findViewById(R.id.tv_detail_next);
        tvProgress = findViewById(R.id.tv_detail_progress);
        progress = findViewById(R.id.progress_detail);
        btnPay = findViewById(R.id.btn_pay_next);
        btnUndo = findViewById(R.id.btn_undo);

        RecyclerView recycler = findViewById(R.id.rv_schedule);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InstallmentAdapter();
        recycler.setAdapter(adapter);

        btnPay.setOnClickListener(v -> confirmPayNext());
        btnUndo.setOnClickListener(v -> confirmUndo());
        findViewById(R.id.btn_delete_loan).setOnClickListener(v -> confirmDelete());

        refresh();
    }

    private void refresh() {
        loan = dao.getById(loanId);
        if (loan == null) {
            Toast.makeText(this, "Loan not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTitle.setText(loan.getName());

        StringBuilder sub = new StringBuilder();
        if (loan.getLender() != null) {
            sub.append(loan.getLender()).append(" • ");
        }
        sub.append(InputUtils.formatPercent(loan.getAnnualRatePercent())).append("% • ")
                .append(loan.getTenureMonths()).append(" months • started ")
                .append(DateUtils.formatDate(loan.getStartDate()));
        tvSub.setText(sub);

        tvEmi.setText("EMI: " + MoneyUtils.format(loan.getEmiAmount()) + " per month");
        tvOutstanding.setText("Outstanding principal: "
                + MoneyUtils.format(loan.getOutstandingAmount()));
        tvNext.setText(loan.isClosed()
                ? "Fully repaid"
                : "Next EMI due: " + DateUtils.formatDate(loan.getNextDueDate()));

        progress.setMax(loan.getTenureMonths());
        progress.setProgress(loan.getPaidInstallments());
        tvProgress.setText(loan.getPaidInstallments() + " of "
                + loan.getTenureMonths() + " EMIs paid");

        btnPay.setEnabled(!loan.isClosed());
        btnUndo.setEnabled(loan.getPaidInstallments() > 0);

        adapter.submit(dao.getSchedule(loanId));
    }

    private void confirmPayNext() {
        int number = loan.getPaidInstallments() + 1;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Mark EMI #" + number + " as paid?")
                .setMessage(MoneyUtils.format(loan.getEmiAmount())
                        + " will be recorded as paid today.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Mark paid", (dialog, which) -> {
                    long today = DateUtils.startOfDay(System.currentTimeMillis());
                    if (dao.markNextInstallmentPaid(loanId, today)) {
                        refresh();
                    } else {
                        Toast.makeText(this, "Nothing left to pay", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void confirmUndo() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Undo EMI #" + loan.getPaidInstallments() + "?")
                .setMessage("The most recent payment will be marked as unpaid again.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Undo", (dialog, which) -> {
                    if (dao.undoLastPayment(loanId)) {
                        refresh();
                    } else {
                        Toast.makeText(this, "No payment to undo", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete this loan?")
                .setMessage("The loan and its whole EMI schedule will be removed.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (dao.delete(loanId)) {
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this, "Could not delete the loan",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
}