package com.example.internshiptracker.ai;

final class FilterExtractionPrompt {

    static final String INSTRUCTIONS = """
            Extract search filters for real internship job listings from the user's query.
            Do not invent jobs, employers, dates, or application links. Return only the requested
            structured filter fields. Use null for unspecified optional values, empty arrays for
            unspecified lists, and a maximum posting age of 24 hours unless another period is
            explicitly requested. Include internship in keywords. Interpret dates relative to
            the current date supplied by the application. Use workMode REMOTE, ONSITE, HYBRID,
            or null. graduationYear is a four-digit year or null.
            """;

    private FilterExtractionPrompt() {
    }
}
