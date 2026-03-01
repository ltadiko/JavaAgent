package com.jobagent.jobagent.cv.controller;

import com.jobagent.jobagent.cv.dto.CvDownloadResponse;
import com.jobagent.jobagent.cv.dto.CvSummaryResponse;
import com.jobagent.jobagent.cv.dto.CvUploadResponse;
import com.jobagent.jobagent.cv.service.CvUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Sprint 3.7 — REST controller for CV operations.
 */
@RestController
@RequestMapping("/api/v1/cv")
@RequiredArgsConstructor
@Slf4j
public class CvController {

    private final CvUploadService cvUploadService;

    /**
     * Upload a new CV file.
     *
     * POST /api/v1/cv
     * Content-Type: multipart/form-data
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CvUploadResponse> uploadCv(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = extractUserId(jwt);
        CvUploadResponse response = cvUploadService.uploadCv(userId, file);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get the active CV summary for the current user.
     *
     * GET /api/v1/cv
     */
    @GetMapping
    public ResponseEntity<CvSummaryResponse> getActiveCv(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = extractUserId(jwt);
        CvSummaryResponse response = cvUploadService.getActiveCv(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get CV history for the current user.
     *
     * GET /api/v1/cv/history
     */
    @GetMapping("/history")
    public ResponseEntity<List<CvSummaryResponse>> getCvHistory(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = extractUserId(jwt);
        List<CvSummaryResponse> history = cvUploadService.getCvHistory(userId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get a presigned download URL for a specific CV.
     *
     * GET /api/v1/cv/{id}/download
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<CvDownloadResponse> getDownloadUrl(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = extractUserId(jwt);
        CvDownloadResponse response = cvUploadService.getDownloadUrl(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft-delete a CV.
     *
     * DELETE /api/v1/cv/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCv(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = extractUserId(jwt);
        cvUploadService.deleteCv(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Extract user_id claim from JWT.
     */
    private UUID extractUserId(Jwt jwt) {
        String userIdClaim = jwt.getClaimAsString("user_id");
        if (userIdClaim == null) {
            throw new IllegalStateException("user_id claim not found in JWT");
        }
        return UUID.fromString(userIdClaim);
    }
}
