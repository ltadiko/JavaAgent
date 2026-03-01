package com.jobagent.jobagent.jobsearch.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Sprint 5.4 — Job match result with score.
 *
 * Represents how well a job matches a user's CV skills.
 */
@Builder
public record JobMatchScore(
        UUID jobId,
        String title,
        String company,
        String location,
        int matchPercentage,
        List<String> matchedSkills,
        List<String> missingSkills,
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
