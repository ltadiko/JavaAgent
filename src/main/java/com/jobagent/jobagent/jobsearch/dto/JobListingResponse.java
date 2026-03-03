package com.jobagent.jobagent.jobsearch.dto;

import com.jobagent.jobagent.jobsearch.model.EmploymentType;
import com.jobagent.jobagent.jobsearch.model.JobListing;
import com.jobagent.jobagent.jobsearch.model.JobStatus;
import com.jobagent.jobagent.jobsearch.model.RemoteType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Sprint 5.4 — Response DTO for job listings.
 */
@Builder
@Schema(description = "Detailed information about a job listing")
public record JobListingResponse(
        @Schema(description = "Unique job listing identifier", example = "550e8400-e29b-41d4-a716-446655440000", format = "uuid")
        UUID id,

        @Schema(description = "Job title", example = "Senior Java Developer")
        String title,

        @Schema(description = "Company name", example = "TechCorp GmbH")
        String company,

        @Schema(description = "Job location", example = "Berlin, Germany")
        String location,

        @Schema(description = "Full job description in plain text or HTML")
        String description,

        @Schema(description = "Job requirements and qualifications")
        String requirements,

        @ArraySchema(schema = @Schema(description = "Required skill", example = "Java"))
        List<String> skills,

        @Schema(description = "Minimum annual salary", example = "65000.00", format = "decimal")
        BigDecimal salaryMin,

        @Schema(description = "Maximum annual salary", example = "95000.00", format = "decimal")
        BigDecimal salaryMax,

        @Schema(description = "ISO 4217 salary currency code", example = "EUR")
        String salaryCurrency,

        @Schema(description = "Human-readable salary range string", example = "€65,000 – €95,000")
        String salaryRange,

        @Schema(description = "Type of employment", example = "FULL_TIME")
        EmploymentType employmentType,

        @Schema(description = "Remote work policy", example = "HYBRID")
        RemoteType remoteType,

        @Schema(description = "URL of the original job posting", example = "https://careers.techcorp.com/job/12345", format = "uri")
        String sourceUrl,

        @Schema(description = "Current status of the job listing", example = "ACTIVE")
        JobStatus status,

        @Schema(description = "Job listing creation timestamp", example = "2026-03-01T08:00:00Z", format = "date-time")
        Instant createdAt,

        @Schema(description = "Job listing expiration timestamp", example = "2026-04-01T23:59:59Z", format = "date-time")
        Instant expiresAt,

        @Schema(description = "Whether the job is currently available for applications", example = "true")
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
