package com.example.internshiptracker.api;

import java.util.List;

import com.example.internshiptracker.jobprovider.JobResponse;
import com.example.internshiptracker.search.JobSearchFilters;

public record JobSearchResponse(
        JobSearchFilters searchFilters,
        List<JobResponse> jobs,
        List<String> warnings) {
}
