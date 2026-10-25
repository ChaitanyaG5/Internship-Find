package com.example.internshiptracker;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.example.internshiptracker.model.Internship;
import com.example.internshiptracker.repository.InternshipRepository;
import com.example.internshiptracker.scheduler.JobFetchScheduler;
import com.example.internshiptracker.service.InternshipService;
import com.example.internshiptracker.source.JobSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "sources.remotive.enabled=false",
        "sources.adzuna.enabled=false",
        "sources.greenhouse.enabled=false",
        "sources.lever.enabled=false",
        "sources.muse.enabled=false",
        "sources.jsearch.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:internship-tests;DB_CLOSE_DELAY=-1"
})
class InternshipTrackerTests {

    @Autowired
    private InternshipRepository repository;

    @Autowired
    private InternshipService internshipService;

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
    }

    @Test
    void searchFiltersByTimeAndAnyKeywordNewestFirst() {
        save(listing("Java Developer Intern", "java", "https://example.com/1", Instant.now().minus(2, ChronoUnit.HOURS)));
        save(listing("Spring Internship", "other", "https://example.com/2", Instant.now().minus(1, ChronoUnit.HOURS)));
        save(listing("Java internship", "old", "https://example.com/3", Instant.now().minus(30, ChronoUnit.HOURS)));

        List<Internship> results = internshipService.search(List.of("java", "spring"), 24, null);

        assertThat(results).extracting(Internship::getUrl)
                .containsExactly("https://example.com/2", "https://example.com/1");
    }

    @Test
    void saveNewDeduplicatesAcrossSourcesAndWithinOneBatch() {
        Internship fromFirstSource = listing("Intern A", "one", "https://example.com/shared", Instant.now());
        Internship duplicateInBatch = listing("Intern A copy", "two", "https://example.com/shared", Instant.now());
        assertThat(internshipService.saveNew(List.of(fromFirstSource))).isEqualTo(1);

        Internship stored = repository.findAllByUrlIn(List.of("https://example.com/shared")).get(0);
        Instant originalPostedAt = stored.getPostedAt();
        stored.setLastCheckedAt(Instant.EPOCH);
        repository.save(stored);
        assertThat(internshipService.saveNew(List.of(duplicateInBatch))).isZero();
        assertThat(repository.count()).isEqualTo(1);
        Internship checked = repository.findAllByUrlIn(List.of("https://example.com/shared")).get(0);
        assertThat(checked.getLastCheckedAt()).isAfter(Instant.EPOCH);
        assertThat(checked.getPostedAt()).isEqualTo(originalPostedAt);
    }

    @Test
    void expirationCleanupDeletesOnlyListingsWithPastApplicationDeadlines() {
        Instant now = Instant.now();
        Internship expired = listing("Expired internship", "example", "https://example.com/expired",
                now.minus(30, ChronoUnit.DAYS));
        expired.setApplicationDeadline(now.minusSeconds(1));
        Internship stillOpen = listing("Open internship", "example", "https://example.com/open",
                now.minus(30, ChronoUnit.DAYS));
        stillOpen.setApplicationDeadline(now.plus(1, ChronoUnit.DAYS));
        Internship noDeadline = listing("Undated internship", "example", "https://example.com/undated",
                now.minus(30, ChronoUnit.DAYS));
        repository.saveAll(List.of(expired, stillOpen, noDeadline));

        assertThat(internshipService.deleteExpiredApplications(now)).isEqualTo(1);
        assertThat(repository.findAll()).extracting(Internship::getUrl)
                .containsExactlyInAnyOrder(
                        "https://example.com/open",
                        "https://example.com/undated");
    }

    @Test
    void searchExcludesListingsWithExpiredApplicationDeadlines() {
        Instant now = Instant.now();
        Internship expired = listing("Expired internship", "example", "https://example.com/expired",
                now.minus(1, ChronoUnit.HOURS));
        expired.setApplicationDeadline(now.minusSeconds(1));
        Internship noDeadline = listing("Open internship", "example", "https://example.com/open",
                now.minus(2, ChronoUnit.HOURS));
        repository.saveAll(List.of(expired, noDeadline));

        assertThat(internshipService.search(List.of(), 24, null))
                .extracting(Internship::getUrl)
                .containsExactly("https://example.com/open");
    }

    @Test
    void schedulerContinuesAfterOneSourceThrows() {
        JobSource broken = new JobSource() {
            @Override
            public String name() {
                return "broken";
            }

            @Override
            public List<Internship> fetch() {
                throw new IllegalStateException("simulated source failure");
            }
        };
        JobSource healthy = new JobSource() {
            @Override
            public String name() {
                return "healthy";
            }

            @Override
            public List<Internship> fetch() {
                return List.of(listing("Healthy internship", "healthy", "https://example.com/healthy", Instant.now()));
            }
        };

        new JobFetchScheduler(List.of(broken, healthy), internshipService).runOnce();

        assertThat(repository.findAllByUrlIn(List.of("https://example.com/healthy"))).hasSize(1);
    }

    private void save(Internship internship) {
        repository.save(internship);
    }

    private static Internship listing(String title, String source, String url, Instant postedAt) {
        Internship internship = new Internship();
        internship.setTitle(title);
        internship.setCompany("Example");
        internship.setLocation("Remote");
        internship.setPostedAt(postedAt);
        internship.setDescription("A useful opportunity");
        internship.setUrl(url);
        internship.setSource(source);
        return internship;
    }
}
