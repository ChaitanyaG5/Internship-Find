package com.example.internshiptracker.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.Clock;
import java.util.List;

import com.example.internshiptracker.jobprovider.JobProviderException;
import com.example.internshiptracker.jobprovider.JobResponse;
import com.example.internshiptracker.search.JobSearchFilters;
import com.example.internshiptracker.service.JobSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class JobSearchControllerTest {

    private JobSearchService service;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        service = new TestJobSearchService();
        configureMvc();
    }

    @Test
    void searchEndpointReturnsExtractedFiltersAndRealProviderListings() throws Exception {
        JobSearchFilters filters = new JobSearchFilters(
                "Java internship", "backend", "India", null, null, null, 24, List.of(), List.of());
        JobResponse job = new JobResponse(
                "id-1", "Java Intern", "Actual Company", "Build systems", "India",
                Instant.parse("2026-10-23T10:00:00Z"), "remotive", "https://jobs.example/apply");
        ((TestJobSearchService) service).response = new JobSearchResponse(filters, List.of(job), List.of());

        mockMvc.perform(post("/api/jobs/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new JobSearchRequest("Java internships in India", "OPENROUTER", 24))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searchFilters.keywords").value("Java internship"))
                .andExpect(jsonPath("$.jobs[0].externalId").value("id-1"))
                .andExpect(jsonPath("$.jobs[0].applicationUrl").value("https://jobs.example/apply"))
                .andExpect(jsonPath("$.jobs[0].source").value("remotive"));
    }

    @Test
    void searchEndpointReturnsClearProviderConfigurationError() throws Exception {
        ((TestJobSearchService) service).failure =
                new JobProviderException("No job listing sources are configured.");

        mockMvc.perform(post("/api/jobs/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"Java internship","provider":"AUTO","postedWithinHours":24}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("No job listing sources are configured."));
    }

    @Test
    void searchEndpointRejectsBlankQueries() throws Exception {
        mockMvc.perform(post("/api/jobs/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"   ","provider":"AUTO"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private void configureMvc() {
        mockMvc = MockMvcBuilders.standaloneSetup(new JobSearchController(service))
                .setControllerAdvice(new JobSearchExceptionHandler())
                .build();
    }

    private static final class TestJobSearchService extends JobSearchService {
        private JobSearchResponse response;
        private RuntimeException failure;

        private TestJobSearchService() {
            super(null, List.of(), Clock.systemUTC());
        }

        @Override
        public JobSearchResponse search(JobSearchRequest request) {
            if (failure != null) {
                throw failure;
            }
            return response;
        }
    }
}
