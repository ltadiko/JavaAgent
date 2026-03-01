package com.jobagent.jobagent.motivation.controller;

import com.jobagent.jobagent.motivation.dto.GenerateLetterRequest;
import com.jobagent.jobagent.motivation.dto.MotivationLetterResponse;
import com.jobagent.jobagent.motivation.dto.UpdateLetterRequest;
import com.jobagent.jobagent.motivation.service.MotivationLetterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sprint 6.8 — REST controller for motivation letter operations.
 */
@RestController
@RequestMapping("/api/v1/motivations")
@RequiredArgsConstructor
@Slf4j
public class MotivationController {

    private final MotivationLetterService letterService;

    /**
     * Generate a new motivation letter.
     */
    @PostMapping("/generate")
    public ResponseEntity<MotivationLetterResponse> generateLetter(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody GenerateLetterRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} requesting letter generation for job {}", userId, request.jobId());

        MotivationLetterResponse response = letterService.generateLetter(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all letters for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<Page<MotivationLetterResponse>> getLetters(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = UUID.fromString(jwt.getSubject());
        Page<MotivationLetterResponse> letters = letterService.getLetters(userId, page, size);
        return ResponseEntity.ok(letters);
    }

    /**
     * Get a specific letter by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MotivationLetterResponse> getLetter(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        MotivationLetterResponse letter = letterService.getLetter(id, userId);
        return ResponseEntity.ok(letter);
    }

    /**
     * Get all letters for a specific job.
     */
    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<MotivationLetterResponse>> getLettersForJob(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID jobId) {

        UUID userId = UUID.fromString(jwt.getSubject());
        List<MotivationLetterResponse> letters = letterService.getLettersForJob(jobId, userId);
        return ResponseEntity.ok(letters);
    }

    /**
     * Update letter content (user edits).
     */
    @PutMapping("/{id}")
    public ResponseEntity<MotivationLetterResponse> updateLetter(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLetterRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} updating letter {}", userId, id);

        MotivationLetterResponse letter = letterService.updateLetter(id, userId, request);
        return ResponseEntity.ok(letter);
    }

    /**
     * Delete a letter.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLetter(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} deleting letter {}", userId, id);

        letterService.deleteLetter(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Mark letter as sent.
     */
    @PostMapping("/{id}/send")
    public ResponseEntity<MotivationLetterResponse> markAsSent(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        MotivationLetterResponse letter = letterService.markAsSent(id, userId);
        return ResponseEntity.ok(letter);
    }

    /**
     * Get letter count for user.
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getLetterCount(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        long count = letterService.countLetters(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
