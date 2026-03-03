package com.jobagent.jobagent.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Sprint 7.4 — Request to submit a job application.
 */
@Schema(description = "Request payload for creating a job application")
public record SubmitApplicationRequest(
        @Schema(description = "Target job listing identifier", example = "550e8400-e29b-41d4-a716-446655440000", format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Job ID is required")
        UUID jobId,

        @Schema(description = "CV to attach to the application", example = "660e8400-e29b-41d4-a716-446655440000", format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "CV ID is required")
        UUID cvId,

        @Schema(description = "Optional motivation letter to include", example = "770e8400-e29b-41d4-a716-446655440000", format = "uuid")
        UUID letterId,

        @Schema(description = "Optional cover note or additional message to the employer", example = "I am very excited about this opportunity...")
        String additionalMessage
) {}
