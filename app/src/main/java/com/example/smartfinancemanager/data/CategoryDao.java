package com.example.smartfinancemanager.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.smartfinancemanager.data.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

    private final DatabaseHelper helper;

    public CategoryDao(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    public List<Category> getAll() {
        return query(null, null);
    }

    /** type must be "INCOME" or "EXPENSE" */
    public List<Category> getByType(String type) {
        return query("type = ?", new String[]{type});
    }

    private List<Category> query(String where, String[] args) {
        List<Category> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(DatabaseHelper.T_CATEGORIES,
                new String[]{"id", "name", "type", "bucket"},
                where, args, null, null, "name ASC")) {
            while (c.moveToNext()) {
                list.add(new Category(
                        c.getLong(0),
                        c.getString(1),
                        c.getString(2),
                        c.isNull(3) ? null : c.getString(3)));
            }
        }
        return list;
    }
}