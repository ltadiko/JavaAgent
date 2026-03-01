package com.jobagent.jobagent.jobsearch.repository;

import com.jobagent.jobagent.jobsearch.model.JobListing;
import com.jobagent.jobagent.jobsearch.model.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Sprint 5.2 — Repository for JobListing entity.
 *
 * Provides search, filter, and matching queries for job listings.
 */
@Repository
public interface JobListingRepository extends JpaRepository<JobListing, UUID> {

    /**
     * Find all jobs for a tenant.
     */
    Page<JobListing> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Find active jobs for a tenant.
     */
    Page<JobListing> findByTenantIdAndStatus(UUID tenantId, JobStatus status, Pageable pageable);

    /**
     * Find job by ID and tenant (for security).
     */
    Optional<JobListing> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Search jobs by title (case-insensitive).
     */
    @Query("SELECT j FROM JobListing j WHERE j.tenantId = :tenantId " +
           "AND j.status = :status " +
           "AND LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    Page<JobListing> searchByTitle(
            @Param("tenantId") UUID tenantId,
            @Param("status") JobStatus status,
            @Param("title") String title,
            Pageable pageable);

    /**
     * Search jobs by company.
     */
    Page<JobListing> findByTenantIdAndStatusAndCompanyContainingIgnoreCase(
            UUID tenantId, JobStatus status, String company, Pageable pageable);

    /**
     * Search jobs by location.
     */
    Page<JobListing> findByTenantIdAndStatusAndLocationContainingIgnoreCase(
            UUID tenantId, JobStatus status, String location, Pageable pageable);

    /**
     * Find jobs that contain any of the specified skills.
     * Uses native query with JSONB containment operator.
     */
    @Query(value = """
            SELECT * FROM job_listings j 
            WHERE j.tenant_id = :tenantId 
            AND j.status = :status
            AND j.skills ?| ARRAY[:skills]
            ORDER BY j.created_at DESC
            """, nativeQuery = true)
    Page<JobListing> findBySkillsContaining(
            @Param("tenantId") UUID tenantId,
            @Param("status") String status,
            @Param("skills") String[] skills,
            Pageable pageable);

    /**
     * Full-text search on title and description.
     */
    @Query(value = """
            SELECT * FROM job_listings j 
            WHERE j.tenant_id = :tenantId 
            AND j.status = 'ACTIVE'
            AND to_tsvector('english', coalesce(j.title, '') || ' ' || coalesce(j.description, '')) 
                @@ plainto_tsquery('english', :searchTerm)
            ORDER BY j.created_at DESC
            """, nativeQuery = true)
    Page<JobListing> fullTextSearch(
            @Param("tenantId") UUID tenantId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable);

    /**
     * Count active jobs for a tenant.
     */
    long countByTenantIdAndStatus(UUID tenantId, JobStatus status);

    /**
     * Find expired jobs that need status update.
     */
    @Query("SELECT j FROM JobListing j WHERE j.status = 'ACTIVE' " +
           "AND j.expiresAt IS NOT NULL AND j.expiresAt < :now")
    List<JobListing> findExpiredJobs(@Param("now") Instant now);

    /**
     * Find by external ID for deduplication during import.
     */
    Optional<JobListing> findByTenantIdAndExternalId(UUID tenantId, String externalId);

    /**
     * Advanced search with multiple criteria.
     */
    @Query("""
            SELECT j FROM JobListing j WHERE j.tenantId = :tenantId 
            AND j.status = :status
            AND (:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%')))
            AND (:company IS NULL OR LOWER(j.company) LIKE LOWER(CONCAT('%', :company, '%')))
            AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%')))
            ORDER BY j.createdAt DESC
            """)
    Page<JobListing> advancedSearch(
            @Param("tenantId") UUID tenantId,
            @Param("status") JobStatus status,
            @Param("title") String title,
            @Param("company") String company,
            @Param("location") String location,
            Pageable pageable);
}
