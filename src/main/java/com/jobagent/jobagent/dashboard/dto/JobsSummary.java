package com.jobagent.jobagent.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Sprint 8.2 — Jobs summary for dashboard.
 */
@Builder
@Schema(description = "Job search and matching summary for the dashboard")
public record JobsSummary(
        @Schema(description = "Number of matched jobs based on CV skills", example = "42", format = "int32")
        int matchesCount,

        @Schema(description = "Highest match score percentage", example = "92", format = "int32")
        int topMatchScore,

        @Schema(description = "Number of new jobs posted today", example = "7", format = "int32")
        int newJobsToday,

        @Schema(description = "Number of jobs saved by the user", example = "5", format = "int32")
        int savedJobs
) {}
