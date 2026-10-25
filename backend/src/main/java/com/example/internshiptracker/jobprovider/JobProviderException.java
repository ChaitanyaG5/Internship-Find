package com.example.internshiptracker.jobprovider;

public class JobProviderException extends RuntimeException {

    public JobProviderException(String message) {
        super(message);
    }

    public JobProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
