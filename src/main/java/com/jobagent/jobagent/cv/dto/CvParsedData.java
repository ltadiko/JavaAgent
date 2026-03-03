package com.jobagent.jobagent.cv.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Sprint 4.2 — Structured data extracted from a CV by AI parsing.
 */
@Schema(description = "Structured data extracted from a CV by AI-powered parsing")
public record CvParsedData(
    @Schema(description = "Candidate's full name", example = "John Doe")
    String fullName,

    @Schema(description = "Candidate's email address", example = "john.doe@example.com", format = "email")
    String email,

    @Schema(description = "Candidate's phone number", example = "+49 170 1234567")
    String phone,

    @Schema(description = "Current job title", example = "Senior Software Engineer")
    String currentTitle,

    @Schema(description = "Professional summary or objective statement", example = "Experienced backend developer with 10 years in Java/Spring ecosystems")
    String summary,

    @ArraySchema(schema = @Schema(description = "Technical or professional skill", example = "Java"))
    List<String> skills,

    @Schema(description = "List of work experience entries")
    List<ExperienceEntry> experience,

    @Schema(description = "List of education entries")
    List<EducationEntry> education,

    @ArraySchema(schema = @Schema(description = "Spoken language", example = "English"))
    List<String> languages,

    @ArraySchema(schema = @Schema(description = "Professional certification", example = "AWS Solutions Architect"))
    List<String> certifications
) {
    /**
     * Work experience entry.
     */
    @Schema(description = "A single work experience entry from the CV")
    public record ExperienceEntry(
        @Schema(description = "Company or employer name", example = "Acme Corp")
        String company,

        @Schema(description = "Job title held", example = "Senior Developer")
        String title,

        @Schema(description = "Work location", example = "Berlin, Germany")
        String location,

        @Schema(description = "Employment start date", example = "2020-01", format = "partial-date")
        String startDate,

        @Schema(description = "Employment end date (null if current)", example = "2024-06", format = "partial-date")
        String endDate,

        @Schema(description = "Role description and responsibilities", example = "Led a team of 5 developers building microservices")
        String description
    ) {}

    /**
     * Education entry.
     */
    @Schema(description = "A single education entry from the CV")
    public record EducationEntry(
        @Schema(description = "Educational institution name", example = "Technical University of Berlin")
        String institution,

        @Schema(description = "Degree obtained", example = "Master of Science")
        String degree,

        @Schema(description = "Field of study", example = "Computer Science")
        String field,

        @Schema(description = "Start date of studies", example = "2014-09", format = "partial-date")
        String startDate,

        @Schema(description = "End date of studies", example = "2016-07", format = "partial-date")
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
