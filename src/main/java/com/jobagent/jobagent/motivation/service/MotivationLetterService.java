package com.jobagent.jobagent.motivation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.jobagent.auth.model.User;
import com.jobagent.jobagent.auth.repository.UserRepository;
import com.jobagent.jobagent.common.exception.ResourceNotFoundException;
import com.jobagent.jobagent.common.multitenancy.TenantContext;
import com.jobagent.jobagent.cv.dto.CvParsedData;
import com.jobagent.jobagent.cv.model.CvDetails;
import com.jobagent.jobagent.cv.model.CvStatus;
import com.jobagent.jobagent.cv.repository.CvDetailsRepository;
import com.jobagent.jobagent.jobsearch.model.JobListing;
import com.jobagent.jobagent.jobsearch.repository.JobListingRepository;
import com.jobagent.jobagent.motivation.dto.GenerateLetterRequest;
import com.jobagent.jobagent.motivation.dto.MotivationLetterResponse;
import com.jobagent.jobagent.motivation.dto.UpdateLetterRequest;
import com.jobagent.jobagent.motivation.model.LetterStatus;
import com.jobagent.jobagent.motivation.model.MotivationLetter;
import com.jobagent.jobagent.motivation.repository.MotivationLetterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Sprint 6.7 — Service for motivation letter operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MotivationLetterService {

    private final MotivationLetterRepository letterRepository;
    private final MotivationGeneratorAgent generatorAgent;
    private final UserRepository userRepository;
    private final CvDetailsRepository cvRepository;
    private final JobListingRepository jobRepository;
    private final ObjectMapper objectMapper;

    /**
     * Generate a new motivation letter.
     */
    @Transactional
    public MotivationLetterResponse generateLetter(UUID userId, GenerateLetterRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        log.info("Generating motivation letter for user {} and job {}", userId, request.jobId());

        // Load user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Load job
        JobListing job = jobRepository.findByIdAndTenantId(request.jobId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + request.jobId()));

        // Load CV (use provided or latest parsed)
        CvDetails cv = loadCv(userId, request.cvId(), tenantId);
        CvParsedData cvData = parseCvData(cv);

        // Generate letter using AI
        String generatedContent = generatorAgent.generateLetter(
                cvData,
                job,
                request.tone(),
                request.language(),
                request.additionalInstructions()
        );

        // Get next version number
        int version = letterRepository.getNextVersion(userId, request.jobId(), tenantId);

        // Create letter entity
        MotivationLetter letter = MotivationLetter.builder()
                .tenantId(tenantId)
                .user(user)
                .cv(cv)
                .jobListing(job)
                .generatedContent(generatedContent)
                .additionalInstructions(request.additionalInstructions())
                .status(LetterStatus.GENERATED)
                .tone(request.tone())
                .language(request.language())
                .version(version)
                .generatedAt(Instant.now())
                .build();

        letter.updateWordCount();
        letter = letterRepository.save(letter);

        log.info("Generated letter {} (version {}) with {} words",
                letter.getId(), version, letter.getWordCount());

        return MotivationLetterResponse.from(letter);
    }

    /**
     * Get all letters for a user with pagination.
     */
    @Transactional(readOnly = true)
    public Page<MotivationLetterResponse> getLetters(UUID userId, int page, int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, size);

        return letterRepository.findByUserIdAndTenantIdOrderByUpdatedAtDesc(userId, tenantId, pageable)
                .map(MotivationLetterResponse::from);
    }

    /**
     * Get a specific letter by ID.
     */
    @Transactional(readOnly = true)
    public MotivationLetterResponse getLetter(UUID letterId, UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();

        MotivationLetter letter = letterRepository.findByIdAndUserIdAndTenantId(letterId, userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Letter not found: " + letterId));

        return MotivationLetterResponse.from(letter);
    }

    /**
     * Get letters for a specific job.
     */
    @Transactional(readOnly = true)
    public List<MotivationLetterResponse> getLettersForJob(UUID jobId, UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();

        return letterRepository.findByJobListingIdAndTenantIdOrderByUpdatedAtDesc(jobId, tenantId)
                .stream()
                .filter(l -> l.getUser().getId().equals(userId))
                .map(MotivationLetterResponse::from)
                .toList();
    }

    /**
     * Update letter content (user edits).
     */
    @Transactional
    public MotivationLetterResponse updateLetter(UUID letterId, UUID userId, UpdateLetterRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        MotivationLetter letter = letterRepository.findByIdAndUserIdAndTenantId(letterId, userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Letter not found: " + letterId));

        letter.setEditedContent(request.content());
        letter.setStatus(LetterStatus.EDITED);
        letter.updateWordCount();

        letter = letterRepository.save(letter);
        log.info("Updated letter {} with {} words", letterId, letter.getWordCount());

        return MotivationLetterResponse.from(letter);
    }

    /**
     * Delete a letter.
     */
    @Transactional
    public void deleteLetter(UUID letterId, UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();

        MotivationLetter letter = letterRepository.findByIdAndUserIdAndTenantId(letterId, userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Letter not found: " + letterId));

        letterRepository.delete(letter);
        log.info("Deleted letter {}", letterId);
    }

    /**
     * Mark letter as sent (with application).
     */
    @Transactional
    public MotivationLetterResponse markAsSent(UUID letterId, UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();

        MotivationLetter letter = letterRepository.findByIdAndUserIdAndTenantId(letterId, userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Letter not found: " + letterId));

        letter.setStatus(LetterStatus.SENT);
        letter = letterRepository.save(letter);

        log.info("Marked letter {} as sent", letterId);
        return MotivationLetterResponse.from(letter);
    }

    /**
     * Count letters for a user.
     */
    @Transactional(readOnly = true)
    public long countLetters(UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();
        return letterRepository.countByUserIdAndTenantId(userId, tenantId);
    }

    /**
     * Load CV - use specified or find latest parsed.
     */
    private CvDetails loadCv(UUID userId, UUID cvId, UUID tenantId) {
        if (cvId != null) {
            return cvRepository.findById(cvId)
                    .orElseThrow(() -> new ResourceNotFoundException("CV not found: " + cvId));
        }

        return cvRepository.findTopByUserIdAndTenantIdAndStatusOrderByCreatedAtDesc(
                        userId, tenantId, CvStatus.PARSED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No parsed CV found. Please upload and parse your CV first."));
    }

    /**
     * Parse CV JSON data.
     */
    private CvParsedData parseCvData(CvDetails cv) {
        if (cv.getParsedJson() == null || cv.getParsedJson().isBlank()) {
            throw new IllegalStateException("CV has not been parsed: " + cv.getId());
        }

        try {
            return objectMapper.readValue(cv.getParsedJson(), CvParsedData.class);
        } catch (Exception e) {
            log.error("Failed to parse CV JSON: {}", e.getMessage());
            throw new IllegalStateException("Invalid CV data format", e);
        }
    }
}
