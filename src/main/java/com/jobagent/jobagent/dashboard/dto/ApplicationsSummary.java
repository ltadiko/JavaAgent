package com.jobagent.jobagent.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Sprint 8.2 — Applications summary for dashboard.
 */
@Builder
@Schema(description = "Application statistics summary for the dashboard")
public record ApplicationsSummary(
        @Schema(description = "Total number of applications", example = "25", format = "int64")
        long total,

        @Schema(description = "Number of draft applications", example = "3", format = "int64")
        long drafts,

        @Schema(description = "Number of pending applications", example = "5", format = "int64")
        long pending,

        @Schema(description = "Number of sent applications", example = "10", format = "int64")
        long sent,

        @Schema(description = "Number of applications at interview stage", example = "4", format = "int64")
        long interviews,

        @Schema(description = "Number of job offers received", example = "2", format = "int64")
        long offers,

        @Schema(description = "Number of rejected applications", example = "1", format = "int64")
        long rejected,

        @Schema(description = "Number of withdrawn applications", example = "0", format = "int64")
        long withdrawn
) {}
