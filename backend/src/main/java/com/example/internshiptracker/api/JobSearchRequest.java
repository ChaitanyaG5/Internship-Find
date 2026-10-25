package com.example.internshiptracker.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JobSearchRequest(
        @NotBlank @Size(max = 500) String query,
        @Size(max = 16) @Pattern(regexp = "(?i)AUTO|OPENROUTER") String provider,
        @Min(1) @Max(168) Integer postedWithinHours) {
}
