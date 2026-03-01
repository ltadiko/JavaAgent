package com.jobagent.jobagent.cv.service;

import com.jobagent.jobagent.auth.model.User;
import com.jobagent.jobagent.auth.repository.UserRepository;
import com.jobagent.jobagent.common.exception.ResourceNotFoundException;
import com.jobagent.jobagent.cv.dto.CvDownloadResponse;
import com.jobagent.jobagent.cv.dto.CvSummaryResponse;
import com.jobagent.jobagent.cv.dto.CvUploadResponse;
import com.jobagent.jobagent.cv.model.CvDetails;
import com.jobagent.jobagent.cv.model.CvStatus;
import com.jobagent.jobagent.cv.repository.CvDetailsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Sprint 3.6 — CV upload orchestration service.
 */
@Service
@Slf4j
@Transactional
public class CvUploadService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private final CvDetailsRepository cvDetailsRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final CvProcessingService cvProcessingService;

    public CvUploadService(
            CvDetailsRepository cvDetailsRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            @Lazy CvProcessingService cvProcessingService) {
        this.cvDetailsRepository = cvDetailsRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.cvProcessingService = cvProcessingService;
    }

    /**
     * Upload a new CV file.
     */
    public CvUploadResponse uploadCv(UUID userId, MultipartFile file) {
        validateFile(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Deactivate previous CVs
        cvDetailsRepository.deactivateAllByUserId(userId);

        // Generate S3 key
        String extension = getFileExtension(file.getOriginalFilename());
        String s3Key = generateS3Key(user.getTenantId(), userId, extension);

        // Upload to storage
        try {
            fileStorageService.upload(s3Key, file.getInputStream(), file.getContentType(), file.getSize());
        } catch (IOException e) {
            throw new CvUploadException("Failed to read file content", e);
        }

        // Save CV details
        CvDetails cvDetails = CvDetails.builder()
                .user(user)
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .s3Key(s3Key)
                .status(CvStatus.UPLOADED)
                .active(true)
                .build();

        cvDetailsRepository.save(cvDetails);

        log.info("CV uploaded for user {}: {} ({} bytes)", userId, file.getOriginalFilename(), file.getSize());

        // Trigger async processing
        cvProcessingService.processAsync(cvDetails.getId());

        return new CvUploadResponse(
                cvDetails.getId(),
                cvDetails.getFileName(),
                cvDetails.getContentType(),
                cvDetails.getFileSize(),
                cvDetails.getStatus().name(),
                cvDetails.getCreatedAt()
        );
    }

    /**
     * Get the active CV for a user.
     */
    @Transactional(readOnly = true)
    public CvSummaryResponse getActiveCv(UUID userId) {
        CvDetails cv = cvDetailsRepository.findByUserIdAndActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active CV found for user: " + userId));
        return toSummaryResponse(cv);
    }

    /**
     * Get CV history for a user.
     */
    @Transactional(readOnly = true)
    public List<CvSummaryResponse> getCvHistory(UUID userId) {
        return cvDetailsRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    /**
     * Get a presigned download URL for a CV.
     */
    @Transactional(readOnly = true)
    public CvDownloadResponse getDownloadUrl(UUID cvId, UUID userId) {
        CvDetails cv = cvDetailsRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("CV not found: " + cvId));

        // Verify ownership
        if (!cv.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("CV not found: " + cvId);
        }

        int expirationMinutes = 15;
        String url = fileStorageService.generatePresignedDownloadUrl(cv.getS3Key(), expirationMinutes);

        return new CvDownloadResponse(url, expirationMinutes);
    }

    /**
     * Soft-delete a CV.
     */
    public void deleteCv(UUID cvId, UUID userId) {
        CvDetails cv = cvDetailsRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("CV not found: " + cvId));

        // Verify ownership
        if (!cv.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("CV not found: " + cvId);
        }

        cv.setActive(false);
        cvDetailsRepository.save(cv);

        log.info("CV {} soft-deleted for user {}", cvId, userId);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CvUploadException("File is required");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new CvUploadException("Invalid file type. Allowed: PDF, DOCX");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CvUploadException("File size exceeds maximum allowed (10 MB)");
        }
    }

    private String generateS3Key(UUID tenantId, UUID userId, String extension) {
        return String.format("cv/%s/%s/%s.%s", tenantId, userId, UUID.randomUUID(), extension);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private CvSummaryResponse toSummaryResponse(CvDetails cv) {
        return new CvSummaryResponse(
                cv.getId(),
                cv.getFileName(),
                cv.getContentType(),
                cv.getFileSize(),
                cv.getStatus().name(),
                cv.getActive(),
                cv.getCreatedAt(),
                cv.getParsedAt()
        );
    }

    /**
     * Custom exception for CV upload errors.
     */
    public static class CvUploadException extends RuntimeException {
        public CvUploadException(String message) {
            super(message);
        }

        public CvUploadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
