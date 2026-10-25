package com.example.internshiptracker.search;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobSearchFilters(
        @NotBlank @Size(max = 300) String keywords,
        @Size(max = 120) String domain,
        @Size(max = 160) String location,
        WorkMode workMode,
        @Size(max = 240) String experienceRequirement,
        @Min(1900) @Max(2200) Integer graduationYear,
        @Min(1) @Max(168) Integer postedWithinHours,
        @Size(max = 10) List<@Size(max = 100) String> companyPreferences,
        @Size(max = 20) List<@Size(max = 80) String> technologyPreferences) {

    public JobSearchFilters withDefaults() {
        return new JobSearchFilters(
                keywords.trim(),
                clean(domain),
                clean(location),
                workMode,
                clean(experienceRequirement),
                graduationYear,
                postedWithinHours == null ? 24 : postedWithinHours,
                companyPreferences == null ? List.of() : companyPreferences.stream().filter(value -> value != null && !value.isBlank())
                        .map(String::trim).distinct().toList(),
                technologyPreferences == null ? List.of() : technologyPreferences.stream()
                        .filter(value -> value != null && !value.isBlank()).map(String::trim).distinct().toList());
    }

    public JobSearchFilters withPostedWithinHours(Integer hours) {
        return new JobSearchFilters(
                keywords,
                domain,
                location,
                workMode,
                experienceRequirement,
                graduationYear,
                hours,
                companyPreferences,
                technologyPreferences);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public enum WorkMode {
        REMOTE,
        ONSITE,
        HYBRID
    }
}
