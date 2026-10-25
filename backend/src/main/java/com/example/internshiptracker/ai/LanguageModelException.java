package com.example.internshiptracker.ai;

public class LanguageModelException extends RuntimeException {

    public LanguageModelException(String message) {
        super(message);
    }

    public LanguageModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
