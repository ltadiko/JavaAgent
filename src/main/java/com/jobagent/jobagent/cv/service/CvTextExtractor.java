package com.jobagent.jobagent.cv.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Sprint 4.1 — Extract text content from CV files (PDF, DOCX) using Apache Tika.
 */
@Service
@Slf4j
public class CvTextExtractor {

    private final Tika tika;

    public CvTextExtractor() {
        this.tika = new Tika();
        // Set reasonable limits
        this.tika.setMaxStringLength(100_000); // 100KB max text
    }

    /**
     * Extract text content from a file.
     *
     * @param inputStream the file content
     * @param fileName    the original filename (for content type detection)
     * @return extracted text content
     * @throws CvParsingException if extraction fails
     */
    public String extractText(InputStream inputStream, String fileName) {
        try {
            String text = tika.parseToString(inputStream);

            if (text == null || text.isBlank()) {
                throw new CvParsingException("No text content could be extracted from: " + fileName);
            }

            log.debug("Extracted {} characters from {}", text.length(), fileName);
            return text.trim();

        } catch (IOException e) {
            log.error("IO error extracting text from {}: {}", fileName, e.getMessage());
            throw new CvParsingException("Failed to read file: " + fileName, e);
        } catch (TikaException e) {
            log.error("Tika error extracting text from {}: {}", fileName, e.getMessage());
            throw new CvParsingException("Failed to parse file: " + fileName, e);
        }
    }

    /**
     * Custom exception for CV parsing errors.
     */
    public static class CvParsingException extends RuntimeException {
        public CvParsingException(String message) {
            super(message);
        }

        public CvParsingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
