package com.jobagent.jobagent.motivation.repository;

import com.jobagent.jobagent.motivation.model.LetterStatus;
import com.jobagent.jobagent.motivation.model.MotivationLetter;
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
 * Sprint 6.3 — Repository for MotivationLetter entity.
 */
@Repository
public interface MotivationLetterRepository extends JpaRepository<MotivationLetter, UUID> {

    /**
     * Find all letters for a user in a tenant.
     */
    Page<MotivationLetter> findByUserIdAndTenantIdOrderByUpdatedAtDesc(
            UUID userId, UUID tenantId, Pageable pageable);

    /**
     * Find letter by ID and tenant (for security).
     */
    Optional<MotivationLetter> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Find letter by ID, user, and tenant.
     */
    Optional<MotivationLetter> findByIdAndUserIdAndTenantId(UUID id, UUID userId, UUID tenantId);

    /**
     * Find all letters for a specific job.
     */
    List<MotivationLetter> findByJobListingIdAndTenantIdOrderByUpdatedAtDesc(
            UUID jobListingId, UUID tenantId);

    /**
     * Find the latest letter for a user and job combination.
     */
    Optional<MotivationLetter> findTopByUserIdAndJobListingIdAndTenantIdOrderByVersionDesc(
            UUID userId, UUID jobListingId, UUID tenantId);

    /**
     * Find letters by status.
     */
    Page<MotivationLetter> findByUserIdAndTenantIdAndStatusOrderByUpdatedAtDesc(
            UUID userId, UUID tenantId, LetterStatus status, Pageable pageable);

    /**
     * Count letters for a user.
     */
    long countByUserIdAndTenantId(UUID userId, UUID tenantId);

    /**
     * Check if letter exists for user and job.
     */
    boolean existsByUserIdAndJobListingIdAndTenantId(UUID userId, UUID jobListingId, UUID tenantId);

    /**
     * Get the next version number for a user+job combination.
     */
    @Query("SELECT COALESCE(MAX(m.version), 0) + 1 FROM MotivationLetter m " +
           "WHERE m.user.id = :userId AND m.jobListing.id = :jobId AND m.tenantId = :tenantId")
    int getNextVersion(
            @Param("userId") UUID userId,
            @Param("jobId") UUID jobId,
            @Param("tenantId") UUID tenantId);

    /**
     * Find all versions of a letter for a user+job.
     */
    List<MotivationLetter> findByUserIdAndJobListingIdAndTenantIdOrderByVersionDesc(
            UUID userId, UUID jobListingId, UUID tenantId);

    /**
     * Delete all letters for a user (for account deletion).
     */
    void deleteByUserIdAndTenantId(UUID userId, UUID tenantId);
}
