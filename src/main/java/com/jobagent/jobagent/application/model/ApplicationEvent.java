package com.jobagent.jobagent.application.model;

import com.jobagent.jobagent.common.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 8.4 — Application event entity for audit trail.
 *
 * <p>Records every status change for a job application,
 * creating a complete timeline of the application lifecycle.
 */
@Entity
@Table(name = "application_events", indexes = {
        @Index(name = "idx_app_events_app", columnList = "application_id"),
        @Index(name = "idx_app_events_created", columnList = "createdAt")
})
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private ApplicationStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30)
    private ApplicationStatus newStatus;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Types of application events.
     */
    public enum EventType {
        CREATED,
        STATUS_CHANGED,
        SUBMITTED,
        SENT,
        SEND_FAILED,
        VIEWED,
        RESPONSE_RECEIVED,
        WITHDRAWN,
        NOTE_ADDED
    }

    /**
     * Factory: create a status-change event.
     */
    public static ApplicationEvent statusChange(
            JobApplication application,
            ApplicationStatus oldStatus,
            ApplicationStatus newStatus,
            String details) {

        return ApplicationEvent.builder()
                .tenantId(application.getTenantId())
                .application(application)
                .eventType(EventType.STATUS_CHANGED)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .details(details)
                .build();
    }

    /**
     * Factory: create a "created" event.
     */
    public static ApplicationEvent created(JobApplication application) {
        return ApplicationEvent.builder()
                .tenantId(application.getTenantId())
                .application(application)
                .eventType(EventType.CREATED)
                .newStatus(application.getStatus())
                .details("Application created")
                .build();
    }
}
