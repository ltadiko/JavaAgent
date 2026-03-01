package com.jobagent.jobagent.dashboard.dto;

import lombok.Builder;
import java.time.Instant;
import java.util.List;

/**
 * Sprint 8.2 — CV summary for dashboard.
 */
@Builder
public record CvSummary(
        int count,
        Instant latestParsedAt,
        int skillsCount,
        List<String> topSkills
) {}
