package com.jobagent.jobagent.application.repository;

import com.jobagent.jobagent.application.model.ApplicationStatus;
import com.jobagent.jobagent.application.model.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Sprint 7.3 — Repository for JobApplication entity.
 */
@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    /**
     * Find all applications for a user in a tenant.
     */
    Page<JobApplication> findByUserIdAndTenantIdOrderByCreatedAtDesc(
            UUID userId, UUID tenantId, Pageable pageable);

    /**
     * Find application by ID and tenant (for security).
     */
    Optional<JobApplication> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Find application by ID, user, and tenant.
     */
    Optional<JobApplication> findByIdAndUserIdAndTenantId(UUID id, UUID userId, UUID tenantId);

    /**
     * Find applications by status.
     */
    Page<JobApplication> findByUserIdAndTenantIdAndStatusOrderByCreatedAtDesc(
            UUID userId, UUID tenantId, ApplicationStatus status, Pageable pageable);

    /**
     * Find applications by multiple statuses.
     */
    Page<JobApplication> findByUserIdAndTenantIdAndStatusInOrderByCreatedAtDesc(
            UUID userId, UUID tenantId, List<ApplicationStatus> statuses, Pageable pageable);

    /**
     * Find application for a specific job by user.
     */
    Optional<JobApplication> findByUserIdAndJobIdAndTenantId(UUID userId, UUID jobId, UUID tenantId);

    /**
     * Check if user has applied to a job.
     */
    boolean existsByUserIdAndJobIdAndTenantId(UUID userId, UUID jobId, UUID tenantId);

    /**
     * Find pending applications that need to be sent.
     */
    List<JobApplication> findByStatusAndTenantId(ApplicationStatus status, UUID tenantId);

    /**
     * Find all pending applications across tenants (for background processing).
     */
    List<JobApplication> findByStatus(ApplicationStatus status);

    /**
     * Count applications by status for a user.
     */
    long countByUserIdAndTenantIdAndStatus(UUID userId, UUID tenantId, ApplicationStatus status);

    /**
     * Count total applications for a user.
     */
    long countByUserIdAndTenantId(UUID userId, UUID tenantId);

    /**
     * Get application statistics for a user.
     */
    @Query("SELECT a.status, COUNT(a) FROM JobApplication a " +
           "WHERE a.user.id = :userId AND a.tenantId = :tenantId " +
           "GROUP BY a.status")
    List<Object[]> getStatusCounts(
            @Param("userId") UUID userId,
            @Param("tenantId") UUID tenantId);

    /**
     * Delete all applications for a user (for account deletion).
     */
    void deleteByUserIdAndTenantId(UUID userId, UUID tenantId);
}
