package com.example.internshiptracker.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.internshiptracker.model.Internship;
import com.example.internshiptracker.repository.InternshipRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternshipService {

    private final InternshipRepository repository;

    @Transactional
    public int saveNew(List<Internship> fetched) {
        Map<String, Internship> uniqueFetched = new LinkedHashMap<>();
        for (Internship internship : fetched) {
            if (internship.getUrl() == null || internship.getUrl().isBlank()) {
                log.warn("Skipping {} listing without an apply URL", internship.getSource());
                continue;
            }
            uniqueFetched.putIfAbsent(internship.getUrl(), internship);
        }
        if (uniqueFetched.isEmpty()) {
            return 0;
        }

        Set<String> existingUrls = repository.findAllByUrlIn(uniqueFetched.keySet()).stream()
                .map(Internship::getUrl)
                .collect(Collectors.toSet());
        List<Internship> newListings = uniqueFetched.entrySet().stream()
                .filter(entry -> !existingUrls.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        repository.saveAll(newListings);
        return newListings.size();
    }

    @Transactional(readOnly = true)
    public List<Internship> search(List<String> keywords, int hours, String source) {
        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<String> cleanedKeywords = keywords == null ? List.of() : keywords.stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .toList();

        Specification<Internship> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(criteriaBuilder.greaterThanOrEqualTo(root.get("postedAt"), cutoff));
            if (source != null && !source.isBlank()) {
                conditions.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("source")), source.trim().toLowerCase(Locale.ROOT)));
            }
            if (!cleanedKeywords.isEmpty()) {
                List<Predicate> keywordMatches = cleanedKeywords.stream()
                        .map(keyword -> {
                            String pattern = "%" + escapeLike(keyword) + "%";
                            return criteriaBuilder.or(
                                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern, '\\'),
                                    criteriaBuilder.like(
                                            criteriaBuilder.lower(root.get("description")), pattern, '\\'));
                        })
                        .toList();
                conditions.add(criteriaBuilder.or(keywordMatches.toArray(Predicate[]::new)));
            }
            return criteriaBuilder.and(conditions.toArray(Predicate[]::new));
        };

        return repository.findAll(specification, Sort.by(Sort.Direction.DESC, "postedAt"));
    }

    @Transactional
    public int deleteOlderThan(Instant cutoff) {
        return repository.deleteByPostedAtBefore(cutoff);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
