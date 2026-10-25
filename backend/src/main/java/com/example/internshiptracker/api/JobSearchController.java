package com.example.internshiptracker.api;

import com.example.internshiptracker.service.JobSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobSearchController {

    private final JobSearchService jobSearchService;

    @PostMapping("/search")
    public JobSearchResponse search(@Valid @RequestBody JobSearchRequest request) {
        return jobSearchService.search(request);
    }
}
