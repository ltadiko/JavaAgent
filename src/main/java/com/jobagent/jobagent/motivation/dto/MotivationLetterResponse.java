package com.jobagent.jobagent.motivation.dto;

import com.jobagent.jobagent.motivation.model.LetterStatus;
import com.jobagent.jobagent.motivation.model.LetterTone;
import com.jobagent.jobagent.motivation.model.MotivationLetter;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 6.5 — Response DTO for motivation letters.
 */
@Builder
public record MotivationLetterResponse(
        UUID id,
        UUID jobId,
        String jobTitle,
        String company,
        UUID cvId,
        String content,
        String editedContent,
        LetterStatus status,
        LetterTone tone,
        String language,
        Integer wordCount,
        Integer version,
        boolean isEdited,
        Instant generatedAt,
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
