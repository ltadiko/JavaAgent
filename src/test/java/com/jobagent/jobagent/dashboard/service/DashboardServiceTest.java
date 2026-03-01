package com.jobagent.jobagent.dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.jobagent.application.model.ApplicationStatus;
import com.jobagent.jobagent.application.model.JobApplication;
import com.jobagent.jobagent.application.repository.JobApplicationRepository;
import com.jobagent.jobagent.auth.model.User;
import com.jobagent.jobagent.auth.repository.UserRepository;
import com.jobagent.jobagent.common.multitenancy.TenantContext;
import com.jobagent.jobagent.cv.model.CvDetails;
import com.jobagent.jobagent.cv.model.CvStatus;
import com.jobagent.jobagent.cv.repository.CvDetailsRepository;
import com.jobagent.jobagent.dashboard.dto.*;
import com.jobagent.jobagent.jobsearch.model.JobListing;
import com.jobagent.jobagent.motivation.repository.MotivationLetterRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Sprint 8.5 — Unit tests for DashboardService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Tests")
class DashboardServiceTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @Mock
    private CvDetailsRepository cvRepository;

    @Mock
    private JobApplicationRepository applicationRepository;

    @Mock
    private MotivationLetterRepository letterRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DashboardService service;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("getDashboardSummary() returns complete summary")
    void getDashboardSummary_returnsSummary() {
        // Given
        User user = createTestUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        Page<CvDetails> cvPage = new PageImpl<>(List.of());
        when(cvRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(
                eq(USER_ID), eq(TENANT_ID), any(PageRequest.class))).thenReturn(cvPage);
        when(cvRepository.findTopByUserIdAndTenantIdAndStatusOrderByCreatedAtDesc(
                USER_ID, TENANT_ID, CvStatus.PARSED)).thenReturn(Optional.empty());

        when(applicationRepository.getStatusCounts(USER_ID, TENANT_ID)).thenReturn(List.of());

        when(letterRepository.countByUserIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(0L);
        Page emptyPage = new PageImpl<>(List.of());
        when(letterRepository.findByUserIdAndTenantIdOrderByUpdatedAtDesc(
                eq(USER_ID), eq(TENANT_ID), any(PageRequest.class))).thenReturn(emptyPage);

        // When
        DashboardSummary summary = service.getDashboardSummary(USER_ID);

        // Then
        assertThat(summary).isNotNull();
        assertThat(summary.user()).isNotNull();
        assertThat(summary.user().name()).isEqualTo("Test User");
        assertThat(summary.cv()).isNotNull();
        assertThat(summary.applications()).isNotNull();
        assertThat(summary.letters()).isNotNull();
    }

    @Test
    @DisplayName("getDashboardSummary() includes user info")
    void getDashboardSummary_includesUserInfo() {
        // Given
        User user = createTestUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        Page<CvDetails> cvPage = new PageImpl<>(List.of());
        when(cvRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(
                eq(USER_ID), eq(TENANT_ID), any(PageRequest.class))).thenReturn(cvPage);
        when(cvRepository.findTopByUserIdAndTenantIdAndStatusOrderByCreatedAtDesc(
                USER_ID, TENANT_ID, CvStatus.PARSED)).thenReturn(Optional.empty());

        when(applicationRepository.getStatusCounts(USER_ID, TENANT_ID)).thenReturn(List.of());

        when(letterRepository.countByUserIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(0L);
        when(letterRepository.findByUserIdAndTenantIdOrderByUpdatedAtDesc(
                eq(USER_ID), eq(TENANT_ID), any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        // When
        DashboardSummary summary = service.getDashboardSummary(USER_ID);

        // Then
        assertThat(summary.user().email()).isEqualTo("test@example.com");
        assertThat(summary.user().region()).isEqualTo("EU");
    }

    @Test
    @DisplayName("getDashboardSummary() includes application stats")
    void getDashboardSummary_includesApplicationStats() {
        // Given
        User user = createTestUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        Page<CvDetails> cvPage = new PageImpl<>(List.of());
        when(cvRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(
                eq(USER_ID), eq(TENANT_ID), any(PageRequest.class))).thenReturn(cvPage);
        when(cvRepository.findTopByUserIdAndTenantIdAndStatusOrderByCreatedAtDesc(
                USER_ID, TENANT_ID, CvStatus.PARSED)).thenReturn(Optional.empty());

        // Mock application stats
        List<Object[]> statusCounts = List.of(
                new Object[]{ApplicationStatus.SENT, 5L},
                new Object[]{ApplicationStatus.INTERVIEW, 2L},
                new Object[]{ApplicationStatus.REJECTED, 1L}
        );
        when(applicationRepository.getStatusCounts(USER_ID, TENANT_ID)).thenReturn(statusCounts);

        when(letterRepository.countByUserIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(0L);
        when(letterRepository.findByUserIdAndTenantIdOrderByUpdatedAtDesc(
                eq(USER_ID), eq(TENANT_ID), any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        // When
        DashboardSummary summary = service.getDashboardSummary(USER_ID);

        // Then
        assertThat(summary.applications().total()).isEqualTo(8);
        assertThat(summary.applications().sent()).isEqualTo(5);
        assertThat(summary.applications().interviews()).isEqualTo(2);
        assertThat(summary.applications().rejected()).isEqualTo(1);
    }

    @Test
    @DisplayName("getRecentActivity() returns activity list")
    void getRecentActivity_returnsActivities() {
        // Given
        JobApplication app = createTestApplication();
        Page<JobApplication> page = new PageImpl<>(List.of(app));

        when(applicationRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(
                eq(USER_ID), eq(TENANT_ID), any(PageRequest.class))).thenReturn(page);

        // When
        List<RecentActivity> activities = service.getRecentActivity(USER_ID, 10);

        // Then
        assertThat(activities).hasSize(1);
        assertThat(activities.get(0).entityType()).isEqualTo("APPLICATION");
    }

    @Test
    @DisplayName("getRecentActivity() limits results")
    void getRecentActivity_limitsResults() {
        // Given
        Page<JobApplication> page = new PageImpl<>(List.of());
        when(applicationRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(
                eq(USER_ID), eq(TENANT_ID), any(PageRequest.class))).thenReturn(page);

        // When
        List<RecentActivity> activities = service.getRecentActivity(USER_ID, 5);

        // Then
        verify(applicationRepository).findByUserIdAndTenantIdOrderByCreatedAtDesc(
                eq(USER_ID), eq(TENANT_ID), eq(PageRequest.of(0, 5)));
    }

    private User createTestUser() {
        User user = User.builder()
                .email("test@example.com")
                .emailHash("hash")
                .fullName("Test User")
                .region("EU")
                .build();
        user.setId(USER_ID);
        user.setCreatedAt(Instant.now().minusSeconds(86400 * 30)); // 30 days ago
        return user;
    }

    private JobApplication createTestApplication() {
        JobListing job = new JobListing();
        job.setId(UUID.randomUUID());
        job.setTitle("Software Engineer");
        job.setCompany("Tech Corp");

        JobApplication app = JobApplication.builder()
                .tenantId(TENANT_ID)
                .job(job)
                .status(ApplicationStatus.SENT)
                .build();
        app.setId(UUID.randomUUID());
        app.setUpdatedAt(Instant.now());
        return app;
    }
}
