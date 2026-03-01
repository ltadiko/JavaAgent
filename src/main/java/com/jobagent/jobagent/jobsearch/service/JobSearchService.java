package com.jobagent.jobagent.jobsearch.service;

import com.jobagent.jobagent.common.exception.ResourceNotFoundException;
import com.jobagent.jobagent.common.multitenancy.TenantContext;
import com.jobagent.jobagent.jobsearch.dto.CreateJobRequest;
import com.jobagent.jobagent.jobsearch.dto.JobListingResponse;
import com.jobagent.jobagent.jobsearch.dto.JobSearchRequest;
import com.jobagent.jobagent.jobsearch.model.JobListing;
import com.jobagent.jobagent.jobsearch.model.JobStatus;
import com.jobagent.jobagent.jobsearch.repository.JobListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Sprint 5.5 — Service for job search operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class JobSearchService {

    private final JobListingRepository jobListingRepository;

    /**
     * Get all active jobs for current tenant with pagination.
     */
    public Page<JobListingResponse> getActiveJobs(int page, int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return jobListingRepository.findByTenantIdAndStatus(tenantId, JobStatus.ACTIVE, pageable)
                .map(JobListingResponse::from);
    }

    /**
     * Search jobs with filters.
     */
    public Page<JobListingResponse> searchJobs(JobSearchRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(
                request.page(),
                request.size(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // Full-text search if keyword is provided
        if (request.keyword() != null && !request.keyword().isBlank()) {
            return jobListingRepository.fullTextSearch(tenantId, request.keyword(), pageable)
                    .map(JobListingResponse::from);
        }

        // Advanced search with multiple criteria
        return jobListingRepository.advancedSearch(
                tenantId,
                JobStatus.ACTIVE,
                request.title(),
                request.company(),
                request.location(),
                pageable
        ).map(JobListingResponse::from);
    }

    /**
     * Get job by ID.
     */
    public JobListingResponse getJobById(UUID jobId) {
        UUID tenantId = TenantContext.requireTenantId();

        return jobListingRepository.findByIdAndTenantId(jobId, tenantId)
                .map(JobListingResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
    }

    /**
     * Get job entity by ID (internal use).
     */
    public Optional<JobListing> findJobById(UUID jobId) {
        UUID tenantId = TenantContext.requireTenantId();
        return jobListingRepository.findByIdAndTenantId(jobId, tenantId);
    }

    /**
     * Create a new job listing.
     */
    @Transactional
    public JobListingResponse createJob(CreateJobRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        // Check for duplicate external ID
        if (request.externalId() != null) {
            Optional<JobListing> existing = jobListingRepository
                    .findByTenantIdAndExternalId(tenantId, request.externalId());
            if (existing.isPresent()) {
                log.info("Job with external ID {} already exists, returning existing", request.externalId());
                return JobListingResponse.from(existing.get());
            }
        }

        JobListing job = JobListing.builder()
                .tenantId(tenantId)
                .title(request.title())
                .company(request.company())
                .location(request.location())
                .description(request.description())
                .requirements(request.requirements())
                .skills(normalizeSkills(request.skills()))
                .salaryMin(request.salaryMin())
                .salaryMax(request.salaryMax())
                .salaryCurrency(request.salaryCurrency())
                .employmentType(request.employmentType())
                .remoteType(request.remoteType())
                .sourceUrl(request.sourceUrl())
                .externalId(request.externalId())
                .expiresAt(request.expiresAt())
                .status(JobStatus.ACTIVE)
                .build();

        JobListing saved = jobListingRepository.save(job);
        log.info("Created job listing: {} at {}", saved.getTitle(), saved.getCompany());

        return JobListingResponse.from(saved);
    }

    /**
     * Update job status.
     */
    @Transactional
    public JobListingResponse updateJobStatus(UUID jobId, JobStatus newStatus) {
        UUID tenantId = TenantContext.requireTenantId();

        JobListing job = jobListingRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        job.setStatus(newStatus);
        JobListing saved = jobListingRepository.save(job);

        log.info("Updated job {} status to {}", jobId, newStatus);
        return JobListingResponse.from(saved);
    }

    /**
     * Mark expired jobs as expired (scheduled task).
     */
    @Transactional
    public int markExpiredJobs() {
        List<JobListing> expiredJobs = jobListingRepository.findExpiredJobs(Instant.now());

        for (JobListing job : expiredJobs) {
            job.setStatus(JobStatus.EXPIRED);
        }

        if (!expiredJobs.isEmpty()) {
            jobListingRepository.saveAll(expiredJobs);
            log.info("Marked {} jobs as expired", expiredJobs.size());
        }

        return expiredJobs.size();
    }

    /**
     * Count active jobs for current tenant.
     */
    public long countActiveJobs() {
        UUID tenantId = TenantContext.requireTenantId();
        return jobListingRepository.countByTenantIdAndStatus(tenantId, JobStatus.ACTIVE);
    }

    /**
     * Normalize skills to lowercase for consistent matching.
     */
    private List<String> normalizeSkills(List<String> skills) {
        if (skills == null) return List.of();
        return skills.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase())
                .distinct()
                .toList();
    }
}
