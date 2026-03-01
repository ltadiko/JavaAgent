package com.jobagent.jobagent.dashboard.dto;

import lombok.Builder;

/**
 * Sprint 8.2 — Complete dashboard summary.
 */
@Builder
public record DashboardSummary(
        UserSummary user,
        CvSummary cv,
        JobsSummary jobs,
        ApplicationsSummary applications,
        LettersSummary letters
) {}
