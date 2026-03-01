package com.jobagent.jobagent.application.controller;

import com.jobagent.jobagent.application.dto.*;
import com.jobagent.jobagent.application.model.ApplicationStatus;
import com.jobagent.jobagent.application.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Sprint 7.7 — REST controller for job application operations.
 */
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Slf4j
public class ApplicationController {

    private final ApplicationService applicationService;

    /**
     * Create a new job application (draft).
     */
    @PostMapping
    public ResponseEntity<JobApplicationResponse> createApplication(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SubmitApplicationRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} creating application for job {}", userId, request.jobId());

        JobApplicationResponse response = applicationService.createApplication(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Submit an application for sending.
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<JobApplicationResponse> submitApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} submitting application {}", userId, id);

        JobApplicationResponse response = applicationService.submitApplication(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all applications for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<Page<JobApplicationResponse>> getApplications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = UUID.fromString(jwt.getSubject());
        Page<JobApplicationResponse> applications = applicationService.getApplications(userId, page, size);
        return ResponseEntity.ok(applications);
    }

    /**
     * Get applications by status.
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<JobApplicationResponse>> getApplicationsByStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = UUID.fromString(jwt.getSubject());
        Page<JobApplicationResponse> applications = applicationService.getApplicationsByStatus(
                userId, status, page, size);
        return ResponseEntity.ok(applications);
    }

    /**
     * Get a specific application.
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> getApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        JobApplicationResponse application = applicationService.getApplication(id, userId);
        return ResponseEntity.ok(application);
    }

    /**
     * Update application status.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<JobApplicationResponse> updateStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody ApplicationStatusUpdate update) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} updating application {} status to {}", userId, id, update.status());

        JobApplicationResponse application = applicationService.updateStatus(id, userId, update);
        return ResponseEntity.ok(application);
    }

    /**
     * Delete a draft application.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} deleting application {}", userId, id);

        applicationService.deleteApplication(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Withdraw an application.
     */
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<JobApplicationResponse> withdrawApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} withdrawing application {}", userId, id);

        JobApplicationResponse application = applicationService.withdrawApplication(id, userId);
        return ResponseEntity.ok(application);
    }

    /**
     * Get application statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApplicationStatsResponse> getStats(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        ApplicationStatsResponse stats = applicationService.getStats(userId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Check if user has applied to a job.
     */
    @GetMapping("/check/{jobId}")
    public ResponseEntity<Map<String, Boolean>> hasApplied(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID jobId) {

        UUID userId = UUID.fromString(jwt.getSubject());
        boolean applied = applicationService.hasApplied(userId, jobId);
        return ResponseEntity.ok(Map.of("applied", applied));
    }
}
