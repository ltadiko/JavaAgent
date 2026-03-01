package com.jobagent.jobagent.application.dto;

import com.jobagent.jobagent.application.model.ApplicationEvent;
import com.jobagent.jobagent.application.model.ApplicationStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 8.4 — Timeline entry for application audit trail.
 */
@Builder
public record ApplicationTimeline(
        UUID id,
        String eventType,
        ApplicationStatus oldStatus,
        ApplicationStatus newStatus,
        String details,
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
