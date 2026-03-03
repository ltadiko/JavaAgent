package com.jobagent.jobagent.jobsearch.dto;

import com.jobagent.jobagent.jobsearch.model.EmploymentType;
import com.jobagent.jobagent.jobsearch.model.RemoteType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.List;

/**
 * Sprint 5.4 — Request DTO for job search with filters.
 */
@Schema(description = "Request payload for searching jobs with optional filters")
public record JobSearchRequest(
        @Schema(description = "Free-text keyword to search across title, company, and description", example = "Java developer")
        String keyword,

        @Schema(description = "Filter by job title", example = "Backend Engineer")
        String title,

        @Schema(description = "Filter by company name", example = "Google")
        String company,

        @Schema(description = "Filter by location", example = "Berlin")
        String location,

        @ArraySchema(schema = @Schema(description = "Required skill", example = "Spring Boot"))
        List<String> skills,

        @Schema(description = "Filter by employment type", example = "FULL_TIME")
        EmploymentType employmentType,

        @Schema(description = "Filter by remote work type", example = "HYBRID")
        RemoteType remoteType,

        @Schema(description = "Minimum salary filter", example = "50000.00", format = "decimal")
        BigDecimal salaryMin,

        @Schema(description = "Maximum salary filter", example = "100000.00", format = "decimal")
        BigDecimal salaryMax,

        @Schema(description = "Zero-based page number", example = "0", minimum = "0", maximum = "1000", format = "int32")
        @Min(0) @Max(1000) Integer page,

        @Schema(description = "Page size (number of results per page)", example = "20", minimum = "1", maximum = "100", format = "int32")
        @Min(1) @Max(100) Integer size
) {
    /**
     * Default constructor with sensible defaults.
     */
    public JobSearchRequest {
        if (page == null) page = 0;
        if (size == null) size = 20;
    }

    /**
     * Check if any search criteria is specified.
     */
    public boolean hasAnyCriteria() {
        return keyword != null || title != null || company != null ||
               location != null || (skills != null && !skills.isEmpty()) ||
               employmentType != null || remoteType != null ||
               salaryMin != null || salaryMax != null;
    }
}
