package com.example.internshiptracker.source;

import java.util.List;

import com.example.internshiptracker.model.Internship;

public interface JobSource {

    String name();

    List<Internship> fetch();
}
