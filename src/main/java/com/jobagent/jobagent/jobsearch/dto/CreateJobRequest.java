package com.jobagent.jobagent.jobsearch.dto;

import com.jobagent.jobagent.jobsearch.model.EmploymentType;
import com.jobagent.jobagent.jobsearch.model.RemoteType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Sprint 5.4 — Request DTO for creating a new job listing.
 */
@Schema(description = "Request payload for creating a new job listing")
public record CreateJobRequest(
        @Schema(description = "Job title", example = "Senior Java Developer", maxLength = 500, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Title is required")
        @Size(max = 500, message = "Title must not exceed 500 characters")
        String title,

        @Schema(description = "Company name", example = "TechCorp GmbH", maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Company is required")
        @Size(max = 255, message = "Company must not exceed 255 characters")
        String company,

        @Schema(description = "Job location", example = "Berlin, Germany", maxLength = 255)
        @Size(max = 255, message = "Location must not exceed 255 characters")
        String location,

        @Schema(description = "Full job description")
        String description,

        @Schema(description = "Job requirements and qualifications")
        String requirements,

        @ArraySchema(schema = @Schema(description = "Required skill", example = "Java"))
        List<String> skills,

        @Schema(description = "Minimum annual salary", example = "65000.00", format = "decimal")
        BigDecimal salaryMin,

        @Schema(description = "Maximum annual salary", example = "95000.00", format = "decimal")
        BigDecimal salaryMax,

        @Schema(description = "ISO 4217 salary currency code (defaults to EUR)", example = "EUR")
        String salaryCurrency,

        @Schema(description = "Type of employment", example = "FULL_TIME")
        EmploymentType employmentType,

        @Schema(description = "Remote work policy", example = "HYBRID")
        RemoteType remoteType,

        @Schema(description = "URL of the original job posting", example = "https://careers.techcorp.com/job/12345", maxLength = 2000, format = "uri")
        @Size(max = 2000, message = "Source URL must not exceed 2000 characters")
        String sourceUrl,

        @Schema(description = "External identifier from the source system", example = "EXT-12345")
        String externalId,

        @Schema(description = "Job listing expiration date", example = "2026-04-01T23:59:59Z", format = "date-time")
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
