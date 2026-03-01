package com.jobagent.jobagent.jobsearch.dto;

import com.jobagent.jobagent.jobsearch.model.EmploymentType;
import com.jobagent.jobagent.jobsearch.model.RemoteType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.List;

/**
 * Sprint 5.4 — Request DTO for job search with filters.
 */
public record JobSearchRequest(
        String keyword,
        String title,
        String company,
        String location,
        List<String> skills,
        EmploymentType employmentType,
        RemoteType remoteType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        @Min(0) @Max(1000) Integer page,
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
