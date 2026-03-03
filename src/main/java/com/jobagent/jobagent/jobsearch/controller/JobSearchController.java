package com.jobagent.jobagent.jobsearch.controller;

import com.jobagent.jobagent.jobsearch.dto.*;
import com.jobagent.jobagent.jobsearch.model.JobStatus;
import com.jobagent.jobagent.jobsearch.service.JobMatchingService;
import com.jobagent.jobagent.jobsearch.service.JobSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Job Search", description = "Browse, search, and match job listings against your CV profile")
public class JobSearchController {

    private final JobSearchService jobSearchService;
    private final JobMatchingService jobMatchingService;

    @Operation(summary = "List active jobs", description = "Returns a paginated list of active job listings")
    @GetMapping
    public ResponseEntity<Page<JobListingResponse>> getJobs(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting jobs page={}, size={}", page, size);
        return ResponseEntity.ok(jobSearchService.getActiveJobs(page, size));
    }

    @Operation(summary = "Search jobs with filters", description = "Searches job listings using keyword, skills, location, salary, and other filters")
    @PostMapping("/search")
    public ResponseEntity<Page<JobListingResponse>> searchJobs(
            @Valid @RequestBody JobSearchRequest request) {

        log.debug("Searching jobs with criteria: {}", request);
        return ResponseEntity.ok(jobSearchService.searchJobs(request));
    }

    @Operation(summary = "Get job by ID", description = "Returns full details of a specific job listing",
            responses = {
                @ApiResponse(responseCode = "200", description = "Job found"),
                @ApiResponse(responseCode = "404", description = "Job not found")
            })
    @GetMapping("/{id}")
    public ResponseEntity<JobListingResponse> getJobById(
            @Parameter(description = "Job listing identifier", required = true) @PathVariable UUID id) {
        log.debug("Getting job by ID: {}", id);
        return ResponseEntity.ok(jobSearchService.getJobById(id));
    }

    @Operation(summary = "Get matched jobs", description = "Returns jobs ranked by match percentage against the user's active CV skills",
            responses = {
                @ApiResponse(responseCode = "200", description = "Matched jobs returned"),
                @ApiResponse(responseCode = "404", description = "No active CV found for matching")
            })
    @GetMapping("/matches")
    public ResponseEntity<Page<JobMatchScore>> getMatchedJobs(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Minimum match percentage (0-100)") @RequestParam(defaultValue = "30") int minMatch,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.debug("Getting matched jobs for user {} with minMatch={}", userId, minMatch);

        return ResponseEntity.ok(
                jobMatchingService.getMatchedJobs(userId, minMatch, page, size)
        );
    }

    @Operation(summary = "Get top job matches", description = "Returns the top N best-matched jobs for the user's CV profile")
    @GetMapping("/top-matches")
    public ResponseEntity<List<JobMatchScore>> getTopMatches(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Maximum number of results") @RequestParam(defaultValue = "10") int limit) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.debug("Getting top {} matches for user {}", limit, userId);

        return ResponseEntity.ok(jobMatchingService.getTopMatches(userId, limit));
    }

    @Operation(summary = "Get match score for a job", description = "Calculates and returns the match score for a specific job against the user's CV")
    @GetMapping("/{id}/match")
    public ResponseEntity<JobMatchScore> getJobMatchScore(
            @Parameter(description = "Job listing identifier", required = true) @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.debug("Getting match score for job {} for user {}", id, userId);

        return ResponseEntity.ok(jobMatchingService.calculateMatchForJob(userId, id));
    }

    @Operation(summary = "Create a job listing", description = "Creates a new job listing (typically used by job scrapers or admin endpoints)",
            responses = {
                @ApiResponse(responseCode = "201", description = "Job created successfully"),
                @ApiResponse(responseCode = "400", description = "Validation error")
            })
    @PostMapping
    public ResponseEntity<JobListingResponse> createJob(
            @Valid @RequestBody CreateJobRequest request) {

        log.info("Creating job: {} at {}", request.title(), request.company());
        JobListingResponse created = jobSearchService.createJob(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update job status", description = "Updates the status of a job listing (e.g., ACTIVE, EXPIRED, CLOSED)")
    @PutMapping("/{id}/status")
    public ResponseEntity<JobListingResponse> updateJobStatus(
            @Parameter(description = "Job listing identifier", required = true) @PathVariable UUID id,
            @Parameter(description = "New job status", required = true) @RequestParam JobStatus status) {

        log.info("Updating job {} status to {}", id, status);
        return ResponseEntity.ok(jobSearchService.updateJobStatus(id, status));
    }

    @Operation(summary = "Get active job count", description = "Returns the total number of active job listings")
    @GetMapping("/count")
    public ResponseEntity<Long> getActiveJobCount() {
        return ResponseEntity.ok(jobSearchService.countActiveJobs());
    }
}
