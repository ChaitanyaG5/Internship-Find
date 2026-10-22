package com.example.internshiptracker.source;

import java.util.List;

import com.example.internshiptracker.model.Internship;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "sources.jsearch.enabled", havingValue = "true")
public class JSearchSource implements JobSource {

    private final RestClient restClient;
    private final String apiKey;

    public JSearchSource(RestClient.Builder restClientBuilder, @Value("${RAPIDAPI_KEY:}") String apiKey) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
    }

    @Override
    public String name() {
        return "jsearch";
    }

    @Override
    public List<Internship> fetch() {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("RAPIDAPI_KEY is required when JSearch is enabled");
        }
        JsonNode response = restClient.get()
                .uri("https://jsearch.p.rapidapi.com/search?query=internship%20in%20India&page=1&num_pages=1")
                .header("X-RapidAPI-Key", apiKey)
                .header("X-RapidAPI-Host", "jsearch.p.rapidapi.com")
                .retrieve()
                .body(JsonNode.class);
        // TODO: Map JSearch response after verifying the current RapidAPI schema and plan limits.
        return List.of();
    }
}
