package com.jobagent.jobagent.motivation.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 6.10 — Unit tests for MotivationLetter entity.
 */
@DisplayName("MotivationLetter Entity Tests")
class MotivationLetterTest {

    @Test
    @DisplayName("Builder should set default values")
    void builder_shouldSetDefaults() {
        MotivationLetter letter = MotivationLetter.builder().build();

        assertThat(letter.getStatus()).isEqualTo(LetterStatus.DRAFT);
        assertThat(letter.getTone()).isEqualTo(LetterTone.PROFESSIONAL);
        assertThat(letter.getLanguage()).isEqualTo("en");
        assertThat(letter.getVersion()).isEqualTo(1);
        assertThat(letter.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("getFinalContent() returns edited if available")
    void getFinalContent_withEdited_returnsEdited() {
        MotivationLetter letter = MotivationLetter.builder()
                .generatedContent("Original content")
                .editedContent("Edited content")
                .build();

        assertThat(letter.getFinalContent()).isEqualTo("Edited content");
    }

    @Test
    @DisplayName("getFinalContent() returns generated if no edits")
    void getFinalContent_withoutEdited_returnsGenerated() {
        MotivationLetter letter = MotivationLetter.builder()
                .generatedContent("Generated content")
                .build();

        assertThat(letter.getFinalContent()).isEqualTo("Generated content");
    }

    @Test
    @DisplayName("getFinalContent() ignores blank edits")
    void getFinalContent_withBlankEdited_returnsGenerated() {
        MotivationLetter letter = MotivationLetter.builder()
                .generatedContent("Generated content")
                .editedContent("   ")
                .build();

        assertThat(letter.getFinalContent()).isEqualTo("Generated content");
    }

    @Test
    @DisplayName("isEdited() returns true when edited content exists")
    void isEdited_withContent_returnsTrue() {
        MotivationLetter letter = MotivationLetter.builder()
                .editedContent("User edits")
                .build();

        assertThat(letter.isEdited()).isTrue();
    }

    @Test
    @DisplayName("isEdited() returns false when no edits")
    void isEdited_withoutContent_returnsFalse() {
        MotivationLetter letter = MotivationLetter.builder().build();

        assertThat(letter.isEdited()).isFalse();
    }

    @Test
    @DisplayName("calculateWordCount() counts words correctly")
    void calculateWordCount_withContent_returnsCount() {
        MotivationLetter letter = MotivationLetter.builder()
                .generatedContent("This is a test letter with seven words.")
                .build();

        assertThat(letter.calculateWordCount()).isEqualTo(8);
    }

    @Test
    @DisplayName("calculateWordCount() returns zero for empty content")
    void calculateWordCount_withEmpty_returnsZero() {
        MotivationLetter letter = MotivationLetter.builder().build();

        assertThat(letter.calculateWordCount()).isZero();
    }

    @Test
    @DisplayName("calculateWordCount() handles whitespace")
    void calculateWordCount_withWhitespace_handlesCorrectly() {
        MotivationLetter letter = MotivationLetter.builder()
                .generatedContent("  Word one   word two  word three  ")
                .build();

        assertThat(letter.calculateWordCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("updateWordCount() updates the field")
    void updateWordCount_updatesField() {
        MotivationLetter letter = MotivationLetter.builder()
                .generatedContent("Five words in this sentence")
                .build();

        letter.updateWordCount();

        assertThat(letter.getWordCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("Can set all fields via builder")
    void builder_canSetAllFields() {
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant now = Instant.now();

        MotivationLetter letter = MotivationLetter.builder()
                .id(id)
                .tenantId(tenantId)
                .generatedContent("Generated text")
                .editedContent("Edited text")
                .additionalInstructions("Be creative")
                .status(LetterStatus.SENT)
                .tone(LetterTone.CREATIVE)
                .language("de")
                .wordCount(100)
                .pdfS3Key("s3://bucket/letter.pdf")
                .version(2)
                .generatedAt(now)
                .build();

        assertThat(letter.getId()).isEqualTo(id);
        assertThat(letter.getTenantId()).isEqualTo(tenantId);
        assertThat(letter.getGeneratedContent()).isEqualTo("Generated text");
        assertThat(letter.getEditedContent()).isEqualTo("Edited text");
        assertThat(letter.getAdditionalInstructions()).isEqualTo("Be creative");
        assertThat(letter.getStatus()).isEqualTo(LetterStatus.SENT);
        assertThat(letter.getTone()).isEqualTo(LetterTone.CREATIVE);
        assertThat(letter.getLanguage()).isEqualTo("de");
        assertThat(letter.getWordCount()).isEqualTo(100);
        assertThat(letter.getPdfS3Key()).isEqualTo("s3://bucket/letter.pdf");
        assertThat(letter.getVersion()).isEqualTo(2);
        assertThat(letter.getGeneratedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Status enum has expected values")
    void letterStatus_hasExpectedValues() {
        assertThat(LetterStatus.values()).containsExactly(
                LetterStatus.DRAFT,
                LetterStatus.GENERATED,
                LetterStatus.EDITED,
                LetterStatus.SENT,
                LetterStatus.ARCHIVED
        );
    }

    @Test
    @DisplayName("Tone enum has expected values")
    void letterTone_hasExpectedValues() {
        assertThat(LetterTone.values()).containsExactly(
                LetterTone.FORMAL,
                LetterTone.PROFESSIONAL,
                LetterTone.CREATIVE,
                LetterTone.CONFIDENT,
                LetterTone.ENTHUSIASTIC
        );
    }
}
