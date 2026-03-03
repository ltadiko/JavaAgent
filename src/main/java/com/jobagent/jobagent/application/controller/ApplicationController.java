package com.jobagent.jobagent.application.controller;

import com.jobagent.jobagent.application.dto.*;
import com.jobagent.jobagent.application.model.ApplicationStatus;
import com.jobagent.jobagent.application.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sprint 7.7 — REST controller for job application operations.
 */
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Applications", description = "Create, submit, track, and manage job applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    @Operation(summary = "Create a job application", description = "Creates a new draft job application with the specified job, CV, and optional motivation letter",
            responses = {
                @ApiResponse(responseCode = "200", description = "Application created as draft"),
                @ApiResponse(responseCode = "404", description = "Job, CV, or letter not found"),
                @ApiResponse(responseCode = "409", description = "Application already exists for this job")
            })
    @PostMapping
    public ResponseEntity<JobApplicationResponse> createApplication(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SubmitApplicationRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} creating application for job {}", userId, request.jobId());

        JobApplicationResponse response = applicationService.createApplication(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Submit an application", description = "Transitions a draft application to submitted state and triggers the sending process")
    @PostMapping("/{id}/submit")
    public ResponseEntity<JobApplicationResponse> submitApplication(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Application identifier", required = true) @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} submitting application {}", userId, id);

        JobApplicationResponse response = applicationService.submitApplication(id, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List applications", description = "Returns a paginated list of all applications for the authenticated user")
    @GetMapping
    public ResponseEntity<Page<JobApplicationResponse>> getApplications(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        UUID userId = UUID.fromString(jwt.getSubject());
        Page<JobApplicationResponse> applications = applicationService.getApplications(userId, page, size);
        return ResponseEntity.ok(applications);
    }

    @Operation(summary = "List applications by status", description = "Returns a paginated list of applications filtered by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<JobApplicationResponse>> getApplicationsByStatus(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Application status filter", required = true) @PathVariable ApplicationStatus status,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        UUID userId = UUID.fromString(jwt.getSubject());
        Page<JobApplicationResponse> applications = applicationService.getApplicationsByStatus(
                userId, status, page, size);
        return ResponseEntity.ok(applications);
    }

    @Operation(summary = "Get an application", description = "Returns full details of a specific application",
            responses = {
                @ApiResponse(responseCode = "200", description = "Application found"),
                @ApiResponse(responseCode = "404", description = "Application not found")
            })
    @GetMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> getApplication(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Application identifier", required = true) @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        JobApplicationResponse application = applicationService.getApplication(id, userId);
        return ResponseEntity.ok(application);
    }

    @Operation(summary = "Update application status", description = "Updates the status of an application (e.g., INTERVIEW, OFFER, REJECTED)")
    @PutMapping("/{id}/status")
    public ResponseEntity<JobApplicationResponse> updateStatus(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Application identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody ApplicationStatusUpdate update) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} updating application {} status to {}", userId, id, update.status());

        JobApplicationResponse application = applicationService.updateStatus(id, userId, update);
        return ResponseEntity.ok(application);
    }

    @Operation(summary = "Delete a draft application", description = "Deletes an application that is still in DRAFT status",
            responses = {
                @ApiResponse(responseCode = "204", description = "Application deleted"),
                @ApiResponse(responseCode = "400", description = "Cannot delete non-draft application"),
                @ApiResponse(responseCode = "404", description = "Application not found")
            })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Application identifier", required = true) @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} deleting application {}", userId, id);

        applicationService.deleteApplication(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Withdraw an application", description = "Withdraws a submitted application, notifying the employer if possible")
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<JobApplicationResponse> withdrawApplication(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Application identifier", required = true) @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} withdrawing application {}", userId, id);

        JobApplicationResponse application = applicationService.withdrawApplication(id, userId);
        return ResponseEntity.ok(application);
    }

    @Operation(summary = "Get application statistics", description = "Returns aggregated statistics of all applications grouped by status")
    @GetMapping("/stats")
    public ResponseEntity<ApplicationStatsResponse> getStats(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        ApplicationStatsResponse stats = applicationService.getStats(userId);
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Get application timeline", description = "Returns the chronological audit trail of events for a specific application")
    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<ApplicationTimeline>> getTimeline(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Application identifier", required = true) @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        List<ApplicationTimeline> timeline = applicationService.getTimeline(id, userId);
        return ResponseEntity.ok(timeline);
    }

    @Operation(summary = "Check if applied to a job", description = "Returns whether the authenticated user has already applied to a specific job")
    @GetMapping("/check/{jobId}")
    public ResponseEntity<Map<String, Boolean>> hasApplied(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Job listing identifier", required = true) @PathVariable UUID jobId) {

        UUID userId = UUID.fromString(jwt.getSubject());
        boolean applied = applicationService.hasApplied(userId, jobId);
        return ResponseEntity.ok(Map.of("applied", applied));
    }
}
