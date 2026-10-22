package com.example.internshiptracker.source;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import com.example.internshiptracker.model.Internship;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(name = "sources.adzuna.enabled", havingValue = "true")
public class AdzunaSource implements JobSource {

    private final RestClient restClient;
    private final String appId;
    private final String appKey;

    public AdzunaSource(
            RestClient.Builder restClientBuilder,
            @Value("${ADZUNA_APP_ID:}") String appId,
            @Value("${ADZUNA_APP_KEY:}") String appKey) {
        this.restClient = restClientBuilder.build();
        this.appId = appId;
        this.appKey = appKey;
    }

    @Override
    public String name() {
        return "adzuna";
    }

    @Override
    public List<Internship> fetch() {
        if (appId.isBlank() || appKey.isBlank()) {
            throw new IllegalStateException("ADZUNA_APP_ID and ADZUNA_APP_KEY are required when Adzuna is enabled");
        }
        var uri = UriComponentsBuilder
                .fromUriString("https://api.adzuna.com/v1/api/jobs/in/search/1")
                .queryParam("app_id", appId)
                .queryParam("app_key", appKey)
                .queryParam("results_per_page", 50)
                .queryParam("max_days_old", 1)
                .queryParam("what", "intern")
                .build()
                .encode()
                .toUri();

        AdzunaResponse response = restClient.get().uri(uri).retrieve().body(AdzunaResponse.class);
        if (response == null || response.results() == null) {
            return List.of();
        }
        return response.results().stream().map(this::toInternship).toList();
    }

    private Internship toInternship(AdzunaJob job) {
        Internship internship = new Internship();
        internship.setTitle(job.title());
        internship.setCompany(job.company() == null ? null : job.company().displayName());
        internship.setLocation(job.location() == null ? null : job.location().displayName());
        internship.setPostedAt(OffsetDateTime.parse(job.created()).toInstant());
        internship.setDescription(DescriptionSanitizer.plainText(job.description()));
        internship.setUrl(job.redirectUrl());
        internship.setSource(name());
        return internship;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AdzunaResponse(List<AdzunaJob> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AdzunaJob(
            String title,
            AdzunaCompany company,
            AdzunaLocation location,
            String created,
            String description,
            @JsonProperty("redirect_url") String redirectUrl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AdzunaCompany(@JsonProperty("display_name") String displayName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AdzunaLocation(@JsonProperty("display_name") String displayName) {
    }
}
