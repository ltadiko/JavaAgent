package com.jobagent.jobagent.motivation.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Sprint 6.5 — Request to update motivation letter content.
 */
public record UpdateLetterRequest(
        @NotBlank(message = "Content is required")
        String content
) {}
