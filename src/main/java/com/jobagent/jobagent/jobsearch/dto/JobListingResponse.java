package com.jobagent.jobagent.jobsearch.dto;

import com.jobagent.jobagent.jobsearch.model.EmploymentType;
import com.jobagent.jobagent.jobsearch.model.JobListing;
import com.jobagent.jobagent.jobsearch.model.JobStatus;
import com.jobagent.jobagent.jobsearch.model.RemoteType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Sprint 5.4 — Response DTO for job listings.
 */
@Builder
public record JobListingResponse(
        UUID id,
        String title,
        String company,
        String location,
        String description,
        String requirements,
        List<String> skills,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        String salaryRange,
        EmploymentType employmentType,
        RemoteType remoteType,
        String sourceUrl,
        JobStatus status,
        Instant createdAt,
        Instant expiresAt,
        boolean available
) {
    /**
     * Factory method to create response from entity.
     */
    public static JobListingResponse from(JobListing job) {
        return JobListingResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .company(job.getCompany())
                .location(job.getLocation())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .skills(job.getSkills())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .salaryCurrency(job.getSalaryCurrency())
                .salaryRange(job.getSalaryRange())
                .employmentType(job.getEmploymentType())
                .remoteType(job.getRemoteType())
                .sourceUrl(job.getSourceUrl())
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .expiresAt(job.getExpiresAt())
                .available(job.isAvailable())
                .build();
    }
}
