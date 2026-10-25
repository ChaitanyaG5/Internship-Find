package com.example.internshiptracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.example.internshiptracker.ai.LanguageModelRouter;
import com.example.internshiptracker.api.JobSearchRequest;
import com.example.internshiptracker.jobprovider.JobProviderException;
import com.example.internshiptracker.jobprovider.JobProviderService;
import com.example.internshiptracker.jobprovider.JobResponse;
import com.example.internshiptracker.search.JobSearchFilters;
import org.junit.jupiter.api.Test;

class JobSearchServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");
    private static final JobSearchFilters FILTERS =
            new JobSearchFilters("Java internship", null, null, null, null, null, 24, List.of(), List.of());

    @Test
    void onlyReturnsOriginalPostingsWithinTwentyFourHours() {
        LanguageModelRouter router = routerReturning(FILTERS);
        JobProviderService provider = provider("remotive", true, List.of(
                job("new", NOW.minusSeconds(23 * 3600L), "https://jobs.example/new"),
                job("boundary", NOW.minusSeconds(24 * 3600L), "https://jobs.example/boundary"),
                job("old", NOW.minusSeconds(25 * 3600L), "https://jobs.example/old"),
                job("unknown-date", null, "https://jobs.example/unknown"),
                job("invalid-link", NOW.minusSeconds(60), "javascript:alert(1)")));

        var response = service(router, provider).search(request(24));

        assertThat(response.jobs()).extracting(JobResponse::externalId).containsExactly("new", "boundary");
    }

    @Test
    void deduplicatesByProviderAndExternalJobId() {
        LanguageModelRouter router = routerReturning(FILTERS);
        JobProviderService first = provider("remotive", true,
                List.of(job("same-id", NOW.minusSeconds(60), "https://jobs.example/one")));
        JobProviderService duplicate = provider("remotive", true,
                List.of(job("same-id", NOW.minusSeconds(30), "https://jobs.example/two")));

        var response = service(router, first, duplicate).search(request(24));

        assertThat(response.jobs()).hasSize(1);
        assertThat(response.jobs().get(0).applicationUrl()).isEqualTo("https://jobs.example/one");
    }

    @Test
    void workModeFiltersDoNotTreatUnknownAsOnsite() {
        JobSearchFilters remoteFilters =
                new JobSearchFilters("Software internship", null, null, JobSearchFilters.WorkMode.REMOTE,
                        null, null, 24, List.of(), List.of());
        JobProviderService provider = provider("adzuna", true, List.of(
                new JobResponse("remote", "Remote Software Intern", "Company", null, "India",
                        NOW.minusSeconds(60), "adzuna", "https://jobs.example/remote"),
                new JobResponse("unknown", "Software Intern", "Company", null, "India",
                        NOW.minusSeconds(60), "adzuna", "https://jobs.example/unknown"),
                new JobResponse("onsite", "On-site Software Intern", "Company", null, "India",
                        NOW.minusSeconds(60), "adzuna", "https://jobs.example/onsite")));

        var response = service(routerReturning(remoteFilters), provider).search(request(24));

        assertThat(response.jobs()).extracting(JobResponse::externalId).containsExactly("remote");
    }

    @Test
    void reportsProviderFailuresAlongsideSuccessfulResults() {
        LanguageModelRouter router = routerReturning(FILTERS);
        JobProviderService failed = new JobProviderService() {
            @Override
            public String name() {
                return "failed";
            }

            @Override
            public boolean isConfigured() {
                return true;
            }

            @Override
            public List<JobResponse> search(JobSearchFilters filters) {
                throw new JobProviderException("unavailable");
            }
        };
        JobProviderService working = provider("remotive", true,
                List.of(job("one", NOW.minusSeconds(60), "https://jobs.example/one")));

        var response = service(router, failed, working).search(request(24));

        assertThat(response.jobs()).hasSize(1);
        assertThat(response.warnings()).containsExactly("failed could not be searched: unavailable");
    }

    @Test
    void failsClearlyWhenNoListingSourceIsConfigured() {
        LanguageModelRouter router = routerReturning(FILTERS);
        JobProviderService disabled = provider("remotive", false, List.of());

        assertThatThrownBy(() -> service(router, disabled).search(request(24)))
                .isInstanceOf(JobProviderException.class)
                .hasMessageContaining("No job listing sources are configured");
    }

    @Test
    void reportsWhenEveryConfiguredListingSourceFails() {
        LanguageModelRouter router = routerReturning(FILTERS);
        JobProviderService failed = new JobProviderService() {
            @Override
            public String name() {
                return "adzuna";
            }

            @Override
            public boolean isConfigured() {
                return true;
            }

            @Override
            public List<JobResponse> search(JobSearchFilters filters) {
                throw new JobProviderException("provider unavailable");
            }
        };

        assertThatThrownBy(() -> service(router, failed).search(request(24)))
                .isInstanceOf(JobProviderException.class)
                .hasMessageContaining("adzuna could not be searched");
    }

    private static JobSearchService service(LanguageModelRouter router, JobProviderService... providers) {
        return new JobSearchService(
                router,
                List.of(providers),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static LanguageModelRouter routerReturning(JobSearchFilters filters) {
        return new LanguageModelRouter(List.of(new com.example.internshiptracker.ai.LanguageModelService() {
            @Override
            public String provider() {
                return "OPENROUTER";
            }

            @Override
            public boolean isConfigured() {
                return true;
            }

            @Override
            public JobSearchFilters extractFilters(String query) {
                return filters;
            }
        }), "OPENROUTER");
    }

    private static JobProviderService provider(String name, boolean configured, List<JobResponse> jobs) {
        return new JobProviderService() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public boolean isConfigured() {
                return configured;
            }

            @Override
            public List<JobResponse> search(JobSearchFilters filters) {
                return jobs;
            }
        };
    }

    private static JobSearchRequest request(Integer hours) {
        return new JobSearchRequest("Java internships", "OPENROUTER", hours);
    }

    private static JobResponse job(String id, Instant postedAt, String url) {
        return new JobResponse(id, "Java Intern", "Company", "Java development", "Remote", postedAt, "remotive", url);
    }
}
