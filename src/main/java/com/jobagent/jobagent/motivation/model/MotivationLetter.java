package com.jobagent.jobagent.motivation.model;

import com.jobagent.jobagent.auth.model.User;
import com.jobagent.jobagent.common.multitenancy.TenantEntityListener;
import com.jobagent.jobagent.cv.model.CvDetails;
import com.jobagent.jobagent.jobsearch.model.JobListing;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 6.1 — Motivation letter entity.
 *
 * Stores AI-generated motivation letters linked to user's CV and target job.
 */
@Entity
@Table(name = "motivation_letters", indexes = {
        @Index(name = "idx_motivation_tenant_id", columnList = "tenantId"),
        @Index(name = "idx_motivation_user_id", columnList = "user_id"),
        @Index(name = "idx_motivation_job_id", columnList = "job_listing_id")
})
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MotivationLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id")
    private CvDetails cv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_listing_id")
    private JobListing jobListing;

    /**
     * The AI-generated letter content (encrypted in DB via migration).
     */
    @Column(name = "letter_text_encrypted", columnDefinition = "TEXT")
    private String generatedContent;

    /**
     * User's edited version of the letter.
     */
    @Column(columnDefinition = "TEXT")
    private String editedContent;

    /**
     * Additional instructions provided by user for generation.
     */
    @Column(name = "additional_instructions", columnDefinition = "TEXT")
    private String additionalInstructions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LetterStatus status = LetterStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private LetterTone tone = LetterTone.PROFESSIONAL;

    @Column(length = 10)
    @Builder.Default
    private String language = "en";

    @Column(name = "word_count")
    private Integer wordCount;

    /**
     * S3 key for PDF version of the letter.
     */
    @Column(name = "pdf_s3_key")
    private String pdfS3Key;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Get the final content (edited if available, otherwise generated).
     */
    public String getFinalContent() {
        return editedContent != null && !editedContent.isBlank()
                ? editedContent
                : generatedContent;
    }

    /**
     * Check if the letter has been edited by the user.
     */
    public boolean isEdited() {
        return editedContent != null && !editedContent.isBlank();
    }

    /**
     * Calculate word count from content.
     */
    public int calculateWordCount() {
        String content = getFinalContent();
        if (content == null || content.isBlank()) {
            return 0;
        }
        return content.trim().split("\\s+").length;
    }

    /**
     * Update word count based on current content.
     */
    public void updateWordCount() {
        this.wordCount = calculateWordCount();
    }
}
