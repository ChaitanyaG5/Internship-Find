package com.example.internshiptracker.jobprovider;

import java.time.Instant;

public record JobResponse(
        String externalId,
        String title,
        String company,
        String description,
        String location,
        Instant postedAt,
        String source,
        String applicationUrl) {
}
