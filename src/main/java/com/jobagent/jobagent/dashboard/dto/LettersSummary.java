package com.jobagent.jobagent.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.Instant;

/**
 * Sprint 8.2 — Letters summary for dashboard.
 */
@Builder
@Schema(description = "Motivation letters summary for the dashboard")
public record LettersSummary(
        @Schema(description = "Total number of generated motivation letters", example = "8", format = "int64")
        long count,

        @Schema(description = "Timestamp of the most recently generated letter", example = "2026-03-02T16:45:00Z", format = "date-time")
        Instant latestAt
) {}
