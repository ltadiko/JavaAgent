package com.jobagent.jobagent.motivation.dto;

import com.jobagent.jobagent.motivation.model.LetterTone;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Sprint 6.5 — Request to generate a new motivation letter.
 */
public record GenerateLetterRequest(
        @NotNull(message = "Job ID is required")
        UUID jobId,
        UUID cvId,
        LetterTone tone,
        String language,
        String additionalInstructions
) {
    public GenerateLetterRequest {
        if (tone == null) tone = LetterTone.PROFESSIONAL;
        if (language == null || language.isBlank()) language = "en";
    }
}
