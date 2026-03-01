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
import com.jobagent.jobagent.motivation.model.LetterTone;
import com.jobagent.jobagent.motivation.model.MotivationLetter;
import com.jobagent.jobagent.motivation.repository.MotivationLetterRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Sprint 6.10 — Unit tests for MotivationLetterService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MotivationLetterService Tests")
class MotivationLetterServiceTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID JOB_ID = UUID.randomUUID();
    private static final UUID CV_ID = UUID.randomUUID();

    @Mock
    private MotivationLetterRepository letterRepository;

    @Mock
    private MotivationGeneratorAgent generatorAgent;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CvDetailsRepository cvRepository;

    @Mock
    private JobListingRepository jobRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MotivationLetterService service;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("generateLetter() creates letter with AI content")
    void generateLetter_success() throws Exception {
        // Given
        GenerateLetterRequest request = new GenerateLetterRequest(
                JOB_ID, null, LetterTone.PROFESSIONAL, "en", null);

        User user = createTestUser();
        JobListing job = createTestJob();
        CvDetails cv = createTestCv();
        CvParsedData cvData = createTestCvData();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(cvRepository.findTopByUserIdAndTenantIdAndStatusOrderByCreatedAtDesc(
                USER_ID, TENANT_ID, CvStatus.PARSED)).thenReturn(Optional.of(cv));
        when(objectMapper.readValue(anyString(), eq(CvParsedData.class))).thenReturn(cvData);
        when(generatorAgent.generateLetter(any(), any(), any(), any(), any()))
                .thenReturn("Generated letter content");
        when(letterRepository.getNextVersion(USER_ID, JOB_ID, TENANT_ID)).thenReturn(1);
        when(letterRepository.save(any(MotivationLetter.class))).thenAnswer(inv -> {
            MotivationLetter letter = inv.getArgument(0);
            letter.setId(UUID.randomUUID());
            return letter;
        });

        // When
        MotivationLetterResponse response = service.generateLetter(USER_ID, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("Generated letter content");
        assertThat(response.status()).isEqualTo(LetterStatus.GENERATED);
        verify(generatorAgent).generateLetter(eq(cvData), eq(job), eq(LetterTone.PROFESSIONAL), eq("en"), isNull());
    }

    @Test
    @DisplayName("generateLetter() throws when user not found")
    void generateLetter_userNotFound_throws() {
        // Given
        GenerateLetterRequest request = new GenerateLetterRequest(JOB_ID, null, null, null, null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.generateLetter(USER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("generateLetter() throws when job not found")
    void generateLetter_jobNotFound_throws() {
        // Given
        GenerateLetterRequest request = new GenerateLetterRequest(JOB_ID, null, null, null, null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(createTestUser()));
        when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.generateLetter(USER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Job not found");
    }

    @Test
    @DisplayName("getLetters() returns paginated results")
    void getLetters_returnsPaginated() {
        // Given
        MotivationLetter letter = createTestLetter();
        Page<MotivationLetter> page = new PageImpl<>(List.of(letter));

        when(letterRepository.findByUserIdAndTenantIdOrderByUpdatedAtDesc(
                eq(USER_ID), eq(TENANT_ID), any(PageRequest.class))).thenReturn(page);

        // When
        Page<MotivationLetterResponse> result = service.getLetters(USER_ID, 0, 20);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().id()).isEqualTo(letter.getId());
    }

    @Test
    @DisplayName("getLetter() returns letter by ID")
    void getLetter_returnsLetter() {
        // Given
        UUID letterId = UUID.randomUUID();
        MotivationLetter letter = createTestLetter();
        letter.setId(letterId);

        when(letterRepository.findByIdAndUserIdAndTenantId(letterId, USER_ID, TENANT_ID))
                .thenReturn(Optional.of(letter));

        // When
        MotivationLetterResponse response = service.getLetter(letterId, USER_ID);

        // Then
        assertThat(response.id()).isEqualTo(letterId);
    }

    @Test
    @DisplayName("getLetter() throws when not found")
    void getLetter_notFound_throws() {
        // Given
        UUID letterId = UUID.randomUUID();
        when(letterRepository.findByIdAndUserIdAndTenantId(letterId, USER_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.getLetter(letterId, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateLetter() updates content and status")
    void updateLetter_updatesContentAndStatus() {
        // Given
        UUID letterId = UUID.randomUUID();
        MotivationLetter letter = createTestLetter();
        letter.setId(letterId);
        UpdateLetterRequest request = new UpdateLetterRequest("User edited content");

        when(letterRepository.findByIdAndUserIdAndTenantId(letterId, USER_ID, TENANT_ID))
                .thenReturn(Optional.of(letter));
        when(letterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        MotivationLetterResponse response = service.updateLetter(letterId, USER_ID, request);

        // Then
        assertThat(response.editedContent()).isEqualTo("User edited content");
        assertThat(response.status()).isEqualTo(LetterStatus.EDITED);
    }

    @Test
    @DisplayName("deleteLetter() removes letter")
    void deleteLetter_removesLetter() {
        // Given
        UUID letterId = UUID.randomUUID();
        MotivationLetter letter = createTestLetter();
        letter.setId(letterId);

        when(letterRepository.findByIdAndUserIdAndTenantId(letterId, USER_ID, TENANT_ID))
                .thenReturn(Optional.of(letter));

        // When
        service.deleteLetter(letterId, USER_ID);

        // Then
        verify(letterRepository).delete(letter);
    }

    @Test
    @DisplayName("markAsSent() updates status")
    void markAsSent_updatesStatus() {
        // Given
        UUID letterId = UUID.randomUUID();
        MotivationLetter letter = createTestLetter();
        letter.setId(letterId);

        when(letterRepository.findByIdAndUserIdAndTenantId(letterId, USER_ID, TENANT_ID))
                .thenReturn(Optional.of(letter));
        when(letterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        MotivationLetterResponse response = service.markAsSent(letterId, USER_ID);

        // Then
        assertThat(response.status()).isEqualTo(LetterStatus.SENT);
    }

    private User createTestUser() {
        User user = User.builder()
                .email("test@example.com")
                .emailHash("hash")
                .fullName("Test User")
                .build();
        user.setId(USER_ID);
        return user;
    }

    private JobListing createTestJob() {
        JobListing job = new JobListing();
        job.setId(JOB_ID);
        job.setTenantId(TENANT_ID);
        job.setTitle("Software Engineer");
        job.setCompany("Tech Corp");
        return job;
    }

    private CvDetails createTestCv() {
        CvDetails cv = CvDetails.builder()
                .status(CvStatus.PARSED)
                .parsedJson("{}")
                .build();
        cv.setId(CV_ID);
        cv.setTenantId(TENANT_ID);
        return cv;
    }

    private CvParsedData createTestCvData() {
        return new CvParsedData(
                "Test User", "test@example.com", "+1234567890",
                "Developer", "Summary", List.of("Java"),
                List.of(), List.of(), List.of(), List.of()
        );
    }

    private MotivationLetter createTestLetter() {
        JobListing job = createTestJob();
        return MotivationLetter.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .jobListing(job)
                .generatedContent("Test letter content")
                .status(LetterStatus.GENERATED)
                .tone(LetterTone.PROFESSIONAL)
                .language("en")
                .version(1)
                .wordCount(50)
                .build();
    }
}
