package com.jobagent.jobagent.application.dto;

import com.jobagent.jobagent.application.model.ApplicationEvent;
import com.jobagent.jobagent.application.model.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 8.4 — Timeline entry for application audit trail.
 */
@Builder
@Schema(description = "A single event in the application's timeline / audit trail")
public record ApplicationTimeline(
        @Schema(description = "Unique event identifier", example = "550e8400-e29b-41d4-a716-446655440000", format = "uuid")
        UUID id,

        @Schema(description = "Type of event that occurred", example = "STATUS_CHANGE", allowableValues = {"CREATED", "SUBMITTED", "SENT", "STATUS_CHANGE", "WITHDRAWN", "NOTE_ADDED"})
        String eventType,

        @Schema(description = "Previous application status", example = "DRAFT")
        ApplicationStatus oldStatus,

        @Schema(description = "New application status after the event", example = "SUBMITTED")
        ApplicationStatus newStatus,

        @Schema(description = "Additional details or notes about the event", example = "Application submitted via email")
        String details,

        @Schema(description = "Timestamp when the event occurred", example = "2026-03-03T10:15:30Z", format = "date-time")
        Instant timestamp
) {
    public static ApplicationTimeline from(ApplicationEvent event) {
        return ApplicationTimeline.builder()
                .id(event.getId())
                .eventType(event.getEventType().name())
                .oldStatus(event.getOldStatus())
                .newStatus(event.getNewStatus())
                .details(event.getDetails())
                .timestamp(event.getCreatedAt())
                .build();
    }
}
