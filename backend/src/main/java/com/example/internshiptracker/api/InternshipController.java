package com.example.internshiptracker.api;

import java.util.Arrays;
import java.util.List;

import com.example.internshiptracker.service.InternshipService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internships")
@RequiredArgsConstructor
@Validated
public class InternshipController {

    private final InternshipService internshipService;

    @GetMapping
    public List<InternshipDto> search(
            @RequestParam(required = false) List<String> keywords,
            @RequestParam(defaultValue = "24") @Min(1) @Max(168) int hours,
            @RequestParam(required = false) String source) {
        List<String> parsedKeywords = keywords == null ? List.of() : keywords.stream()
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        return internshipService.search(parsedKeywords, hours, source).stream()
                .map(InternshipDto::from)
                .toList();
    }
}
