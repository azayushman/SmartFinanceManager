package com.example.smartfinancemanager;

import android.app.Application;
import android.util.Log;

import com.example.smartfinancemanager.data.CategoryDao;
import com.example.smartfinancemanager.data.DatabaseHelper;

public class SmartFinanceApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DatabaseHelper.getInstance(this).getWritableDatabase(); // creates tables on first run
        Log.d("SFM_DB", "Categories in database: " + new CategoryDao(this).getAll().size());
    }
}