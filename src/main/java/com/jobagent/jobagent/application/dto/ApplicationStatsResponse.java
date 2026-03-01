package com.jobagent.jobagent.application.dto;

import java.util.Map;

/**
 * Sprint 7.4 — Application statistics response.
 */
public record ApplicationStatsResponse(
        long total,
        long drafts,
        long pending,
        long sent,
        long interviews,
        long offers,
        long rejected,
        Map<String, Long> byStatus
) {}
