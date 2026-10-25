package com.example.internshiptracker.ai;

import java.util.List;
import java.util.Map;
import java.util.Arrays;

final class SearchFilterSchema {

    private static final Map<String, Object> SCHEMA = Map.of(
            "type", "object",
            "additionalProperties", false,
            "properties", Map.of(
                    "keywords", Map.of("type", "string"),
                    "domain", nullableString(),
                    "location", nullableString(),
                    "workMode", Map.of("type", List.of("string", "null"), "enum",
                            Arrays.asList("REMOTE", "ONSITE", "HYBRID", null)),
                    "experienceRequirement", nullableString(),
                    "graduationYear", Map.of("type", List.of("integer", "null")),
                    "postedWithinHours", Map.of("type", List.of("integer", "null")),
                    "companyPreferences", stringArray(),
                    "technologyPreferences", stringArray()),
            "required", List.of(
                    "keywords", "domain", "location", "workMode", "experienceRequirement",
                    "graduationYear", "postedWithinHours", "companyPreferences", "technologyPreferences"));

    private SearchFilterSchema() {
    }

    static Map<String, Object> schema() {
        return SCHEMA;
    }

    private static Map<String, Object> nullableString() {
        return Map.of("type", List.of("string", "null"));
    }

    private static Map<String, Object> stringArray() {
        return Map.of("type", "array", "items", Map.of("type", "string"));
    }
}
