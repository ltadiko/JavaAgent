package com.jobagent.jobagent.jobsearch.controller;

import com.jobagent.jobagent.jobsearch.dto.*;
import com.jobagent.jobagent.jobsearch.model.JobStatus;
import com.jobagent.jobagent.jobsearch.service.JobMatchingService;
import com.jobagent.jobagent.jobsearch.service.JobSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Sprint 5.7 — REST controller for job search operations.
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobSearchController {

    private final JobSearchService jobSearchService;
    private final JobMatchingService jobMatchingService;

    /**
     * Get active jobs with pagination.
     *
     * GET /api/v1/jobs?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<JobListingResponse>> getJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting jobs page={}, size={}", page, size);
        return ResponseEntity.ok(jobSearchService.getActiveJobs(page, size));
    }

    /**
     * Search jobs with filters.
     *
     * POST /api/v1/jobs/search
     */
    @PostMapping("/search")
    public ResponseEntity<Page<JobListingResponse>> searchJobs(
            @Valid @RequestBody JobSearchRequest request) {

        log.debug("Searching jobs with criteria: {}", request);
        return ResponseEntity.ok(jobSearchService.searchJobs(request));
    }

    /**
     * Get job by ID.
     *
     * GET /api/v1/jobs/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobListingResponse> getJobById(@PathVariable UUID id) {
        log.debug("Getting job by ID: {}", id);
        return ResponseEntity.ok(jobSearchService.getJobById(id));
    }

    /**
     * Get jobs matched to the current user's CV.
     *
     * GET /api/v1/jobs/matches?minMatch=50&page=0&size=20
     */
    @GetMapping("/matches")
    public ResponseEntity<Page<JobMatchScore>> getMatchedJobs(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "30") int minMatch,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.debug("Getting matched jobs for user {} with minMatch={}", userId, minMatch);

        return ResponseEntity.ok(
                jobMatchingService.getMatchedJobs(userId, minMatch, page, size)
        );
    }

    /**
     * Get top N job matches for current user.
     *
     * GET /api/v1/jobs/top-matches?limit=10
     */
    @GetMapping("/top-matches")
    public ResponseEntity<List<JobMatchScore>> getTopMatches(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "10") int limit) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.debug("Getting top {} matches for user {}", limit, userId);

        return ResponseEntity.ok(jobMatchingService.getTopMatches(userId, limit));
    }

    /**
     * Get match score for a specific job against user's CV.
     *
     * GET /api/v1/jobs/{id}/match
     */
    @GetMapping("/{id}/match")
    public ResponseEntity<JobMatchScore> getJobMatchScore(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.debug("Getting match score for job {} for user {}", id, userId);

        return ResponseEntity.ok(jobMatchingService.calculateMatchForJob(userId, id));
    }

    /**
     * Create a new job listing.
     *
     * POST /api/v1/jobs
     */
    @PostMapping
    public ResponseEntity<JobListingResponse> createJob(
            @Valid @RequestBody CreateJobRequest request) {

        log.info("Creating job: {} at {}", request.title(), request.company());
        JobListingResponse created = jobSearchService.createJob(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update job status.
     *
     * PUT /api/v1/jobs/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<JobListingResponse> updateJobStatus(
            @PathVariable UUID id,
            @RequestParam JobStatus status) {

        log.info("Updating job {} status to {}", id, status);
        return ResponseEntity.ok(jobSearchService.updateJobStatus(id, status));
    }

    /**
     * Get count of active jobs.
     *
     * GET /api/v1/jobs/count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getActiveJobCount() {
        return ResponseEntity.ok(jobSearchService.countActiveJobs());
    }
}
