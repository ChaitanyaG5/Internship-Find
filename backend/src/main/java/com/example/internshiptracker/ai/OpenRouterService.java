package com.example.internshiptracker.ai;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.internshiptracker.search.JobSearchFilters;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class OpenRouterService implements LanguageModelService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final String apiKey;
    private final String model;

    public OpenRouterService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            Validator validator,
            @Value("${OPENROUTER_API_KEY:}") String apiKey,
            @Value("${app.ai.openrouter.model:openai/gpt-4o-mini}") String model) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String provider() {
        return "OPENROUTER";
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    @Override
    public JobSearchFilters extractFilters(String query) {
        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", FilterExtractionPrompt.INSTRUCTIONS),
                        Map.of("role", "user", "content", query)),
                "tools", List.of(Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", "extract_internship_search_filters",
                                "description", "Extract structured filters for searching real internship listings.",
                                "parameters", SearchFilterSchema.schema()))),
                "tool_choice", Map.of(
                        "type", "function",
                        "function", Map.of("name", "extract_internship_search_filters")));
        try {
            JsonNode response = restClient.post()
                    .uri("https://openrouter.ai/api/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("X-Title", "Internship Tracker")
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode arguments = response == null ? null
                    : response.path("choices").path(0).path("message").path("tool_calls").path(0)
                            .path("function").path("arguments");
            if (arguments == null || arguments.isMissingNode() || arguments.isNull()) {
                throw new LanguageModelException("OpenRouter returned no structured search filters.");
            }
            String content = arguments.isTextual() ? arguments.asText() : arguments.toString();
            return validate(objectMapper.readValue(content, JobSearchFilters.class));
        } catch (RestClientResponseException exception) {
            String providerMessage = null;
            try {
                JsonNode error = objectMapper.readTree(exception.getResponseBodyAsString()).path("error").path("message");
                if (!error.isMissingNode() && !error.isNull() && !error.asText().isBlank()) {
                    providerMessage = error.asText();
                }
            } catch (java.io.IOException ignored) {
                // Fall back to the HTTP status when the provider response is not JSON.
            }
            String details = providerMessage == null ? exception.getStatusText() : providerMessage;
            throw new LanguageModelException(
                    "OpenRouter rejected the request (HTTP " + exception.getStatusCode().value() + "): " + details,
                    exception);
        } catch (RestClientException exception) {
            throw new LanguageModelException(
                    "Could not reach OpenRouter. Check network connectivity and provider availability.",
                    exception);
        } catch (java.io.IOException exception) {
            throw new LanguageModelException("OpenRouter returned invalid structured search filters.", exception);
        }
    }

    private JobSearchFilters validate(JobSearchFilters filters) {
        if (filters == null) {
            throw new LanguageModelException("OpenRouter returned an empty structured search filter.");
        }
        Set<ConstraintViolation<JobSearchFilters>> violations = validator.validate(filters);
        if (!violations.isEmpty()) {
            throw new LanguageModelException("OpenRouter returned search filters that failed validation.");
        }
        return filters;
    }
}
