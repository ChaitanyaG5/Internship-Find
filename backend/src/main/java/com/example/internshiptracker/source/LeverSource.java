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
@ConditionalOnProperty(name = "sources.lever.enabled", havingValue = "true")
public class LeverSource implements JobSource {

    private final RestClient restClient;
    private final List<String> companies;

    public LeverSource(
            RestClient.Builder restClientBuilder,
            @Value("${sources.lever.companies:}") String companySlugs) {
        this.restClient = restClientBuilder.build();
        this.companies = java.util.Arrays.stream(companySlugs.split(","))
                .map(String::trim)
                .filter(slug -> !slug.isEmpty())
                .toList();
    }

    @Override
    public String name() {
        return "lever";
    }

    @Override
    public List<Internship> fetch() {
        List<Internship> listings = new ArrayList<>();
        for (String company : companies) {
            JsonNode response = restClient.get()
                    .uri("https://api.lever.co/v0/postings/{company}?mode=json", company)
                    .retrieve()
                    .body(JsonNode.class);
            if (response != null && response.isArray()) {
                response.forEach(job -> mapListing(job, company).ifPresent(listings::add));
            }
        }
        return listings;
    }

    private java.util.Optional<Internship> mapListing(JsonNode job, String company) {
        // TODO: Confirm the public postings schema before adding more Lever-specific fields.
        String title = job.path("text").asText("");
        if (!title.toLowerCase(Locale.ROOT).contains("intern")) {
            return java.util.Optional.empty();
        }
        String url = job.path("hostedUrl").asText("");
        long createdAt = job.path("createdAt").asLong(0);
        if (url.isBlank() || createdAt <= 0) {
            return java.util.Optional.empty();
        }
        Internship internship = new Internship();
        internship.setTitle(title);
        internship.setCompany(company);
        internship.setLocation(job.path("categories").path("location").asText(""));
        internship.setPostedAt(Instant.ofEpochMilli(createdAt));
        internship.setDescription(DescriptionSanitizer.plainText(job.path("descriptionPlain").asText("")));
        internship.setUrl(url);
        internship.setSource(name());
        return java.util.Optional.of(internship);
    }
}
