package com.example.internshiptracker.jobprovider;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
public class RemotiveJobProvider implements JobProviderService {

    private final RestClient restClient;
    private final boolean enabled;

    public RemotiveJobProvider(
            RestClient.Builder restClientBuilder,
            @Value("${sources.remotive.enabled:true}") boolean enabled) {
        this.restClient = restClientBuilder.build();
        this.enabled = enabled;
    }

    @Override
    public String name() {
        return "remotive";
    }

    @Override
    public boolean isConfigured() {
        return enabled;
    }

    @Override
    public List<JobResponse> search(JobSearchFilters filters) {
        String query = searchTerms(filters);
        URI uri = UriComponentsBuilder.fromUriString("https://remotive.com/api/remote-jobs")
                .queryParam("search", query)
                .build()
                .encode()
                .toUri();
        try {
            JsonNode response = restClient.get().uri(uri).retrieve().body(JsonNode.class);
            JsonNode jobs = response == null ? null : response.path("jobs");
            if (jobs == null || !jobs.isArray()) {
                return List.of();
            }
            List<JobResponse> results = new ArrayList<>();
            jobs.forEach(job -> map(job).ifPresent(results::add));
            return results;
        } catch (RestClientException exception) {
            throw new JobProviderException("The Remotive job source is temporarily unavailable.", exception);
        }
    }

    private static java.util.Optional<JobResponse> map(JsonNode job) {
        String id = job.path("id").asText("");
        String url = validHttpUrl(job.path("url").asText(null));
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
        String date = job.path("publication_date").asText(null);
        return java.util.Optional.of(new JobResponse(
                id,
                title,
                trimToNull(job.path("company_name").asText(null)),
                description.isBlank() ? null : description,
                trimToNull(job.path("candidate_required_location").asText(null)),
                parsePublicationDate(date),
                "remotive",
                url));
    }

    private static String searchTerms(JobSearchFilters filters) {
        List<String> terms = new ArrayList<>();
        terms.add(filters.keywords());
        if (filters.domain() != null) {
            terms.add(filters.domain());
        }
        terms.addAll(filters.technologyPreferences());
        if (filters.location() != null && !filters.location().isBlank()
                && !filters.location().equalsIgnoreCase("remote")) {
            terms.add(filters.location());
        }
        terms.addAll(filters.companyPreferences());
        if (filters.experienceRequirement() != null) {
            terms.add(filters.experienceRequirement());
        }
        if (filters.graduationYear() != null) {
            terms.add(String.valueOf(filters.graduationYear()));
        }
        return String.join(" ", terms).toLowerCase(Locale.ROOT);
    }

    static Instant parsePublicationDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException exception) {
            try {
                return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private static String validHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(value);
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
