package com.jobagent.jobagent.dashboard.dto;

import lombok.Builder;

/**
 * Sprint 8.2 — Applications summary for dashboard.
 */
@Builder
public record ApplicationsSummary(
        long total,
        long drafts,
        long pending,
        long sent,
        long interviews,
        long offers,
        long rejected,
        long withdrawn
) {}
