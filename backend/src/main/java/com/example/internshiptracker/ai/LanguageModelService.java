package com.example.internshiptracker.ai;

import com.example.internshiptracker.search.JobSearchFilters;

public interface LanguageModelService {

    String provider();

    boolean isConfigured();

    JobSearchFilters extractFilters(String query);
}
