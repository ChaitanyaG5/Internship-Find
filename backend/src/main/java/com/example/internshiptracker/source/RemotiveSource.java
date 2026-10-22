package com.example.internshiptracker.source;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.example.internshiptracker.model.Internship;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "sources.remotive.enabled", havingValue = "true")
public class RemotiveSource implements JobSource {

    private static final String API_URL = "https://remotive.com/api/remote-jobs?search=intern";
    private final RestClient restClient;

    public RemotiveSource(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String name() {
        return "remotive";
    }

    @Override
    public List<Internship> fetch() {
        RemotiveResponse response = restClient.get().uri(API_URL).retrieve().body(RemotiveResponse.class);
        if (response == null || response.jobs() == null) {
            return List.of();
        }
        return response.jobs().stream().map(this::toInternship).toList();
    }

    private Internship toInternship(RemotiveJob job) {
        Internship internship = new Internship();
        internship.setTitle(job.title());
        internship.setCompany(job.companyName());
        internship.setLocation(job.candidateRequiredLocation());
        internship.setPostedAt(parsePublicationDate(job.publicationDate()));
        internship.setDescription(DescriptionSanitizer.plainText(job.description()));
        internship.setUrl(job.url());
        internship.setSource(name());
        return internship;
    }

    static java.time.Instant parsePublicationDate(String publicationDate) {
        try {
            return OffsetDateTime.parse(publicationDate).toInstant();
        } catch (DateTimeParseException exception) {
            return LocalDateTime.parse(publicationDate).toInstant(ZoneOffset.UTC);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemotiveResponse(List<RemotiveJob> jobs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemotiveJob(
            String title,
            @JsonProperty("company_name") String companyName,
            @JsonProperty("candidate_required_location") String candidateRequiredLocation,
            @JsonProperty("publication_date") String publicationDate,
            String description,
            String url) {
    }
}
