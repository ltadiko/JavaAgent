package com.jobagent.jobagent.application.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Sprint 7.4 — Request to submit a job application.
 */
public record SubmitApplicationRequest(
        @NotNull(message = "Job ID is required")
        UUID jobId,

        @NotNull(message = "CV ID is required")
        UUID cvId,

        UUID letterId,  // Optional - motivation letter

        String additionalMessage  // Optional - cover note
) {}
