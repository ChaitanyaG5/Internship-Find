package com.example.internshiptracker.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "internships", uniqueConstraints = @UniqueConstraint(columnNames = "url"))
@Getter
@Setter
@NoArgsConstructor
public class Internship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 300)
    private String company;

    @Column(length = 500)
    private String location;

    @Column(nullable = false)
    private Instant postedAt;

    @Column(length = 10000)
    private String description;

    @Column(nullable = false, unique = true, length = 2048)
    private String url;

    @Column(nullable = false, length = 100)
    private String source;
}
