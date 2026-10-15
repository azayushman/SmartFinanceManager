package com.example.smartfinancemanager;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.smartfinancemanager.ui.advice.AdviceFragment;
import com.example.smartfinancemanager.ui.budgets.BudgetsFragment;
import com.example.smartfinancemanager.ui.home.HomeFragment;
import com.example.smartfinancemanager.ui.loans.LoansFragment;
import com.example.smartfinancemanager.ui.transactions.TransactionsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, 0); // bottom bar handles its own inset
            return insets;
        });

        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            Fragment fragment = fragmentFor(item.getItemId());
            if (fragment == null) return false;
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        });

        if (savedInstanceState == null) {
            nav.setSelectedItemId(R.id.nav_home);
        }
    }

    private Fragment fragmentFor(int itemId) {
        if (itemId == R.id.nav_home) return new HomeFragment();
        if (itemId == R.id.nav_transactions) return new TransactionsFragment();
        if (itemId == R.id.nav_budgets) return new BudgetsFragment();
        if (itemId == R.id.nav_loans) return new LoansFragment();
        if (itemId == R.id.nav_advice) return new AdviceFragment();
        return null;
    }
}