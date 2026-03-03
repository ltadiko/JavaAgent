package com.jobagent.jobagent.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.Instant;

/**
 * Sprint 8.2 — User summary for dashboard.
 */
@Builder
@Schema(description = "User profile summary for the dashboard")
public record UserSummary(
        @Schema(description = "User's full name", example = "John Doe")
        String name,

        @Schema(description = "User's email address", example = "john.doe@example.com", format = "email")
        String email,

        @Schema(description = "Account registration date", example = "2026-01-15T08:00:00Z", format = "date-time")
        Instant memberSince,

        @Schema(description = "Data residency region", example = "eu-west")
        String region
) {}
