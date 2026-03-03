package com.jobagent.jobagent.application.dto;

import com.jobagent.jobagent.application.model.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Sprint 7.4 — Request to update application status.
 */
@Schema(description = "Request payload for updating the status of a job application")
public record ApplicationStatusUpdate(
        @Schema(description = "New application status", example = "INTERVIEW", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Status is required")
        ApplicationStatus status,

        @Schema(description = "Optional notes about the status change", example = "Phone screen scheduled for March 10")
        String notes
) {}
