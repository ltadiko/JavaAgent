package com.jobagent.jobagent.cv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprint 4.1 — Unit tests for CvTextExtractor.
 */
@DisplayName("CvTextExtractor Tests")
class CvTextExtractorTest {

    private CvTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new CvTextExtractor();
    }

    @Test
    @DisplayName("Extract text from plain text file")
    void extractText_plainText_success() {
        String content = "John Doe\nSoftware Engineer\nSkills: Java, Python";
        InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        String result = extractor.extractText(stream, "resume.txt");

        assertThat(result).contains("John Doe");
        assertThat(result).contains("Software Engineer");
        assertThat(result).contains("Java");
    }

    @Test
    @DisplayName("Extract text handles empty content gracefully")
    void extractText_emptyContent_throwsException() {
        InputStream stream = new ByteArrayInputStream(new byte[0]);

        assertThatThrownBy(() -> extractor.extractText(stream, "empty.pdf"))
                .isInstanceOf(CvTextExtractor.CvParsingException.class)
                .hasMessageContaining("No text content");
    }

    @Test
    @DisplayName("Extract text trims whitespace")
    void extractText_withWhitespace_trimmed() {
        String content = "   \n\n  John Doe  \n\n   ";
        InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        String result = extractor.extractText(stream, "resume.txt");

        assertThat(result).isEqualTo("John Doe");
    }
}
