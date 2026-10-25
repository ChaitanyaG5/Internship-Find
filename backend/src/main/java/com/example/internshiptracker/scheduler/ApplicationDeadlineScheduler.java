package com.example.internshiptracker.scheduler;

import java.time.Instant;

import com.example.internshiptracker.service.InternshipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationDeadlineScheduler {

    private final InternshipService internshipService;

    @Scheduled(cron = "${app.expiration.cron:0 0 2 * * *}", zone = "Asia/Kolkata")
    public void deleteExpiredListings() {
        int deleted = internshipService.deleteExpiredApplications(Instant.now());
        log.info("Application deadline cleanup removed {} expired listings", deleted);
    }
}
