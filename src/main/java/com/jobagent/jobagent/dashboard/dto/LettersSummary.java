package com.jobagent.jobagent.dashboard.dto;

import lombok.Builder;
import java.time.Instant;

/**
 * Sprint 8.2 — Letters summary for dashboard.
 */
@Builder
public record LettersSummary(
        long count,
        Instant latestAt
) {}
