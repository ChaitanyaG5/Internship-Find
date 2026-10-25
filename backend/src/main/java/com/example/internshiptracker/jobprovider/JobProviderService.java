package com.example.internshiptracker.jobprovider;

import java.util.List;

import com.example.internshiptracker.search.JobSearchFilters;

public interface JobProviderService {

    String name();

    boolean isConfigured();

    List<JobResponse> search(JobSearchFilters filters);
}
