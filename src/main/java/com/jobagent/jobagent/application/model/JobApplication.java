package com.jobagent.jobagent.application.model;

import com.jobagent.jobagent.auth.model.User;
import com.jobagent.jobagent.common.multitenancy.TenantEntityListener;
import com.jobagent.jobagent.cv.model.CvDetails;
import com.jobagent.jobagent.jobsearch.model.JobListing;
import com.jobagent.jobagent.motivation.model.MotivationLetter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 7.2 — Job application entity.
 *
 * Tracks job applications submitted by users, linking CV and motivation letter.
 */
@Entity
@Table(name = "applications", indexes = {
        @Index(name = "idx_applications_tenant", columnList = "tenantId"),
        @Index(name = "idx_applications_user", columnList = "user_id"),
        @Index(name = "idx_applications_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "job_listing_id"})
})
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_listing_id", nullable = false)
    private JobListing job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    private CvDetails cv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "letter_id")
    private MotivationLetter letter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    /**
     * Method used to apply (EMAIL, PORTAL, API, etc.)
     */
    @Column(name = "apply_method", length = 20)
    private String applyMethod;

    /**
     * Confirmation reference from employer (if any).
     */
    @Column(name = "confirmation_ref")
    private String confirmationRef;

    /**
     * Reason for failure (if status is FAILED).
     */
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    /**
     * Additional message/cover note from user.
     */
    @Column(name = "additional_message", columnDefinition = "TEXT")
    private String additionalMessage;

    /**
     * When the application was submitted by user.
     */
    @Column(name = "submitted_at")
    private Instant submittedAt;

    /**
     * When the application was actually sent to employer.
     */
    @Column(name = "sent_at")
    private Instant sentAt;

    /**
     * When the employer viewed the application (if trackable).
     */
    @Column(name = "viewed_at")
    private Instant viewedAt;

    /**
     * When the employer responded.
     */
    @Column(name = "response_at")
    private Instant responseAt;

    @Version
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Check if application can be submitted.
     */
    public boolean canSubmit() {
        return status == ApplicationStatus.DRAFT;
    }

    /**
     * Check if application can be modified.
     */
    public boolean canModify() {
        return status == ApplicationStatus.DRAFT || status == ApplicationStatus.FAILED;
    }

    /**
     * Check if application is in a final state.
     */
    public boolean isFinal() {
        return status == ApplicationStatus.ACCEPTED ||
               status == ApplicationStatus.REJECTED ||
               status == ApplicationStatus.WITHDRAWN;
    }

    /**
     * Submit the application.
     */
    public void submit() {
        if (!canSubmit()) {
            throw new IllegalStateException("Cannot submit application in status: " + status);
        }
        this.status = ApplicationStatus.PENDING;
        this.submittedAt = Instant.now();
    }

    /**
     * Mark as sent.
     */
    public void markSent(String confirmationRef, String applyMethod) {
        this.status = ApplicationStatus.SENT;
        this.sentAt = Instant.now();
        this.confirmationRef = confirmationRef;
        this.applyMethod = applyMethod;
    }

    /**
     * Mark as failed.
     */
    public void markFailed(String reason) {
        this.status = ApplicationStatus.FAILED;
        this.failureReason = reason;
    }

    /**
     * Withdraw the application.
     */
    public void withdraw() {
        if (isFinal()) {
            throw new IllegalStateException("Cannot withdraw application in final status: " + status);
        }
        this.status = ApplicationStatus.WITHDRAWN;
    }
}
