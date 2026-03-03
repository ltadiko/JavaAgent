package com.jobagent.jobagent.dashboard.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.Instant;
import java.util.List;

/**
 * Sprint 8.2 — CV summary for dashboard.
 */
@Builder
@Schema(description = "CV summary information for the dashboard")
public record CvSummary(
        @Schema(description = "Total number of uploaded CVs", example = "3", format = "int32")
        int count,

        @Schema(description = "Timestamp of the most recent CV parsing", example = "2026-03-01T14:30:00Z", format = "date-time")
        Instant latestParsedAt,

        @Schema(description = "Total number of skills extracted from the active CV", example = "15", format = "int32")
        int skillsCount,

        @ArraySchema(schema = @Schema(description = "Top skill extracted from CV", example = "Java"))
        List<String> topSkills
) {}
