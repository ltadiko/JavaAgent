package com.jobagent.jobagent.cv.dto;

import java.util.List;

/**
 * Sprint 4.2 — Structured data extracted from a CV by AI parsing.
 */
public record CvParsedData(
    String fullName,
    String email,
    String phone,
    String currentTitle,
    String summary,
    List<String> skills,
    List<ExperienceEntry> experience,
    List<EducationEntry> education,
    List<String> languages,
    List<String> certifications
) {
    /**
     * Work experience entry.
     */
    public record ExperienceEntry(
        String company,
        String title,
        String location,
        String startDate,
        String endDate,
        String description
    ) {}

    /**
     * Education entry.
     */
    public record EducationEntry(
        String institution,
        String degree,
        String field,
        String startDate,
        String endDate
    ) {}

    /**
     * Create an empty parsed data object.
     */
    public static CvParsedData empty() {
        return new CvParsedData(
            null, null, null, null, null,
            List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }
}
