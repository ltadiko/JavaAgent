package com.jobagent.jobagent.jobsearch.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Sprint 5.4 — Job match result with score.
 *
 * Represents how well a job matches a user's CV skills.
 */
@Builder
@Schema(description = "Job match result showing how well a job matches the user's CV profile")
public record JobMatchScore(
        @Schema(description = "Job listing identifier", example = "550e8400-e29b-41d4-a716-446655440000", format = "uuid")
        UUID jobId,

        @Schema(description = "Job title", example = "Senior Java Developer")
        String title,

        @Schema(description = "Company name", example = "TechCorp GmbH")
        String company,

        @Schema(description = "Job location", example = "Berlin, Germany")
        String location,

        @Schema(description = "Percentage match between CV skills and job requirements (0-100)", example = "85", minimum = "0", maximum = "100", format = "int32")
        int matchPercentage,

        @ArraySchema(schema = @Schema(description = "Skill that matched between CV and job", example = "Java"))
        List<String> matchedSkills,

        @ArraySchema(schema = @Schema(description = "Skill required by job but missing from CV", example = "Kubernetes"))
        List<String> missingSkills,

        @Schema(description = "Full job listing details")
        JobListingResponse job
) {
    /**
     * Check if this is a strong match (>= 70%).
     */
    public boolean isStrongMatch() {
        return matchPercentage >= 70;
    }

    /**
     * Check if this is a good match (>= 50%).
     */
    public boolean isGoodMatch() {
        return matchPercentage >= 50;
    }

    /**
     * Get match quality description.
     */
    public String getMatchQuality() {
        if (matchPercentage >= 80) return "Excellent";
        if (matchPercentage >= 70) return "Strong";
        if (matchPercentage >= 50) return "Good";
        if (matchPercentage >= 30) return "Partial";
        return "Low";
    }
}
