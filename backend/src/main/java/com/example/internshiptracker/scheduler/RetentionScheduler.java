package com.example.internshiptracker.scheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.example.internshiptracker.service.InternshipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetentionScheduler {

    private final InternshipService internshipService;

    @Value("${app.retention.days:7}")
    private int retentionDays;

    @Scheduled(cron = "${app.retention.cron:0 0 2 * * *}", zone = "Asia/Kolkata")
    public void deleteExpiredListings() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = internshipService.deleteOlderThan(cutoff);
        log.info("Retention cleanup removed {} listings older than {} days", deleted, retentionDays);
    }
}
