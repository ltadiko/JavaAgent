package com.jobagent.jobagent.cv.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 3.6 — Response DTO for CV upload.
 */
public record CvUploadResponse(
    UUID id,
    String fileName,
    String contentType,
    Long fileSize,
    String status,
    Instant createdAt
) {}
