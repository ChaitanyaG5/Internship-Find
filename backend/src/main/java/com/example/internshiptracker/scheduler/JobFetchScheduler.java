package com.example.internshiptracker.scheduler;

import java.util.List;

import com.example.internshiptracker.model.Internship;
import com.example.internshiptracker.service.InternshipService;
import com.example.internshiptracker.source.JobSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobFetchScheduler {

    private final List<JobSource> sources;
    private final InternshipService internshipService;

    @EventListener(ApplicationReadyEvent.class)
    public void fetchAtStartup() {
        runOnce();
    }

    @Scheduled(cron = "${app.fetch.cron:0 0 */6 * * *}", zone = "Asia/Kolkata")
    public void fetchOnSchedule() {
        runOnce();
    }

    public void runOnce() {
        for (JobSource source : sources) {
            long startedAt = System.nanoTime();
            int fetchedCount = 0;
            try {
                List<Internship> listings = source.fetch();
                fetchedCount = listings.size();
                int newCount = internshipService.saveNew(listings);
                log.info("Source {} fetched {} jobs, {} new, in {} ms",
                        source.name(), fetchedCount, newCount, elapsedMillis(startedAt));
            } catch (Exception exception) {
                log.error("Source {} failed after fetching {} jobs in {} ms; continuing with remaining sources",
                        source.name(), fetchedCount, elapsedMillis(startedAt), exception);
            }
        }
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
