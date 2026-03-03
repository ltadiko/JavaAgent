package com.jobagent.jobagent.motivation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Sprint 6.5 — Request to update motivation letter content.
 */
@Schema(description = "Request payload for updating motivation letter content")
public record UpdateLetterRequest(
        @Schema(description = "Updated letter content (plain text or markdown)", example = "Dear Hiring Manager,\n\nI am writing to express my interest in...", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Content is required")
        String content
) {}
