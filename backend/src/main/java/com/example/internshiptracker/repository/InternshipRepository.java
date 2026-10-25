package com.example.internshiptracker.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.internshiptracker.model.Internship;

public interface InternshipRepository extends JpaRepository<Internship, Long>, JpaSpecificationExecutor<Internship> {

    List<Internship> findAllByUrlIn(Collection<String> urls);

    int deleteByApplicationDeadlineBefore(Instant cutoff);
}
