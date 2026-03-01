package com.jobagent.jobagent.dashboard.dto;

import lombok.Builder;
import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 8.2 — Recent activity item for dashboard feed.
 */
@Builder
public record RecentActivity(
        UUID id,
        ActivityType type,
        String title,
        String description,
        String entityType,
        UUID entityId,
        Instant timestamp
) {
    public enum ActivityType {
        CV_UPLOADED,
        CV_PARSED,
        JOB_MATCH,
        LETTER_GENERATED,
        APPLICATION_CREATED,
        APPLICATION_SUBMITTED,
        APPLICATION_SENT,
        APPLICATION_VIEWED,
        APPLICATION_RESPONSE
    }
}
