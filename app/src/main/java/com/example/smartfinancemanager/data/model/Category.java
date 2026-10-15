package com.example.smartfinancemanager.data.model;

public class Category {
    private long id;
    private String name;
    private String type;    // "INCOME" or "EXPENSE"
    private String bucket;  // "NEED", "WANT", "SAVING" or null

    public Category(long id, String name, String type, String bucket) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.bucket = bucket;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getBucket() { return bucket; }

    @Override
    public String toString() { return name; } // lets a Spinner show the name
}