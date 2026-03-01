package com.jobagent.jobagent.application.service;

import com.jobagent.jobagent.application.dto.*;
import com.jobagent.jobagent.application.model.ApplicationEvent;
import com.jobagent.jobagent.application.model.ApplicationStatus;
import com.jobagent.jobagent.application.model.JobApplication;
import com.jobagent.jobagent.application.repository.ApplicationEventRepository;
import com.jobagent.jobagent.application.repository.JobApplicationRepository;
import com.jobagent.jobagent.auth.model.User;
import com.jobagent.jobagent.auth.repository.UserRepository;
import com.jobagent.jobagent.common.exception.DuplicateResourceException;
import com.jobagent.jobagent.common.exception.ResourceNotFoundException;
import com.jobagent.jobagent.common.multitenancy.TenantContext;
import com.jobagent.jobagent.cv.model.CvDetails;
import com.jobagent.jobagent.cv.repository.CvDetailsRepository;
import com.jobagent.jobagent.jobsearch.model.JobListing;
import com.jobagent.jobagent.jobsearch.repository.JobListingRepository;
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
 * Sprint 7.8 — Unit tests for ApplicationService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationService Tests")
class ApplicationServiceTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID JOB_ID = UUID.randomUUID();
    private static final UUID CV_ID = UUID.randomUUID();
    private static final UUID APP_ID = UUID.randomUUID();

    @Mock
    private JobApplicationRepository applicationRepository;

    @Mock
    private ApplicationEventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobListingRepository jobRepository;

    @Mock
    private CvDetailsRepository cvRepository;

    @Mock
    private MotivationLetterRepository letterRepository;

    @Mock
    private ApplicationSenderService senderService;

    @InjectMocks
    private ApplicationService service;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("createApplication() creates draft application")
    void createApplication_success() {
        // Given
        SubmitApplicationRequest request = new SubmitApplicationRequest(
                JOB_ID, CV_ID, null, "Please consider my application");

        User user = createTestUser();
        JobListing job = createTestJob();
        CvDetails cv = createTestCv();

        when(applicationRepository.existsByUserIdAndJobIdAndTenantId(USER_ID, JOB_ID, TENANT_ID))
                .thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(cvRepository.findById(CV_ID)).thenReturn(Optional.of(cv));
        when(applicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> {
            JobApplication app = inv.getArgument(0);
            app.setId(APP_ID);
            return app;
        });

        // When
        JobApplicationResponse response = service.createApplication(USER_ID, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(ApplicationStatus.DRAFT);
        assertThat(response.additionalMessage()).isEqualTo("Please consider my application");
        verify(applicationRepository).save(any(JobApplication.class));
        verify(eventRepository).save(argThat(event ->
                event.getEventType() == ApplicationEvent.EventType.CREATED));
    }

    @Test
    @DisplayName("createApplication() throws on duplicate")
    void createApplication_duplicate_throws() {
        // Given
        SubmitApplicationRequest request = new SubmitApplicationRequest(JOB_ID, CV_ID, null, null);

        when(applicationRepository.existsByUserIdAndJobIdAndTenantId(USER_ID, JOB_ID, TENANT_ID))
                .thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> service.createApplication(USER_ID, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already applied");
    }

    @Test
    @DisplayName("submitApplication() changes status and triggers send")
    void submitApplication_success() {
        // Given
        JobApplication application = createTestApplication();
        application.setStatus(ApplicationStatus.DRAFT);

        when(applicationRepository.findByIdAndUserIdAndTenantId(APP_ID, USER_ID, TENANT_ID))
                .thenReturn(Optional.of(application));
        when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        JobApplicationResponse response = service.submitApplication(APP_ID, USER_ID);

        // Then
        assertThat(response.status()).isEqualTo(ApplicationStatus.PENDING);
        verify(senderService).sendAsync(APP_ID);
        verify(eventRepository).save(argThat(event ->
                event.getEventType() == ApplicationEvent.EventType.STATUS_CHANGED &&
                event.getOldStatus() == ApplicationStatus.DRAFT &&
                event.getNewStatus() == ApplicationStatus.PENDING));
    }

    @Test
    @DisplayName("submitApplication() throws when not DRAFT")
    void submitApplication_notDraft_throws() {
        // Given
        JobApplication application = createTestApplication();
        application.setStatus(ApplicationStatus.SENT);

        when(applicationRepository.findByIdAndUserIdAndTenantId(APP_ID, USER_ID, TENANT_ID))
                .thenReturn(Optional.of(application));

        // When/Then
        assertThatThrownBy(() -> service.submitApplication(APP_ID, USER_ID))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("getApplications() returns paginated results")
    void getApplications_returnsPaginated() {
        // Given
        JobApplication application = createTestApplication();
        Page<JobApplication> page = new PageImpl<>(List.of(application));

        when(applicationRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(
                eq(USER_ID), eq(TENANT_ID), any(PageRequest.class))).thenReturn(page);

        // When
        Page<JobApplicationResponse> result = service.getApplications(USER_ID, 0, 20);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getApplication() returns application")
    void getApplication_returnsApplication() {
        // Given
        JobApplication application = createTestApplication();

        when(applicationRepository.findByIdAndUserIdAndTenantId(APP_ID, USER_ID, TENANT_ID))
                .thenReturn(Optional.of(application));

        // When
        JobApplicationResponse response = service.getApplication(APP_ID, USER_ID);

        // Then
        assertThat(response.id()).isEqualTo(APP_ID);
    }

    @Test
    @DisplayName("getApplication() throws when not found")
    void getApplication_notFound_throws() {
        // Given
        when(applicationRepository.findByIdAndUserIdAndTenantId(APP_ID, USER_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.getApplication(APP_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("withdrawApplication() changes status")
    void withdrawApplication_success() {
        // Given
        JobApplication application = createTestApplication();
        application.setStatus(ApplicationStatus.SENT);

        when(applicationRepository.findByIdAndUserIdAndTenantId(APP_ID, USER_ID, TENANT_ID))
                .thenReturn(Optional.of(application));
        when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        JobApplicationResponse response = service.withdrawApplication(APP_ID, USER_ID);

        // Then
        assertThat(response.status()).isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("deleteApplication() removes draft")
    void deleteApplication_removesApplication() {
        // Given
        JobApplication application = createTestApplication();
        application.setStatus(ApplicationStatus.DRAFT);

        when(applicationRepository.findByIdAndUserIdAndTenantId(APP_ID, USER_ID, TENANT_ID))
                .thenReturn(Optional.of(application));

        // When
        service.deleteApplication(APP_ID, USER_ID);

        // Then
        verify(applicationRepository).delete(application);
    }

    @Test
    @DisplayName("deleteApplication() throws when not modifiable")
    void deleteApplication_notModifiable_throws() {
        // Given
        JobApplication application = createTestApplication();
        application.setStatus(ApplicationStatus.SENT);

        when(applicationRepository.findByIdAndUserIdAndTenantId(APP_ID, USER_ID, TENANT_ID))
                .thenReturn(Optional.of(application));

        // When/Then
        assertThatThrownBy(() -> service.deleteApplication(APP_ID, USER_ID))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("hasApplied() returns true when exists")
    void hasApplied_exists_returnsTrue() {
        // Given
        when(applicationRepository.existsByUserIdAndJobIdAndTenantId(USER_ID, JOB_ID, TENANT_ID))
                .thenReturn(true);

        // When
        boolean result = service.hasApplied(USER_ID, JOB_ID);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("getTimeline() returns events for application")
    void getTimeline_returnsEvents() {
        // Given
        JobApplication application = createTestApplication();
        when(applicationRepository.findByIdAndUserIdAndTenantId(APP_ID, USER_ID, TENANT_ID))
                .thenReturn(Optional.of(application));

        ApplicationEvent event = ApplicationEvent.statusChange(
                application, ApplicationStatus.DRAFT, ApplicationStatus.PENDING, "Submitted");
        event.setId(UUID.randomUUID());

        when(eventRepository.findByApplicationIdAndTenantIdOrderByCreatedAtDesc(APP_ID, TENANT_ID))
                .thenReturn(List.of(event));

        // When
        List<ApplicationTimeline> timeline = service.getTimeline(APP_ID, USER_ID);

        // Then
        assertThat(timeline).hasSize(1);
        assertThat(timeline.get(0).eventType()).isEqualTo("STATUS_CHANGED");
        assertThat(timeline.get(0).oldStatus()).isEqualTo(ApplicationStatus.DRAFT);
        assertThat(timeline.get(0).newStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    @DisplayName("getTimeline() throws when application not found")
    void getTimeline_notFound_throws() {
        // Given
        when(applicationRepository.findByIdAndUserIdAndTenantId(APP_ID, USER_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.getTimeline(APP_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
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
        job.setLocation("Berlin");
        return job;
    }

    private CvDetails createTestCv() {
        CvDetails cv = CvDetails.builder()
                .fileName("resume.pdf")
                .build();
        cv.setId(CV_ID);
        cv.setTenantId(TENANT_ID);
        return cv;
    }

    private JobApplication createTestApplication() {
        JobApplication app = JobApplication.builder()
                .tenantId(TENANT_ID)
                .job(createTestJob())
                .cv(createTestCv())
                .status(ApplicationStatus.DRAFT)
                .build();
        app.setId(APP_ID);
        return app;
    }
}
