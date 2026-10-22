package com.example.internshiptracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InternshipTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(InternshipTrackerApplication.class, args);
    }
}
