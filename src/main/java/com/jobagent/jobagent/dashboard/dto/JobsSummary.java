package com.jobagent.jobagent.dashboard.dto;

import lombok.Builder;

/**
 * Sprint 8.2 — Jobs summary for dashboard.
 */
@Builder
public record JobsSummary(
        int matchesCount,
        int topMatchScore,
        int newJobsToday,
        int savedJobs
) {}
