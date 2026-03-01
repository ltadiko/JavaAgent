package com.jobagent.jobagent.jobsearch.service;

import com.jobagent.jobagent.common.multitenancy.TenantContext;
import com.jobagent.jobagent.jobsearch.dto.CreateJobRequest;
import com.jobagent.jobagent.jobsearch.dto.JobListingResponse;
import com.jobagent.jobagent.jobsearch.dto.JobSearchRequest;
import com.jobagent.jobagent.jobsearch.model.EmploymentType;
import com.jobagent.jobagent.jobsearch.model.JobListing;
import com.jobagent.jobagent.jobsearch.model.JobStatus;
import com.jobagent.jobagent.jobsearch.model.RemoteType;
import com.jobagent.jobagent.jobsearch.repository.JobListingRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Sprint 5.8 — Unit tests for JobSearchService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JobSearchService Tests")
class JobSearchServiceTest {

    @Mock
    private JobListingRepository jobListingRepository;

    @InjectMocks
    private JobSearchService jobSearchService;

    private static MockedStatic<TenantContext> tenantContextMock;
    private static final UUID TENANT_ID = UUID.randomUUID();

    @BeforeAll
    static void setUpClass() {
        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::requireTenantId).thenReturn(TENANT_ID);
    }

    @AfterAll
    static void tearDownClass() {
        tenantContextMock.close();
    }

    @Nested
    @DisplayName("getActiveJobs")
    class GetActiveJobsTests {

        @Test
        @DisplayName("should return active jobs with pagination")
        void shouldReturnActiveJobsWithPagination() {
            // Given
            JobListing job = createTestJob();
            Page<JobListing> jobPage = new PageImpl<>(List.of(job));
            when(jobListingRepository.findByTenantIdAndStatus(eq(TENANT_ID), eq(JobStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(jobPage);

            // When
            Page<JobListingResponse> result = jobSearchService.getActiveJobs(0, 20);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).title()).isEqualTo("Senior Java Developer");
        }

        @Test
        @DisplayName("should return empty page when no jobs")
        void shouldReturnEmptyPageWhenNoJobs() {
            // Given
            when(jobListingRepository.findByTenantIdAndStatus(eq(TENANT_ID), eq(JobStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(Page.empty());

            // When
            Page<JobListingResponse> result = jobSearchService.getActiveJobs(0, 20);

            // Then
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchJobs")
    class SearchJobsTests {

        @Test
        @DisplayName("should search with keyword using full-text search")
        void shouldSearchWithKeyword() {
            // Given
            JobListing job = createTestJob();
            Page<JobListing> jobPage = new PageImpl<>(List.of(job));
            when(jobListingRepository.fullTextSearch(eq(TENANT_ID), eq("java developer"), any(Pageable.class)))
                    .thenReturn(jobPage);

            JobSearchRequest request = new JobSearchRequest(
                    "java developer", null, null, null, null, null, null, null, null, 0, 20);

            // When
            Page<JobListingResponse> result = jobSearchService.searchJobs(request);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(jobListingRepository).fullTextSearch(eq(TENANT_ID), eq("java developer"), any(Pageable.class));
        }

        @Test
        @DisplayName("should search with advanced criteria")
        void shouldSearchWithAdvancedCriteria() {
            // Given
            JobListing job = createTestJob();
            Page<JobListing> jobPage = new PageImpl<>(List.of(job));
            when(jobListingRepository.advancedSearch(
                    eq(TENANT_ID), eq(JobStatus.ACTIVE), eq("Developer"), eq("Tech"), eq("Amsterdam"), any(Pageable.class)))
                    .thenReturn(jobPage);

            JobSearchRequest request = new JobSearchRequest(
                    null, "Developer", "Tech", "Amsterdam", null, null, null, null, null, 0, 20);

            // When
            Page<JobListingResponse> result = jobSearchService.searchJobs(request);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getJobById")
    class GetJobByIdTests {

        @Test
        @DisplayName("should return job when found")
        void shouldReturnJobWhenFound() {
            // Given
            UUID jobId = UUID.randomUUID();
            JobListing job = createTestJob();
            job.setId(jobId);
            when(jobListingRepository.findByIdAndTenantId(jobId, TENANT_ID))
                    .thenReturn(Optional.of(job));

            // When
            JobListingResponse result = jobSearchService.getJobById(jobId);

            // Then
            assertThat(result.id()).isEqualTo(jobId);
            assertThat(result.title()).isEqualTo("Senior Java Developer");
        }

        @Test
        @DisplayName("should throw exception when job not found")
        void shouldThrowExceptionWhenJobNotFound() {
            // Given
            UUID jobId = UUID.randomUUID();
            when(jobListingRepository.findByIdAndTenantId(jobId, TENANT_ID))
                    .thenReturn(Optional.empty());

            // Then
            assertThatThrownBy(() -> jobSearchService.getJobById(jobId))
                    .hasMessageContaining("Job not found");
        }
    }

    @Nested
    @DisplayName("createJob")
    class CreateJobTests {

        @Test
        @DisplayName("should create new job")
        void shouldCreateNewJob() {
            // Given
            CreateJobRequest request = new CreateJobRequest(
                    "Backend Developer",
                    "Tech Corp",
                    "Remote",
                    "Build APIs",
                    "3+ years experience",
                    List.of("Java", "Spring"),
                    new BigDecimal("60000"),
                    new BigDecimal("80000"),
                    "EUR",
                    EmploymentType.FULL_TIME,
                    RemoteType.REMOTE,
                    "https://example.com/job",
                    null,
                    null
            );

            when(jobListingRepository.save(any(JobListing.class)))
                    .thenAnswer(inv -> {
                        JobListing job = inv.getArgument(0);
                        job.setId(UUID.randomUUID());
                        return job;
                    });

            // When
            JobListingResponse result = jobSearchService.createJob(request);

            // Then
            assertThat(result.title()).isEqualTo("Backend Developer");
            assertThat(result.company()).isEqualTo("Tech Corp");
            assertThat(result.skills()).containsExactly("java", "spring");
            verify(jobListingRepository).save(any(JobListing.class));
        }

        @Test
        @DisplayName("should return existing job when external ID exists")
        void shouldReturnExistingJobWhenExternalIdExists() {
            // Given
            JobListing existingJob = createTestJob();
            existingJob.setExternalId("EXT-123");

            when(jobListingRepository.findByTenantIdAndExternalId(TENANT_ID, "EXT-123"))
                    .thenReturn(Optional.of(existingJob));

            CreateJobRequest request = new CreateJobRequest(
                    "New Title", "Corp", null, null, null, null, null, null, null, null, null, null, "EXT-123", null);

            // When
            JobListingResponse result = jobSearchService.createJob(request);

            // Then
            assertThat(result.title()).isEqualTo("Senior Java Developer"); // Existing title
            verify(jobListingRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateJobStatus")
    class UpdateJobStatusTests {

        @Test
        @DisplayName("should update job status")
        void shouldUpdateJobStatus() {
            // Given
            UUID jobId = UUID.randomUUID();
            JobListing job = createTestJob();
            job.setId(jobId);
            job.setStatus(JobStatus.ACTIVE);

            when(jobListingRepository.findByIdAndTenantId(jobId, TENANT_ID))
                    .thenReturn(Optional.of(job));
            when(jobListingRepository.save(any(JobListing.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            JobListingResponse result = jobSearchService.updateJobStatus(jobId, JobStatus.FILLED);

            // Then
            assertThat(result.status()).isEqualTo(JobStatus.FILLED);
        }
    }

    @Nested
    @DisplayName("markExpiredJobs")
    class MarkExpiredJobsTests {

        @Test
        @DisplayName("should mark expired jobs")
        void shouldMarkExpiredJobs() {
            // Given
            JobListing expiredJob = createTestJob();
            expiredJob.setExpiresAt(Instant.now().minusSeconds(3600));

            when(jobListingRepository.findExpiredJobs(any(Instant.class)))
                    .thenReturn(List.of(expiredJob));

            // When
            int count = jobSearchService.markExpiredJobs();

            // Then
            assertThat(count).isEqualTo(1);
            assertThat(expiredJob.getStatus()).isEqualTo(JobStatus.EXPIRED);
            verify(jobListingRepository).saveAll(anyList());
        }
    }

    private JobListing createTestJob() {
        return JobListing.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .title("Senior Java Developer")
                .company("Tech Corp")
                .location("Amsterdam, NL")
                .description("Join our team!")
                .requirements("5+ years Java experience")
                .skills(List.of("java", "spring", "postgresql"))
                .salaryMin(new BigDecimal("70000"))
                .salaryMax(new BigDecimal("90000"))
                .salaryCurrency("EUR")
                .employmentType(EmploymentType.FULL_TIME)
                .remoteType(RemoteType.HYBRID)
                .status(JobStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
