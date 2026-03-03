package com.jobagent.jobagent.motivation.dto;

import com.jobagent.jobagent.motivation.model.LetterTone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Sprint 6.5 — Request to generate a new motivation letter.
 */
@Schema(description = "Request payload for AI-powered motivation letter generation")
public record GenerateLetterRequest(
        @Schema(description = "Target job listing identifier", example = "550e8400-e29b-41d4-a716-446655440000", format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Job ID is required")
        UUID jobId,

        @Schema(description = "CV to use for letter generation (uses active CV if not provided)", example = "660e8400-e29b-41d4-a716-446655440000", format = "uuid")
        UUID cvId,

        @Schema(description = "Desired tone of the letter", example = "PROFESSIONAL", defaultValue = "PROFESSIONAL")
        LetterTone tone,

        @Schema(description = "ISO 639-1 language code for the letter", example = "en", defaultValue = "en")
        String language,

        @Schema(description = "Additional instructions for the AI generator", example = "Emphasize my cloud architecture experience")
        String additionalInstructions
) {
    public GenerateLetterRequest {
        if (tone == null) tone = LetterTone.PROFESSIONAL;
        if (language == null || language.isBlank()) language = "en";
    }
}
