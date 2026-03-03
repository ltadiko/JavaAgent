package com.jobagent.jobagent.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Sprint 8.2 — Complete dashboard summary.
 */
@Builder
@Schema(description = "Complete dashboard summary aggregating user, CV, jobs, applications, and letters data")
public record DashboardSummary(
        @Schema(description = "User profile summary")
        UserSummary user,

        @Schema(description = "CV upload and parsing summary")
        CvSummary cv,

        @Schema(description = "Job search and matching summary")
        JobsSummary jobs,

        @Schema(description = "Application statistics summary")
        ApplicationsSummary applications,

        @Schema(description = "Motivation letters summary")
        LettersSummary letters
) {}
