package com.example.internshiptracker.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.internshiptracker.search.JobSearchFilters;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class LanguageModelIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final jakarta.validation.Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void openRouterExtractsStructuredFilters() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"tool_calls":[{"function":{"arguments":"{\\"keywords\\":\\"Java internship\\",\\"domain\\":\\"backend\\",\\"location\\":\\"India\\",\\"workMode\\":\\"REMOTE\\",\\"experienceRequirement\\":null,\\"graduationYear\\":2027,\\"postedWithinHours\\":24,\\"companyPreferences\\":[],\\"technologyPreferences\\":[\\"Java\\"]}"}}]}}]}
                        """, MediaType.APPLICATION_JSON));
        OpenRouterService service = new OpenRouterService(builder, objectMapper, validator, "test-key", "test-model");

        JobSearchFilters filters = service.extractFilters("Remote Java internship in India");

        assertThat(filters.keywords()).isEqualTo("Java internship");
        assertThat(filters.workMode()).isEqualTo(JobSearchFilters.WorkMode.REMOTE);
        assertThat(filters.graduationYear()).isEqualTo(2027);
        server.verify();
    }

    @Test
    void openRouterReportsProviderRejectionDetails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":{"message":"Invalid API key"}}
                                """));
        OpenRouterService service = new OpenRouterService(builder, objectMapper, validator, "invalid-test-key", "test-model");

        assertThatThrownBy(() -> service.extractFilters("Java internship"))
                .isInstanceOf(LanguageModelException.class)
                .hasMessageContaining("HTTP 401")
                .hasMessageContaining("Invalid API key");
        server.verify();
    }

    @Test
    void autoUsesConfiguredOpenRouterProvider() {
        OpenRouterService provider = new OpenRouterService(
                RestClient.builder(), objectMapper, validator, "test-key", "test-model");
        LanguageModelRouter router = new LanguageModelRouter(java.util.List.of(provider), "OPENROUTER");

        assertThat(provider.provider()).isEqualTo("OPENROUTER");
        assertThat(provider.isConfigured()).isTrue();
        assertThatThrownBy(() -> router.extract("Java internship", "CLAUDE"))
                .isInstanceOf(LanguageModelException.class)
                .hasMessageContaining("Choose OPENROUTER or AUTO");
    }
}
