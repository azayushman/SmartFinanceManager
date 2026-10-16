package com.example.smartfinancemanager.utils;

public final class ValidationUtils {

    public static final int MAX_DESCRIPTION_LENGTH = 60;
    public static final int MAX_NOTE_LENGTH = 200;

    private ValidationUtils() {}

    public static String validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "Enter a description";
        }
        if (description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            return "Keep it under " + MAX_DESCRIPTION_LENGTH + " characters";
        }
        return null;
    }

    public static String validateNote(String note) {
        if (note != null && note.trim().length() > MAX_NOTE_LENGTH) {
            return "Keep it under " + MAX_NOTE_LENGTH + " characters";
        }
        return null;
    }

    /** Policy: transactions cannot be dated in the future. */
    public static String validateDate(long dateMillis, long nowMillis) {
        if (dateMillis <= 0) {
            return "Select a date";
        }
        if (dateMillis > nowMillis) {
            return "Date cannot be in the future";
        }
        return null;
    }
}