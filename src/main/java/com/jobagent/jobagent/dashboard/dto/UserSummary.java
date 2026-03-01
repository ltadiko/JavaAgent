package com.jobagent.jobagent.dashboard.dto;

import lombok.Builder;
import java.time.Instant;

/**
 * Sprint 8.2 — User summary for dashboard.
 */
@Builder
public record UserSummary(
        String name,
        String email,
        Instant memberSince,
        String region
) {}
