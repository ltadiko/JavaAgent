package com.jobagent.jobagent.motivation.dto;

import com.jobagent.jobagent.motivation.model.LetterStatus;
import com.jobagent.jobagent.motivation.model.LetterTone;
import com.jobagent.jobagent.motivation.model.MotivationLetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 6.5 — Response DTO for motivation letters.
 */
@Builder
@Schema(description = "AI-generated motivation letter details")
public record MotivationLetterResponse(
        @Schema(description = "Unique letter identifier", example = "550e8400-e29b-41d4-a716-446655440000", format = "uuid")
        UUID id,

        @Schema(description = "Associated job listing identifier", example = "660e8400-e29b-41d4-a716-446655440000", format = "uuid")
        UUID jobId,

        @Schema(description = "Title of the target job", example = "Senior Java Developer")
        String jobTitle,

        @Schema(description = "Company name of the target job", example = "TechCorp GmbH")
        String company,

        @Schema(description = "CV used for letter generation", example = "770e8400-e29b-41d4-a716-446655440000", format = "uuid")
        UUID cvId,

        @Schema(description = "AI-generated letter content")
        String content,

        @Schema(description = "User-edited version of the letter content (null if not edited)")
        String editedContent,

        @Schema(description = "Current status of the letter", example = "GENERATED")
        LetterStatus status,

        @Schema(description = "Tone used for generation", example = "PROFESSIONAL")
        LetterTone tone,

        @Schema(description = "ISO 639-1 language code of the letter", example = "en")
        String language,

        @Schema(description = "Word count of the letter", example = "350", format = "int32")
        Integer wordCount,

        @Schema(description = "Version number (increments on regeneration)", example = "1", format = "int32")
        Integer version,

        @Schema(description = "Whether the user has edited the generated content", example = "false")
        boolean isEdited,

        @Schema(description = "Letter generation timestamp", example = "2026-03-03T10:15:30Z", format = "date-time")
        Instant generatedAt,

        @Schema(description = "Last update timestamp", example = "2026-03-03T10:20:00Z", format = "date-time")
        Instant updatedAt
) {
    /**
     * Create response from entity.
     */
    public static MotivationLetterResponse from(MotivationLetter letter) {
        return MotivationLetterResponse.builder()
                .id(letter.getId())
                .jobId(letter.getJobListing() != null ? letter.getJobListing().getId() : null)
                .jobTitle(letter.getJobListing() != null ? letter.getJobListing().getTitle() : null)
                .company(letter.getJobListing() != null ? letter.getJobListing().getCompany() : null)
                .cvId(letter.getCv() != null ? letter.getCv().getId() : null)
                .content(letter.getGeneratedContent())
                .editedContent(letter.getEditedContent())
                .status(letter.getStatus())
                .tone(letter.getTone())
                .language(letter.getLanguage())
                .wordCount(letter.getWordCount())
                .version(letter.getVersion())
                .isEdited(letter.isEdited())
                .generatedAt(letter.getGeneratedAt())
                .updatedAt(letter.getUpdatedAt())
                .build();
    }
}
