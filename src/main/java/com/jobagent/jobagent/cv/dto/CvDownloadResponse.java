package com.jobagent.jobagent.cv.dto;

/**
 * Sprint 3.6 — Response DTO for presigned download URL.
 */
public record CvDownloadResponse(
    String downloadUrl,
    int expiresInMinutes
) {}
