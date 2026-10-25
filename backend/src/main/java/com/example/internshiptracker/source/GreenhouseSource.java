package com.example.internshiptracker.source;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.internshiptracker.model.Internship;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "sources.greenhouse.enabled", havingValue = "true")
public class GreenhouseSource implements JobSource {

    private final RestClient restClient;
    private final List<String> companies;

    public GreenhouseSource(
            RestClient.Builder restClientBuilder,
            @Value("${sources.greenhouse.companies:}") String companySlugs) {
        this.restClient = restClientBuilder.build();
        this.companies = parseSlugs(companySlugs);
    }

    @Override
    public String name() {
        return "greenhouse";
    }

    @Override
    public List<Internship> fetch() {
        List<Internship> listings = new ArrayList<>();
        for (String company : companies) {
            JsonNode response = restClient.get()
                    .uri("https://boards-api.greenhouse.io/v1/boards/{company}/jobs?content=true", company)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode jobs = response == null ? null : response.path("jobs");
            if (jobs != null && jobs.isArray()) {
                jobs.forEach(job -> mapListing(job, company).ifPresent(listings::add));
            }
        }
        return listings;
    }

    private java.util.Optional<Internship> mapListing(JsonNode job, String company) {
        // TODO: Confirm any provider schema changes before extending this public-board mapping.
        String title = job.path("title").asText("");
        if (!title.toLowerCase(Locale.ROOT).contains("intern")) {
            return java.util.Optional.empty();
        }
        String url = job.path("absolute_url").asText("");
        String postedAt = job.path("first_published").asText("");
        if (url.isBlank() || postedAt.isBlank()) {
            return java.util.Optional.empty();
        }
        Internship internship = new Internship();
        internship.setTitle(title);
        internship.setCompany(company);
        internship.setLocation(job.path("location").path("name").asText(""));
        internship.setPostedAt(Instant.parse(postedAt));
        internship.setDescription(DescriptionSanitizer.plainText(job.path("content").asText("")));
        internship.setUrl(url);
        internship.setSource(name());
        return java.util.Optional.of(internship);
    }

    private static List<String> parseSlugs(String configured) {
        if (configured == null || configured.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(slug -> !slug.isEmpty())
                .toList();
    }
}
