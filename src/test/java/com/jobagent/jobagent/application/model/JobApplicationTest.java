package com.jobagent.jobagent.application.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprint 7.8 — Unit tests for JobApplication entity.
 */
@DisplayName("JobApplication Entity Tests")
class JobApplicationTest {

    @Test
    @DisplayName("Builder should set default values")
    void builder_shouldSetDefaults() {
        JobApplication app = JobApplication.builder().build();

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
        assertThat(app.getCreatedAt()).isNotNull();
        assertThat(app.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("canSubmit() returns true for DRAFT")
    void canSubmit_draft_returnsTrue() {
        JobApplication app = JobApplication.builder()
                .status(ApplicationStatus.DRAFT)
                .build();

        assertThat(app.canSubmit()).isTrue();
    }

    @Test
    @DisplayName("canSubmit() returns false for non-DRAFT")
    void canSubmit_nonDraft_returnsFalse() {
        JobApplication app = JobApplication.builder()
                .status(ApplicationStatus.SENT)
                .build();

        assertThat(app.canSubmit()).isFalse();
    }

    @Test
    @DisplayName("canModify() returns true for DRAFT")
    void canModify_draft_returnsTrue() {
        JobApplication app = JobApplication.builder()
                .status(ApplicationStatus.DRAFT)
                .build();

        assertThat(app.canModify()).isTrue();
    }

    @Test
    @DisplayName("canModify() returns true for FAILED")
    void canModify_failed_returnsTrue() {
        JobApplication app = JobApplication.builder()
                .status(ApplicationStatus.FAILED)
                .build();

        assertThat(app.canModify()).isTrue();
    }

    @Test
    @DisplayName("canModify() returns false for SENT")
    void canModify_sent_returnsFalse() {
        JobApplication app = JobApplication.builder()
                .status(ApplicationStatus.SENT)
                .build();

        assertThat(app.canModify()).isFalse();
    }

    @Test
    @DisplayName("isFinal() returns true for ACCEPTED")
    void isFinal_accepted_returnsTrue() {
        JobApplication app = JobApplication.builder()
                .status(ApplicationStatus.ACCEPTED)
                .build();

        assertThat(app.isFinal()).isTrue();
    }

    @Test
    @DisplayName("isFinal() returns true for REJECTED")
    void isFinal_rejected_returnsTrue() {
        JobApplication app = JobApplication.builder()
                .status(ApplicationStatus.REJECTED)
                .build();

        assertThat(app.isFinal()).isTrue();
    }

    @Test
    @DisplayName("isFinal() returns false for SENT")
    void isFinal_sent_returnsFalse() {
        JobApplication app = JobApplication.builder()
                .status(ApplicationStatus.SENT)
                .build();

        assertThat(app.isFinal()).isFalse();
    }

    @Test
    @DisplayName("submit() changes status to PENDING")
    void submit_changesStatusToPending() {
        JobApplication app = JobApplication.builder()
                .status(ApplicationStatus.DRAFT)
                .build();

        app.submit();

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(app.getSubmittedAt()).isNotNull();
    }

    @Test
    @DisplayName("submit() throws when not DRAFT")
    void submit_notDraft_throws() {
        JobApplication app = JobApplication.builder()
                .status(ApplicationStatus.SENT)
                .build();

        assertThatThrownBy(app::submit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot submit");
    }

    @Test
    @DisplayName("markSent() updates status and fields")
    void markSent_updatesFields() {
        JobApplication app = JobApplication.builder().build();

        app.markSent("REF-123", "EMAIL");

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.SENT);
        assertThat(app.getConfirmationRef()).isEqualTo("REF-123");
        assertThat(app.getApplyMethod()).isEqualTo("EMAIL");
        assertThat(app.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("markFailed() updates status and reason")
    void markFailed_updatesFields() {
        JobApplication app = JobApplication.builder().build();

        app.markFailed("Connection timeout");

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.FAILED);
        assertThat(app.getFailureReason()).isEqualTo("Connection timeout");
    }

    @Test
    @DisplayName("withdraw() changes status to WITHDRAWN")
    void withdraw_changesStatus() {
        JobApplication app = JobApplication.builder()
                .status(ApplicationStatus.SENT)
                .build();

        app.withdraw();

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("withdraw() throws when in final status")
    void withdraw_final_throws() {
        JobApplication app = JobApplication.builder()
                .status(ApplicationStatus.ACCEPTED)
                .build();

        assertThatThrownBy(app::withdraw)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot withdraw");
    }

    @Test
    @DisplayName("ApplicationStatus enum has expected values")
    void applicationStatus_hasExpectedValues() {
        assertThat(ApplicationStatus.values()).contains(
                ApplicationStatus.DRAFT,
                ApplicationStatus.PENDING,
                ApplicationStatus.PROCESSING,
                ApplicationStatus.SENT,
                ApplicationStatus.VIEWED,
                ApplicationStatus.REJECTED,
                ApplicationStatus.INTERVIEW,
                ApplicationStatus.OFFERED,
                ApplicationStatus.ACCEPTED,
                ApplicationStatus.WITHDRAWN,
                ApplicationStatus.FAILED
        );
    }

    @Test
    @DisplayName("Can set all fields via builder")
    void builder_canSetAllFields() {
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant now = Instant.now();

        JobApplication app = JobApplication.builder()
                .id(id)
                .tenantId(tenantId)
                .status(ApplicationStatus.INTERVIEW)
                .applyMethod("PORTAL")
                .confirmationRef("CONF-456")
                .failureReason(null)
                .additionalMessage("Looking forward to hearing from you")
                .submittedAt(now)
                .sentAt(now)
                .viewedAt(now)
                .responseAt(now)
                .version(1)
                .build();

        assertThat(app.getId()).isEqualTo(id);
        assertThat(app.getTenantId()).isEqualTo(tenantId);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
        assertThat(app.getApplyMethod()).isEqualTo("PORTAL");
        assertThat(app.getConfirmationRef()).isEqualTo("CONF-456");
        assertThat(app.getAdditionalMessage()).isEqualTo("Looking forward to hearing from you");
    }
}
