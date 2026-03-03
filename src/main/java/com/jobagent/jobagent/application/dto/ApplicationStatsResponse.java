package com.jobagent.jobagent.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Sprint 7.4 — Application statistics response.
 */
@Schema(description = "Aggregated application statistics for the user")
public record ApplicationStatsResponse(
        @Schema(description = "Total number of applications", example = "25", format = "int64")
        long total,

        @Schema(description = "Number of draft applications", example = "3", format = "int64")
        long drafts,

        @Schema(description = "Number of pending applications", example = "5", format = "int64")
        long pending,

        @Schema(description = "Number of sent applications", example = "10", format = "int64")
        long sent,

        @Schema(description = "Number of applications with interview stage", example = "4", format = "int64")
        long interviews,

        @Schema(description = "Number of applications with job offers", example = "2", format = "int64")
        long offers,

        @Schema(description = "Number of rejected applications", example = "1", format = "int64")
        long rejected,

        @Schema(description = "Breakdown of applications by status", example = "{\"DRAFT\": 3, \"SUBMITTED\": 5, \"SENT\": 10}")
        Map<String, Long> byStatus
) {}
