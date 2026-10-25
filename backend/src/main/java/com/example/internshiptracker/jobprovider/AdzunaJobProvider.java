package com.example.internshiptracker.jobprovider;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.internshiptracker.search.JobSearchFilters;
import com.fasterxml.jackson.databind.JsonNode;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AdzunaJobProvider implements JobProviderService {

    private final RestClient restClient;
    private final String appId;
    private final String appKey;
    private final String country;
    private final boolean enabled;

    public AdzunaJobProvider(
            RestClient.Builder restClientBuilder,
            @Value("${ADZUNA_APP_ID:}") String appId,
            @Value("${ADZUNA_APP_KEY:}") String appKey,
            @Value("${sources.adzuna.country:in}") String country,
            @Value("${sources.adzuna.enabled:false}") boolean enabled) {
        this.restClient = restClientBuilder.build();
        this.appId = appId;
        this.appKey = appKey;
        this.country = country;
        this.enabled = enabled;
    }

    @Override
    public String name() {
        return "adzuna";
    }

    @Override
    public boolean isConfigured() {
        return enabled && !appId.isBlank() && !appKey.isBlank();
    }

    @Override
    public List<JobResponse> search(JobSearchFilters filters) {
        String terms = searchTerms(filters);
        int maxDaysOld = Math.max(1, (filters.postedWithinHours() + 23) / 24);
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("https://api.adzuna.com/v1/api/jobs/{country}/search/1")
                .queryParam("app_id", appId)
                .queryParam("app_key", appKey)
                .queryParam("results_per_page", 50)
                .queryParam("max_days_old", maxDaysOld)
                .queryParam("what", terms);
        if (filters.location() != null) {
            builder.queryParam("where", filters.location());
        }
        URI uri = builder.buildAndExpand(country).encode().toUri();
        try {
            JsonNode response = restClient.get().uri(uri).retrieve().body(JsonNode.class);
            JsonNode jobs = response == null ? null : response.path("results");
            if (jobs == null || !jobs.isArray()) {
                return List.of();
            }
            List<JobResponse> results = new ArrayList<>();
            jobs.forEach(job -> map(job).ifPresent(results::add));
            return results;
        } catch (RestClientException exception) {
            throw new JobProviderException("The Adzuna job source is temporarily unavailable.", exception);
        }
    }

    private static java.util.Optional<JobResponse> map(JsonNode job) {
        String id = job.path("id").asText("");
        String url = validHttpUrl(job.path("redirect_url").asText(null));
        String title = trimToNull(job.path("title").asText(null));
        if (id.isBlank() || url == null || title == null) {
            return java.util.Optional.empty();
        }
        String description = job.path("description").asText("");
        if (!description.isBlank()) {
            description = Jsoup.clean(description, Safelist.none()).trim();
            if (description.length() > 10_000) {
                description = description.substring(0, 10_000);
            }
        }
        return java.util.Optional.of(new JobResponse(
                id,
                title,
                trimToNull(job.path("company").path("display_name").asText(null)),
                description.isBlank() ? null : description,
                trimToNull(job.path("location").path("display_name").asText(null)),
                parseDate(job.path("created").asText(null)),
                "adzuna",
                url));
    }

    private static String searchTerms(JobSearchFilters filters) {
        List<String> terms = new ArrayList<>();
        terms.add(filters.keywords());
        if (filters.domain() != null) {
            terms.add(filters.domain());
        }
        terms.addAll(filters.technologyPreferences());
        if (filters.experienceRequirement() != null) {
            terms.add(filters.experienceRequirement());
        }
        if (filters.graduationYear() != null) {
            terms.add(String.valueOf(filters.graduationYear()));
        }
        terms.addAll(filters.companyPreferences());
        if (filters.workMode() == JobSearchFilters.WorkMode.REMOTE) {
            terms.add("remote");
        }
        terms.add("internship");
        return String.join(" ", terms).toLowerCase(Locale.ROOT);
    }

    private static Instant parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static String validHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            java.net.URI uri = java.net.URI.create(value);
            String scheme = uri.getScheme();
            return uri.isAbsolute() && uri.getHost() != null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    ? uri.toString()
                    : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
