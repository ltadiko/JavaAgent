package com.jobagent.jobagent.cv.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Sprint 3.6 — Response DTO for presigned download URL.
 */
@Schema(description = "Response containing a presigned URL for CV file download")
public record CvDownloadResponse(
    @Schema(description = "Presigned URL for downloading the CV file", example = "https://minio.local:9000/cv-bucket/abc123?X-Amz-Signature=...", format = "uri")
    String downloadUrl,

    @Schema(description = "URL validity duration in minutes", example = "15", format = "int32")
    int expiresInMinutes
) {}
