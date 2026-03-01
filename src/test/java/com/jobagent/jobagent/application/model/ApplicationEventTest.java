package com.jobagent.jobagent.application.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 8.4 — Unit tests for ApplicationEvent entity.
 */
@DisplayName("ApplicationEvent Entity Tests")
class ApplicationEventTest {

    @Test
    @DisplayName("Builder should set default createdAt")
    void builder_shouldSetDefaults() {
        ApplicationEvent event = ApplicationEvent.builder().build();
        assertThat(event.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("statusChange() factory creates correct event")
    void statusChange_createsCorrectEvent() {
        JobApplication app = JobApplication.builder()
                .tenantId(UUID.randomUUID())
                .status(ApplicationStatus.SENT)
                .build();
        app.setId(UUID.randomUUID());

        ApplicationEvent event = ApplicationEvent.statusChange(
                app, ApplicationStatus.PENDING, ApplicationStatus.SENT, "Sent via email");

        assertThat(event.getEventType()).isEqualTo(ApplicationEvent.EventType.STATUS_CHANGED);
        assertThat(event.getOldStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(event.getNewStatus()).isEqualTo(ApplicationStatus.SENT);
        assertThat(event.getDetails()).isEqualTo("Sent via email");
        assertThat(event.getTenantId()).isEqualTo(app.getTenantId());
        assertThat(event.getApplication()).isEqualTo(app);
    }

    @Test
    @DisplayName("created() factory creates correct event")
    void created_createsCorrectEvent() {
        JobApplication app = JobApplication.builder()
                .tenantId(UUID.randomUUID())
                .status(ApplicationStatus.DRAFT)
                .build();
        app.setId(UUID.randomUUID());

        ApplicationEvent event = ApplicationEvent.created(app);

        assertThat(event.getEventType()).isEqualTo(ApplicationEvent.EventType.CREATED);
        assertThat(event.getNewStatus()).isEqualTo(ApplicationStatus.DRAFT);
        assertThat(event.getOldStatus()).isNull();
        assertThat(event.getDetails()).isEqualTo("Application created");
    }

    @Test
    @DisplayName("EventType enum has expected values")
    void eventType_hasExpectedValues() {
        assertThat(ApplicationEvent.EventType.values()).contains(
                ApplicationEvent.EventType.CREATED,
                ApplicationEvent.EventType.STATUS_CHANGED,
                ApplicationEvent.EventType.SUBMITTED,
                ApplicationEvent.EventType.SENT,
                ApplicationEvent.EventType.SEND_FAILED,
                ApplicationEvent.EventType.VIEWED,
                ApplicationEvent.EventType.RESPONSE_RECEIVED,
                ApplicationEvent.EventType.WITHDRAWN,
                ApplicationEvent.EventType.NOTE_ADDED
        );
    }

    @Test
    @DisplayName("Can set all fields via builder")
    void builder_canSetAllFields() {
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        JobApplication app = JobApplication.builder().build();

        ApplicationEvent event = ApplicationEvent.builder()
                .id(id)
                .tenantId(tenantId)
                .application(app)
                .eventType(ApplicationEvent.EventType.VIEWED)
                .oldStatus(ApplicationStatus.SENT)
                .newStatus(ApplicationStatus.VIEWED)
                .details("Employer opened the application")
                .build();

        assertThat(event.getId()).isEqualTo(id);
        assertThat(event.getTenantId()).isEqualTo(tenantId);
        assertThat(event.getApplication()).isEqualTo(app);
        assertThat(event.getEventType()).isEqualTo(ApplicationEvent.EventType.VIEWED);
        assertThat(event.getDetails()).isEqualTo("Employer opened the application");
    }
}
