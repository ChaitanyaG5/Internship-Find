package com.example.internshiptracker.api;

import com.example.internshiptracker.ai.LanguageModelException;
import com.example.internshiptracker.jobprovider.JobProviderException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class JobSearchExceptionHandler {

    @ExceptionHandler(LanguageModelException.class)
    public ResponseEntity<ApiError> handleLanguageModel(LanguageModelException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(JobProviderException.class)
    public ResponseEntity<ApiError> handleJobProvider(JobProviderException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(new ApiError("Enter a search query of 1 to 500 characters and a valid posting-age filter."));
    }
}
