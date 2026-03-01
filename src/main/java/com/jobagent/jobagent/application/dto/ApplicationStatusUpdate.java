package com.jobagent.jobagent.application.dto;

import com.jobagent.jobagent.application.model.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Sprint 7.4 — Request to update application status.
 */
public record ApplicationStatusUpdate(
        @NotNull(message = "Status is required")
        ApplicationStatus status,

        String notes  // Optional notes about the status change
) {}
