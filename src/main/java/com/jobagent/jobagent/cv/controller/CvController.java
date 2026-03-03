package com.jobagent.jobagent.cv.controller;

import com.jobagent.jobagent.cv.dto.CvDownloadResponse;
import com.jobagent.jobagent.cv.dto.CvSummaryResponse;
import com.jobagent.jobagent.cv.dto.CvUploadResponse;
import com.jobagent.jobagent.cv.service.CvUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "CV Management", description = "Upload, parse, download, and manage CV documents")
public class CvController {

    private final CvUploadService cvUploadService;

    @Operation(summary = "Upload a CV file", description = "Uploads a CV document (PDF, DOCX) and triggers AI-powered parsing",
            responses = {
                @ApiResponse(responseCode = "201", description = "CV uploaded successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid file type or size"),
                @ApiResponse(responseCode = "401", description = "Not authenticated")
            })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CvUploadResponse> uploadCv(
            @Parameter(description = "CV file (PDF or DOCX, max 10MB)") @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = extractUserId(jwt);
        CvUploadResponse response = cvUploadService.uploadCv(userId, file);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get active CV", description = "Returns the currently active CV for the authenticated user",
            responses = {
                @ApiResponse(responseCode = "200", description = "Active CV retrieved"),
                @ApiResponse(responseCode = "404", description = "No active CV found")
            })
    @GetMapping
    public ResponseEntity<CvSummaryResponse> getActiveCv(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = extractUserId(jwt);
        CvSummaryResponse response = cvUploadService.getActiveCv(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get CV upload history", description = "Returns all CV uploads for the authenticated user, ordered by upload date descending")
    @GetMapping("/history")
    public ResponseEntity<List<CvSummaryResponse>> getCvHistory(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = extractUserId(jwt);
        List<CvSummaryResponse> history = cvUploadService.getCvHistory(userId);
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "Get CV download URL", description = "Generates a presigned URL for downloading a specific CV file",
            responses = {
                @ApiResponse(responseCode = "200", description = "Download URL generated"),
                @ApiResponse(responseCode = "404", description = "CV not found"),
                @ApiResponse(responseCode = "403", description = "Access denied — CV belongs to another user")
            })
    @GetMapping("/{id}/download")
    public ResponseEntity<CvDownloadResponse> getDownloadUrl(
            @Parameter(description = "CV identifier", required = true) @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = extractUserId(jwt);
        CvDownloadResponse response = cvUploadService.getDownloadUrl(id, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a CV", description = "Soft-deletes a CV document. The file is retained but marked as inactive",
            responses = {
                @ApiResponse(responseCode = "204", description = "CV deleted successfully"),
                @ApiResponse(responseCode = "404", description = "CV not found")
            })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCv(
            @Parameter(description = "CV identifier", required = true) @PathVariable UUID id,
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
