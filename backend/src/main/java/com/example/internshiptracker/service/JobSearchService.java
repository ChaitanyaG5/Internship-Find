package com.example.internshiptracker.service;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.internshiptracker.ai.LanguageModelRouter;
import com.example.internshiptracker.api.JobSearchRequest;
import com.example.internshiptracker.api.JobSearchResponse;
import com.example.internshiptracker.jobprovider.JobProviderException;
import com.example.internshiptracker.jobprovider.JobProviderService;
import com.example.internshiptracker.jobprovider.JobResponse;
import com.example.internshiptracker.search.JobSearchFilters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JobSearchService {

    private final LanguageModelRouter languageModelRouter;
    private final List<JobProviderService> providers;
    private final Clock clock;

    public JobSearchService(LanguageModelRouter languageModelRouter, List<JobProviderService> providers, Clock clock) {
        this.languageModelRouter = languageModelRouter;
        this.providers = providers;
        this.clock = clock;
    }

    public JobSearchResponse search(JobSearchRequest request) {
        JobSearchFilters filters = languageModelRouter.extract(request.query(), request.provider());
        if (request.postedWithinHours() != null) {
            filters = filters.withPostedWithinHours(request.postedWithinHours());
        }
        filters = filters.withDefaults();

        List<JobProviderService> configuredProviders = providers.stream()
                .filter(JobProviderService::isConfigured)
                .toList();
        if (configuredProviders.isEmpty()) {
            throw new JobProviderException(
                    "No job listing sources are configured. Enable Remotive or configure ADZUNA_APP_ID, ADZUNA_APP_KEY, and sources.adzuna.enabled=true.");
        }

        Instant now = clock.instant();
        Instant cutoff = now.minus(filters.postedWithinHours(), ChronoUnit.HOURS);
        Map<String, JobResponse> uniqueListings = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        int successfulProviders = 0;
        for (JobProviderService provider : configuredProviders) {
            try {
                List<JobResponse> listings = provider.search(filters);
                successfulProviders++;
                for (JobResponse listing : listings) {
                    if (isValidRecentListing(listing, filters, cutoff, now)) {
                        String identifier = listing.externalId() == null || listing.externalId().isBlank()
                                ? listing.applicationUrl()
                                : listing.externalId();
                        uniqueListings.putIfAbsent(listing.source().toLowerCase(Locale.ROOT) + ":" + identifier, listing);
                    }
                }
            } catch (JobProviderException exception) {
                warnings.add(provider.name() + " could not be searched: " + exception.getMessage());
                log.warn("Job provider {} failed during live search", provider.name());
            }
        }
        if (successfulProviders == 0) {
            throw new JobProviderException(String.join(" ", warnings));
        }

        return new JobSearchResponse(
                filters,
                uniqueListings.values().stream()
                        .sorted((first, second) -> second.postedAt().compareTo(first.postedAt()))
                        .toList(),
                List.copyOf(warnings));
    }

    private static boolean isValidRecentListing(
            JobResponse job,
            JobSearchFilters filters,
            Instant cutoff,
            Instant now) {
        if (job == null || job.externalId() == null || job.externalId().isBlank()
                || job.title() == null || job.title().isBlank()
                || job.source() == null || job.source().isBlank()
                || !validHttpUrl(job.applicationUrl())
                || job.postedAt() == null
                || job.postedAt().isBefore(cutoff)
                || job.postedAt().isAfter(now)) {
            return false;
        }

        String text = listingText(job);
        if (filters.location() != null && !containsIgnoreCase(job.location(), filters.location())
                && !containsIgnoreCase(job.description(), filters.location())) {
            return false;
        }
        if (filters.domain() != null && !containsIgnoreCase(text, filters.domain())) {
            return false;
        }
        if (filters.workMode() != null && !matchesWorkMode(filters.workMode(), job)) {
            return false;
        }
        if (filters.experienceRequirement() != null
                && !containsIgnoreCase(text, filters.experienceRequirement())) {
            return false;
        }
        if (filters.graduationYear() != null
                && !text.contains(String.valueOf(filters.graduationYear()))) {
            return false;
        }
        if (!filters.companyPreferences().isEmpty()
                && filters.companyPreferences().stream().noneMatch(preference -> containsIgnoreCase(job.company(), preference))) {
            return false;
        }
        return filters.technologyPreferences().stream().allMatch(technology -> containsIgnoreCase(text, technology));
    }

    private static String listingText(JobResponse job) {
        return String.join(" ",
                safe(job.title()),
                safe(job.company()),
                safe(job.description()),
                safe(job.location())).toLowerCase(Locale.ROOT);
    }

    private static boolean matchesWorkMode(JobSearchFilters.WorkMode mode, JobResponse job) {
        if (job.source().equalsIgnoreCase("remotive")) {
            return mode == JobSearchFilters.WorkMode.REMOTE;
        }
        String text = listingText(job);
        boolean remote = text.contains("remote") || text.contains("work from home")
                || text.contains("anywhere");
        return switch (mode) {
            case REMOTE -> remote;
            case ONSITE -> text.contains("onsite") || text.contains("on-site")
                    || text.contains("on site") || text.contains("in-office") || text.contains("in office");
            case HYBRID -> text.contains("hybrid");
        };
    }

    private static boolean containsIgnoreCase(String value, String search) {
        return value != null && search != null
                && value.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean validHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            return uri.isAbsolute() && uri.getHost() != null
                    && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
