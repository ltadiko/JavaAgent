package com.jobagent.jobagent.motivation.controller;

import com.jobagent.jobagent.motivation.dto.GenerateLetterRequest;
import com.jobagent.jobagent.motivation.dto.MotivationLetterResponse;
import com.jobagent.jobagent.motivation.dto.UpdateLetterRequest;
import com.jobagent.jobagent.motivation.service.MotivationLetterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Motivation Letters", description = "AI-powered motivation letter generation, editing, and management")
public class MotivationController {

    private final MotivationLetterService letterService;

    @Operation(summary = "Generate a motivation letter", description = "Uses AI to generate a personalized motivation letter based on the user's CV and the target job listing",
            responses = {
                @ApiResponse(responseCode = "200", description = "Letter generated successfully"),
                @ApiResponse(responseCode = "404", description = "Job or CV not found")
            })
    @PostMapping("/generate")
    public ResponseEntity<MotivationLetterResponse> generateLetter(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody GenerateLetterRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} requesting letter generation for job {}", userId, request.jobId());

        MotivationLetterResponse response = letterService.generateLetter(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List motivation letters", description = "Returns a paginated list of all motivation letters for the authenticated user")
    @GetMapping
    public ResponseEntity<Page<MotivationLetterResponse>> getLetters(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        UUID userId = UUID.fromString(jwt.getSubject());
        Page<MotivationLetterResponse> letters = letterService.getLetters(userId, page, size);
        return ResponseEntity.ok(letters);
    }

    @Operation(summary = "Get a motivation letter", description = "Returns full details of a specific motivation letter",
            responses = {
                @ApiResponse(responseCode = "200", description = "Letter found"),
                @ApiResponse(responseCode = "404", description = "Letter not found")
            })
    @GetMapping("/{id}")
    public ResponseEntity<MotivationLetterResponse> getLetter(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Letter identifier", required = true) @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        MotivationLetterResponse letter = letterService.getLetter(id, userId);
        return ResponseEntity.ok(letter);
    }

    @Operation(summary = "Get letters for a job", description = "Returns all motivation letters generated for a specific job listing")
    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<MotivationLetterResponse>> getLettersForJob(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Job listing identifier", required = true) @PathVariable UUID jobId) {

        UUID userId = UUID.fromString(jwt.getSubject());
        List<MotivationLetterResponse> letters = letterService.getLettersForJob(jobId, userId);
        return ResponseEntity.ok(letters);
    }

    @Operation(summary = "Update letter content", description = "Updates the content of a motivation letter with user edits")
    @PutMapping("/{id}")
    public ResponseEntity<MotivationLetterResponse> updateLetter(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Letter identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody UpdateLetterRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} updating letter {}", userId, id);

        MotivationLetterResponse letter = letterService.updateLetter(id, userId, request);
        return ResponseEntity.ok(letter);
    }

    @Operation(summary = "Delete a motivation letter", description = "Permanently deletes a motivation letter",
            responses = {
                @ApiResponse(responseCode = "204", description = "Letter deleted"),
                @ApiResponse(responseCode = "404", description = "Letter not found")
            })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLetter(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Letter identifier", required = true) @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} deleting letter {}", userId, id);

        letterService.deleteLetter(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark letter as sent", description = "Updates the letter status to SENT, indicating it has been used in an application")
    @PostMapping("/{id}/send")
    public ResponseEntity<MotivationLetterResponse> markAsSent(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Letter identifier", required = true) @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        MotivationLetterResponse letter = letterService.markAsSent(id, userId);
        return ResponseEntity.ok(letter);
    }

    @Operation(summary = "Get letter count", description = "Returns the total number of motivation letters for the authenticated user")
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getLetterCount(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        long count = letterService.countLetters(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
