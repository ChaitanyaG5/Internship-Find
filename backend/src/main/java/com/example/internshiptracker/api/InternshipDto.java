package com.example.internshiptracker.api;

import java.time.Instant;

import com.example.internshiptracker.model.Internship;

public record InternshipDto(
        String title,
        String company,
        String location,
        Instant postedAt,
        Instant applicationDeadline,
        String url,
        String source) {

    public static InternshipDto from(Internship internship) {
        return new InternshipDto(
                internship.getTitle(),
                internship.getCompany(),
                internship.getLocation(),
                internship.getPostedAt(),
                internship.getApplicationDeadline(),
                internship.getUrl(),
                internship.getSource());
    }
}
