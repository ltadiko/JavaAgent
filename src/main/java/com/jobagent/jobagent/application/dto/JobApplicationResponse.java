package com.jobagent.jobagent.application.dto;

import com.jobagent.jobagent.application.model.ApplicationStatus;
import com.jobagent.jobagent.application.model.JobApplication;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 7.4 — Response DTO for job applications.
 */
@Builder
@Schema(description = "Detailed information about a job application")
public record JobApplicationResponse(
        @Schema(description = "Unique application identifier", example = "550e8400-e29b-41d4-a716-446655440000", format = "uuid")
        UUID id,

        @Schema(description = "Target job listing identifier", example = "660e8400-e29b-41d4-a716-446655440000", format = "uuid")
        UUID jobId,

        @Schema(description = "Title of the applied job", example = "Senior Java Developer")
        String jobTitle,

        @Schema(description = "Company name", example = "TechCorp GmbH")
        String company,

        @Schema(description = "Job location", example = "Berlin, Germany")
        String location,

        @Schema(description = "CV attached to the application", example = "770e8400-e29b-41d4-a716-446655440000", format = "uuid")
        UUID cvId,

        @Schema(description = "Name of the attached CV file", example = "john-doe-cv.pdf")
        String cvFileName,

        @Schema(description = "Motivation letter attached to the application", example = "880e8400-e29b-41d4-a716-446655440000", format = "uuid")
        UUID letterId,

        @Schema(description = "Current application status", example = "SUBMITTED")
        ApplicationStatus status,

        @Schema(description = "Method used to submit the application", example = "EMAIL", allowableValues = {"EMAIL", "PORTAL", "API", "MANUAL"})
        String applyMethod,

        @Schema(description = "Confirmation reference from the employer", example = "APP-2026-0042")
        String confirmationRef,

        @Schema(description = "Reason for application failure (null if not failed)", example = "Email delivery failed")
        String failureReason,

        @Schema(description = "Additional message sent with the application")
        String additionalMessage,

        @Schema(description = "Timestamp when the application was submitted by the user", format = "date-time")
        Instant submittedAt,

        @Schema(description = "Timestamp when the application was sent to the employer", format = "date-time")
        Instant sentAt,

        @Schema(description = "Timestamp when the employer viewed the application", format = "date-time")
        Instant viewedAt,

        @Schema(description = "Timestamp when the employer responded", format = "date-time")
        Instant responseAt,

        @Schema(description = "Record creation timestamp", format = "date-time")
        Instant createdAt,

        @Schema(description = "Last update timestamp", format = "date-time")
        Instant updatedAt
) {
    /**
     * Create response from entity.
     */
    public static JobApplicationResponse from(JobApplication app) {
        return JobApplicationResponse.builder()
                .id(app.getId())
                .jobId(app.getJob() != null ? app.getJob().getId() : null)
                .jobTitle(app.getJob() != null ? app.getJob().getTitle() : null)
                .company(app.getJob() != null ? app.getJob().getCompany() : null)
                .location(app.getJob() != null ? app.getJob().getLocation() : null)
                .cvId(app.getCv() != null ? app.getCv().getId() : null)
                .cvFileName(app.getCv() != null ? app.getCv().getFileName() : null)
                .letterId(app.getLetter() != null ? app.getLetter().getId() : null)
                .status(app.getStatus())
                .applyMethod(app.getApplyMethod())
                .confirmationRef(app.getConfirmationRef())
                .failureReason(app.getFailureReason())
                .additionalMessage(app.getAdditionalMessage())
                .submittedAt(app.getSubmittedAt())
                .sentAt(app.getSentAt())
                .viewedAt(app.getViewedAt())
                .responseAt(app.getResponseAt())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
