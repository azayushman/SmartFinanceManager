package com.example.smartfinancemanager.advice;

/** One piece of advice. Severity order is also the display order (most urgent first). */
public class AdviceItem {

    public enum Severity { ALERT, WARNING, INFO, GOOD }

    private final Severity severity;
    private final String title;
    private final String message;

    public AdviceItem(Severity severity, String title, String message) {
        this.severity = severity;
        this.title = title;
        this.message = message;
    }

    public Severity getSeverity() { return severity; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
}