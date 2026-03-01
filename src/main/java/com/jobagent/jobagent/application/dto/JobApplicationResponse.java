package com.jobagent.jobagent.application.dto;

import com.jobagent.jobagent.application.model.ApplicationStatus;
import com.jobagent.jobagent.application.model.JobApplication;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 7.4 — Response DTO for job applications.
 */
@Builder
public record JobApplicationResponse(
        UUID id,
        UUID jobId,
        String jobTitle,
        String company,
        String location,
        UUID cvId,
        String cvFileName,
        UUID letterId,
        ApplicationStatus status,
        String applyMethod,
        String confirmationRef,
        String failureReason,
        String additionalMessage,
        Instant submittedAt,
        Instant sentAt,
        Instant viewedAt,
        Instant responseAt,
        Instant createdAt,
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
