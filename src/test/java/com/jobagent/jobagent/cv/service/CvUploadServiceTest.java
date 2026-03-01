package com.jobagent.jobagent.cv.service;

import com.jobagent.jobagent.auth.model.User;
import com.jobagent.jobagent.auth.repository.UserRepository;
import com.jobagent.jobagent.cv.dto.CvUploadResponse;
import com.jobagent.jobagent.cv.model.CvDetails;
import com.jobagent.jobagent.cv.model.CvStatus;
import com.jobagent.jobagent.cv.repository.CvDetailsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Sprint 3.6 — Unit tests for CvUploadService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CvUploadService Tests")
class CvUploadServiceTest {

    @Mock
    private CvDetailsRepository cvDetailsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private CvProcessingService cvProcessingService;

    @Mock
    private MultipartFile multipartFile;

    private CvUploadService cvUploadService;

    private User testUser;
    private UUID userId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        cvUploadService = new CvUploadService(
                cvDetailsRepository, userRepository, fileStorageService, cvProcessingService);

        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();

        testUser = User.builder()
                .email("test@example.com")
                .emailHash("hash")
                .fullName("Test User")
                .country("US")
                .region("US")
                .build();
        testUser.setId(userId);
        testUser.setTenantId(tenantId);
    }

    @Test
    @DisplayName("Upload PDF file successfully")
    void uploadCv_validPdf_success() throws IOException {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getOriginalFilename()).thenReturn("resume.pdf");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(cvDetailsRepository.save(any(CvDetails.class))).thenAnswer(invocation -> {
            CvDetails cv = invocation.getArgument(0);
            cv.setId(UUID.randomUUID());
            return cv;
        });

        // When
        CvUploadResponse response = cvUploadService.uploadCv(userId, multipartFile);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.fileName()).isEqualTo("resume.pdf");
        assertThat(response.contentType()).isEqualTo("application/pdf");
        assertThat(response.status()).isEqualTo("UPLOADED");

        verify(cvDetailsRepository).deactivateAllByUserId(userId);
        verify(fileStorageService).upload(anyString(), any(), eq("application/pdf"), eq(1024L));
        verify(cvDetailsRepository).save(any(CvDetails.class));
    }

    @Test
    @DisplayName("Upload DOCX file successfully")
    void uploadCv_validDocx_success() throws IOException {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        when(multipartFile.getSize()).thenReturn(2048L);
        when(multipartFile.getOriginalFilename()).thenReturn("resume.docx");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[2048]));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(cvDetailsRepository.save(any(CvDetails.class))).thenAnswer(invocation -> {
            CvDetails cv = invocation.getArgument(0);
            cv.setId(UUID.randomUUID());
            return cv;
        });

        // When
        CvUploadResponse response = cvUploadService.uploadCv(userId, multipartFile);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.fileName()).isEqualTo("resume.docx");
    }

    @Test
    @DisplayName("Reject invalid file type")
    void uploadCv_invalidFileType_throwsException() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("text/plain");

        // When/Then
        assertThatThrownBy(() -> cvUploadService.uploadCv(userId, multipartFile))
                .isInstanceOf(CvUploadService.CvUploadException.class)
                .hasMessageContaining("Invalid file type");
    }

    @Test
    @DisplayName("Reject file exceeding size limit")
    void uploadCv_fileTooLarge_throwsException() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getSize()).thenReturn(11L * 1024 * 1024); // 11 MB

        // When/Then
        assertThatThrownBy(() -> cvUploadService.uploadCv(userId, multipartFile))
                .isInstanceOf(CvUploadService.CvUploadException.class)
                .hasMessageContaining("exceeds maximum");
    }

    @Test
    @DisplayName("Reject empty file")
    void uploadCv_emptyFile_throwsException() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> cvUploadService.uploadCv(userId, multipartFile))
                .isInstanceOf(CvUploadService.CvUploadException.class)
                .hasMessageContaining("File is required");
    }

    @Test
    @DisplayName("Previous CV is deactivated on new upload")
    void uploadCv_deactivatesPreviousCv() throws IOException {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getOriginalFilename()).thenReturn("resume.pdf");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(cvDetailsRepository.save(any(CvDetails.class))).thenAnswer(invocation -> {
            CvDetails cv = invocation.getArgument(0);
            cv.setId(UUID.randomUUID());
            return cv;
        });

        // When
        cvUploadService.uploadCv(userId, multipartFile);

        // Then
        verify(cvDetailsRepository).deactivateAllByUserId(userId);
    }

    @Test
    @DisplayName("S3 key contains tenant and user IDs")
    void uploadCv_s3KeyFormat_correct() throws IOException {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getOriginalFilename()).thenReturn("resume.pdf");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(cvDetailsRepository.save(any(CvDetails.class))).thenAnswer(invocation -> {
            CvDetails cv = invocation.getArgument(0);
            cv.setId(UUID.randomUUID());
            return cv;
        });

        // When
        cvUploadService.uploadCv(userId, multipartFile);

        // Then
        ArgumentCaptor<CvDetails> captor = ArgumentCaptor.forClass(CvDetails.class);
        verify(cvDetailsRepository).save(captor.capture());

        String s3Key = captor.getValue().getS3Key();
        assertThat(s3Key).startsWith("cv/");
        assertThat(s3Key).contains(tenantId.toString());
        assertThat(s3Key).contains(userId.toString());
        assertThat(s3Key).endsWith(".pdf");
    }
}
