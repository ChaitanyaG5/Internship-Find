package com.example.internshiptracker.source;

import java.util.List;

import com.example.internshiptracker.model.Internship;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "sources.muse.enabled", havingValue = "true")
public class MuseSource implements JobSource {

    private final RestClient restClient;

    public MuseSource(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String name() {
        return "muse";
    }

    @Override
    public List<Internship> fetch() {
        JsonNode response = restClient.get()
                .uri("https://www.themuse.com/api/public/jobs?page=0")
                .retrieve()
                .body(JsonNode.class);
        // TODO: Map The Muse response after verifying the current public API schema.
        return List.of();
    }
}
