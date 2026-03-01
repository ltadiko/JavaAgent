package com.jobagent.jobagent.cv.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 3.6 — Summary DTO for CV details.
 */
public record CvSummaryResponse(
    UUID id,
    String fileName,
    String contentType,
    Long fileSize,
    String status,
    Boolean active,
    Instant createdAt,
    Instant parsedAt
) {}
