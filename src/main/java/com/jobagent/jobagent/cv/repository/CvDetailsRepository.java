package com.jobagent.jobagent.cv.repository;

import com.jobagent.jobagent.cv.model.CvDetails;
import com.jobagent.jobagent.cv.model.CvStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Sprint 3.3 — Repository for CV details.
 */
public interface CvDetailsRepository extends JpaRepository<CvDetails, UUID> {

    /**
     * Find the active CV for a user.
     */
    Optional<CvDetails> findByUserIdAndActiveTrue(UUID userId);

    /**
     * Find all CVs for a user, ordered by creation date (newest first).
     */
    List<CvDetails> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Deactivate all CVs for a user (before uploading new one).
     */
    @Modifying
    @Query("UPDATE CvDetails c SET c.active = false WHERE c.user.id = :userId AND c.active = true")
    int deactivateAllByUserId(@Param("userId") UUID userId);

    /**
     * Find CVs by tenant (for admin purposes).
     */
    List<CvDetails> findByTenantId(UUID tenantId);

    /**
     * Sprint 8.0 — Find CVs for a user with pagination (for dashboard).
     */
    Page<CvDetails> findByUserIdAndTenantIdOrderByCreatedAtDesc(
            UUID userId, UUID tenantId, Pageable pageable);

    /**
     * Sprint 5.6 — Find the latest parsed CV for a user (for job matching).
     */
    Optional<CvDetails> findTopByUserIdAndTenantIdAndStatusOrderByCreatedAtDesc(
            UUID userId, UUID tenantId, CvStatus status);
}
