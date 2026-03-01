package com.jobagent.jobagent.jobsearch.model;

import com.jobagent.jobagent.common.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sprint 5.1 — Job listing entity with multi-tenant support.
 *
 * Represents a job posting that users can search, match against their CV,
 * and apply to.
 */
@Entity
@Table(name = "job_listings", indexes = {
        @Index(name = "idx_job_tenant_id", columnList = "tenantId"),
        @Index(name = "idx_job_status", columnList = "status"),
        @Index(name = "idx_job_location", columnList = "location"),
        @Index(name = "idx_job_company", columnList = "company"),
        @Index(name = "idx_job_created_at", columnList = "createdAt")
})
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobListing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false)
    private String company;

    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    /**
     * List of required/desired skills for this job.
     * Stored as JSONB for efficient querying.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> skills = new ArrayList<>();

    @Column(precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Column(length = 3)
    @Builder.Default
    private String salaryCurrency = "EUR";

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private RemoteType remoteType;

    @Column(length = 2000)
    private String sourceUrl;

    /**
     * External ID from the source system (e.g., LinkedIn job ID).
     */
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private JobStatus status = JobStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    private Instant expiresAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Check if the job listing has expired.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if the job is still active and not expired.
     */
    public boolean isAvailable() {
        return status == JobStatus.ACTIVE && !isExpired();
    }

    /**
     * Add a skill to the required skills list.
     */
    public void addSkill(String skill) {
        if (skills == null) {
            skills = new ArrayList<>();
        }
        if (skill != null && !skill.isBlank()) {
            skills.add(skill.trim().toLowerCase());
        }
    }

    /**
     * Get salary range as formatted string.
     */
    public String getSalaryRange() {
        if (salaryMin == null && salaryMax == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (salaryMin != null) {
            sb.append(salaryCurrency).append(" ").append(salaryMin);
        }
        if (salaryMax != null) {
            if (salaryMin != null) {
                sb.append(" - ");
            }
            sb.append(salaryCurrency).append(" ").append(salaryMax);
        }
        return sb.toString();
    }
}
