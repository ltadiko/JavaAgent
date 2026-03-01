package com.jobagent.jobagent.jobsearch.dto;

import com.jobagent.jobagent.jobsearch.model.EmploymentType;
import com.jobagent.jobagent.jobsearch.model.RemoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Sprint 5.4 — Request DTO for creating a new job listing.
 */
public record CreateJobRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 500, message = "Title must not exceed 500 characters")
        String title,

        @NotBlank(message = "Company is required")
        @Size(max = 255, message = "Company must not exceed 255 characters")
        String company,

        @Size(max = 255, message = "Location must not exceed 255 characters")
        String location,

        String description,

        String requirements,

        List<String> skills,

        BigDecimal salaryMin,

        BigDecimal salaryMax,

        String salaryCurrency,

        EmploymentType employmentType,

        RemoteType remoteType,

        @Size(max = 2000, message = "Source URL must not exceed 2000 characters")
        String sourceUrl,

        String externalId,

        Instant expiresAt
) {
    /**
     * Apply defaults.
     */
    public CreateJobRequest {
        if (salaryCurrency == null || salaryCurrency.isBlank()) {
            salaryCurrency = "EUR";
        }
    }
}
