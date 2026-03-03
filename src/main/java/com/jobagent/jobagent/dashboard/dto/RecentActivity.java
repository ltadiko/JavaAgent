package com.jobagent.jobagent.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 8.2 — Recent activity item for dashboard feed.
 */
@Builder
@Schema(description = "A recent activity item in the user's activity feed")
public record RecentActivity(
        @Schema(description = "Unique activity identifier", example = "550e8400-e29b-41d4-a716-446655440000", format = "uuid")
        UUID id,

        @Schema(description = "Type of activity", example = "APPLICATION_SUBMITTED")
        ActivityType type,

        @Schema(description = "Activity title", example = "Application Submitted")
        String title,

        @Schema(description = "Human-readable activity description", example = "You submitted an application to TechCorp GmbH for Senior Java Developer")
        String description,

        @Schema(description = "Type of entity involved in the activity", example = "APPLICATION", allowableValues = {"CV", "JOB", "LETTER", "APPLICATION"})
        String entityType,

        @Schema(description = "Identifier of the related entity", example = "660e8400-e29b-41d4-a716-446655440000", format = "uuid")
        UUID entityId,

        @Schema(description = "Timestamp when the activity occurred", example = "2026-03-03T10:15:30Z", format = "date-time")
        Instant timestamp
) {
    @Schema(description = "Enumeration of possible activity types")
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
