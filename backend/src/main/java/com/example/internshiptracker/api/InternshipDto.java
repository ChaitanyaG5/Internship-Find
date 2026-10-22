package com.example.internshiptracker.api;

import java.time.Instant;

import com.example.internshiptracker.model.Internship;

public record InternshipDto(
        String title,
        String company,
        String location,
        Instant postedAt,
        String url,
        String source) {

    public static InternshipDto from(Internship internship) {
        return new InternshipDto(
                internship.getTitle(),
                internship.getCompany(),
                internship.getLocation(),
                internship.getPostedAt(),
                internship.getUrl(),
                internship.getSource());
    }
}
