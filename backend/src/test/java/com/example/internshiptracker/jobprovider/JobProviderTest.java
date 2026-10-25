package com.example.internshiptracker.jobprovider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.internshiptracker.search.JobSearchFilters;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class JobProviderTest {

    @Test
    void remotiveResponseIsNormalizedWithSourceIdAndOriginalDate() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://remotive.com/api/remote-jobs?search=")))
                .andRespond(withSuccess("""
                        {"jobs":[{"id":12345,"title":"Java Backend Intern","company_name":"Actual Co","candidate_required_location":"India","publication_date":"2026-10-23T10:00:00Z","description":"<p>Build services</p>","url":"https://jobs.example/apply/12345"}]}
                        """, MediaType.APPLICATION_JSON));
        RemotiveJobProvider provider = new RemotiveJobProvider(builder, true);

        var jobs = provider.search(filters());

        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).externalId()).isEqualTo("12345");
        assertThat(jobs.get(0).source()).isEqualTo("remotive");
        assertThat(jobs.get(0).description()).isEqualTo("Build services");
        assertThat(jobs.get(0).postedAt()).isEqualTo(java.time.Instant.parse("2026-10-23T10:00:00Z"));
        server.verify();
    }

    @Test
    void adzunaResponseIsNormalizedWithSourceIdAndOriginalDate() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://api.adzuna.com/v1/api/jobs/in/search/1?")))
                .andRespond(withSuccess("""
                        {"results":[{"id":"adz-234","title":"Java Intern","company":{"display_name":"Real Company"},"location":{"display_name":"India"},"created":"2026-10-23T10:00:00Z","description":"Spring development","redirect_url":"https://jobs.example/apply/adz-234"}]}
                        """, MediaType.APPLICATION_JSON));
        AdzunaJobProvider provider = new AdzunaJobProvider(builder, "id", "key", "in", true);

        var jobs = provider.search(filters());

        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).externalId()).isEqualTo("adz-234");
        assertThat(jobs.get(0).source()).isEqualTo("adzuna");
        assertThat(jobs.get(0).company()).isEqualTo("Real Company");
        server.verify();
    }

    @Test
    void invalidOriginalPostingTimestampIsNotReplacedWithAnotherDate() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://remotive.com/api/remote-jobs?search=")))
                .andRespond(withSuccess("""
                        {"jobs":[{"id":88,"title":"Intern","publication_date":"unknown","url":"https://jobs.example/88"}]}
                        """, MediaType.APPLICATION_JSON));
        RemotiveJobProvider provider = new RemotiveJobProvider(builder, true);

        var jobs = provider.search(filters());

        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).postedAt()).isNull();
        server.verify();
    }

    private static JobSearchFilters filters() {
        return new JobSearchFilters("Java internship", null, null, null, null, null, 24, java.util.List.of(), java.util.List.of());
    }
}
