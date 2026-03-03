package com.jobagent.jobagent.cv.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 3.6 — Response DTO for CV upload.
 */
@Schema(description = "Response returned after a CV file is uploaded")
public record CvUploadResponse(
    @Schema(description = "Unique CV identifier", example = "550e8400-e29b-41d4-a716-446655440000", format = "uuid")
    UUID id,

    @Schema(description = "Original file name", example = "john-doe-cv.pdf")
    String fileName,

    @Schema(description = "MIME type of the uploaded file", example = "application/pdf")
    String contentType,

    @Schema(description = "File size in bytes", example = "245760", format = "int64")
    Long fileSize,

    @Schema(description = "CV processing status", example = "UPLOADED", allowableValues = {"UPLOADED", "PARSING", "PARSED", "FAILED"})
    String status,

    @Schema(description = "Upload timestamp", example = "2026-03-03T10:15:30Z", format = "date-time")
    Instant createdAt
) {}
