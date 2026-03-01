package com.jobagent.jobagent.cv.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.jobagent.cv.dto.CvParsedData;
import com.jobagent.jobagent.cv.model.CvDetails;
import com.jobagent.jobagent.cv.model.CvStatus;
import com.jobagent.jobagent.cv.repository.CvDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 4.4 — CV Processing Service.
 *
 * <p>Orchestrates the CV parsing pipeline:
 * 1. Download file from storage
 * 2. Extract text (Tika)
 * 3. Parse with AI
 * 4. Update CV details with parsed data
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CvProcessingService {

    private final CvDetailsRepository cvDetailsRepository;
    private final FileStorageService fileStorageService;
    private final CvTextExtractor textExtractor;
    private final CvParserAgent parserAgent;
    private final ObjectMapper objectMapper;

    /**
     * Process a CV asynchronously.
     *
     * @param cvId the CV details ID
     */
    @Async("cvProcessingExecutor")
    public void processAsync(UUID cvId) {
        log.info("Starting async CV processing for: {}", cvId);
        process(cvId);
    }

    /**
     * Process a CV synchronously.
     *
     * @param cvId the CV details ID
     */
    @Transactional
    public void process(UUID cvId) {
        CvDetails cv = cvDetailsRepository.findById(cvId)
                .orElseThrow(() -> new IllegalArgumentException("CV not found: " + cvId));

        if (cv.getStatus() == CvStatus.PARSED) {
            log.info("CV {} already parsed, skipping", cvId);
            return;
        }

        try {
            // Update status to PARSING
            cv.setStatus(CvStatus.PARSING);
            cvDetailsRepository.save(cv);

            // Step 1: Download file
            log.debug("Downloading CV file: {}", cv.getS3Key());
            InputStream fileStream = fileStorageService.download(cv.getS3Key());

            // Step 2: Extract text
            log.debug("Extracting text from CV: {}", cv.getFileName());
            String cvText = textExtractor.extractText(fileStream, cv.getFileName());

            // Step 3: Parse with AI
            log.debug("Parsing CV with AI");
            CvParsedData parsedData = parserAgent.parse(cvText);

            // Step 4: Save parsed data
            String parsedJson = objectMapper.writeValueAsString(parsedData);
            cv.setParsedJson(parsedJson);
            cv.setStatus(CvStatus.PARSED);
            cv.setParsedAt(Instant.now());
            cv.setErrorMessage(null);
            cvDetailsRepository.save(cv);

            log.info("CV {} processed successfully", cvId);

        } catch (Exception e) {
            log.error("CV processing failed for {}: {}", cvId, e.getMessage(), e);

            cv.setStatus(CvStatus.FAILED);
            cv.setErrorMessage(e.getMessage());
            cvDetailsRepository.save(cv);
        }
    }

    /**
     * Get parsed data for a CV.
     *
     * @param cvId the CV details ID
     * @return parsed data or null if not yet parsed
     */
    @Transactional(readOnly = true)
    public CvParsedData getParsedData(UUID cvId) {
        CvDetails cv = cvDetailsRepository.findById(cvId)
                .orElseThrow(() -> new IllegalArgumentException("CV not found: " + cvId));

        if (cv.getParsedJson() == null) {
            return null;
        }

        try {
            return objectMapper.readValue(cv.getParsedJson(), CvParsedData.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize parsed CV data for {}: {}", cvId, e.getMessage());
            return null;
        }
    }

    /**
     * Reprocess a CV (e.g., after AI model update).
     *
     * @param cvId the CV details ID
     */
    @Transactional
    public void reprocess(UUID cvId) {
        CvDetails cv = cvDetailsRepository.findById(cvId)
                .orElseThrow(() -> new IllegalArgumentException("CV not found: " + cvId));

        cv.setStatus(CvStatus.UPLOADED);
        cv.setParsedJson(null);
        cv.setParsedAt(null);
        cv.setErrorMessage(null);
        cvDetailsRepository.save(cv);

        process(cvId);
    }
}
