package com.jobagent.jobagent.jobsearch.service;

import com.jobagent.jobagent.common.multitenancy.TenantContext;
import com.jobagent.jobagent.cv.dto.CvParsedData;
import com.jobagent.jobagent.cv.model.CvDetails;
import com.jobagent.jobagent.cv.model.CvStatus;
import com.jobagent.jobagent.cv.repository.CvDetailsRepository;
import com.jobagent.jobagent.jobsearch.dto.JobMatchScore;
import com.jobagent.jobagent.jobsearch.model.JobListing;
import com.jobagent.jobagent.jobsearch.model.JobStatus;
import com.jobagent.jobagent.jobsearch.repository.JobListingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Sprint 5.8 — Unit tests for JobMatchingService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JobMatchingService Tests")
class JobMatchingServiceTest {

    @Mock
    private JobListingRepository jobListingRepository;

    @Mock
    private CvDetailsRepository cvDetailsRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private JobMatchingService jobMatchingService;

    private static MockedStatic<TenantContext> tenantContextMock;
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

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
    @DisplayName("getMatchedJobs")
    class GetMatchedJobsTests {

        @Test
        @DisplayName("should return matched jobs sorted by score")
        void shouldReturnMatchedJobsSortedByScore() throws Exception {
            // Given
            setupUserWithSkills(List.of("java", "spring", "postgresql", "docker"));

            JobListing job1 = createJob("Java Developer", List.of("java", "spring"));  // 100% match
            JobListing job2 = createJob("Full Stack", List.of("java", "react", "node")); // 33% match
            JobListing job3 = createJob("Backend Lead", List.of("java", "spring", "postgresql")); // 75% match

            Page<JobListing> jobs = new PageImpl<>(List.of(job1, job2, job3));
            when(jobListingRepository.findByTenantIdAndStatus(eq(TENANT_ID), eq(JobStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(jobs);

            // When
            Page<JobMatchScore> result = jobMatchingService.getMatchedJobs(USER_ID, 30, 0, 20);

            // Then
            assertThat(result.getContent()).hasSize(3);
            // Should be sorted by match percentage descending
            assertThat(result.getContent().get(0).matchPercentage()).isEqualTo(100);
            assertThat(result.getContent().get(1).matchPercentage()).isEqualTo(100); // 3/3 = 100%
            assertThat(result.getContent().get(2).matchPercentage()).isEqualTo(33);  // 1/3 ≈ 33%
        }

        @Test
        @DisplayName("should filter by minimum match percentage")
        void shouldFilterByMinMatchPercentage() throws Exception {
            // Given
            setupUserWithSkills(List.of("java", "spring"));

            JobListing job1 = createJob("Java Dev", List.of("java", "spring")); // 100%
            JobListing job2 = createJob("Python Dev", List.of("python", "django")); // 0%

            Page<JobListing> jobs = new PageImpl<>(List.of(job1, job2));
            when(jobListingRepository.findByTenantIdAndStatus(eq(TENANT_ID), eq(JobStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(jobs);

            // When
            Page<JobMatchScore> result = jobMatchingService.getMatchedJobs(USER_ID, 50, 0, 20);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).title()).isEqualTo("Java Dev");
        }

        @Test
        @DisplayName("should return empty when user has no skills")
        void shouldReturnEmptyWhenUserHasNoSkills() {
            // Given
            when(cvDetailsRepository.findTopByUserIdAndTenantIdAndStatusOrderByCreatedAtDesc(
                    USER_ID, TENANT_ID, CvStatus.PARSED))
                    .thenReturn(Optional.empty());

            // When
            Page<JobMatchScore> result = jobMatchingService.getMatchedJobs(USER_ID, 0, 0, 20);

            // Then
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("calculateMatchForJob")
    class CalculateMatchForJobTests {

        @Test
        @DisplayName("should calculate match for specific job")
        void shouldCalculateMatchForSpecificJob() throws Exception {
            // Given
            UUID jobId = UUID.randomUUID();
            setupUserWithSkills(List.of("java", "spring", "postgresql"));

            JobListing job = createJob("Backend", List.of("java", "spring", "kafka"));
            job.setId(jobId);
            when(jobListingRepository.findByIdAndTenantId(jobId, TENANT_ID))
                    .thenReturn(Optional.of(job));

            // When
            JobMatchScore result = jobMatchingService.calculateMatchForJob(USER_ID, jobId);

            // Then
            assertThat(result.matchPercentage()).isEqualTo(67); // 2/3 ≈ 67%
            assertThat(result.matchedSkills()).containsExactlyInAnyOrder("java", "spring");
            assertThat(result.missingSkills()).containsExactly("kafka");
        }

        @Test
        @DisplayName("should return 100% for job with no required skills")
        void shouldReturn100ForJobWithNoSkills() throws Exception {
            // Given
            UUID jobId = UUID.randomUUID();
            setupUserWithSkills(List.of("java"));

            JobListing job = createJob("Entry Level", List.of());
            job.setId(jobId);
            when(jobListingRepository.findByIdAndTenantId(jobId, TENANT_ID))
                    .thenReturn(Optional.of(job));

            // When
            JobMatchScore result = jobMatchingService.calculateMatchForJob(USER_ID, jobId);

            // Then
            assertThat(result.matchPercentage()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("getTopMatches")
    class GetTopMatchesTests {

        @Test
        @DisplayName("should return top N matches")
        void shouldReturnTopNMatches() throws Exception {
            // Given
            setupUserWithSkills(List.of("java", "spring", "kubernetes"));

            JobListing job1 = createJob("Job1", List.of("java"));
            JobListing job2 = createJob("Job2", List.of("java", "spring"));
            JobListing job3 = createJob("Job3", List.of("java", "spring", "kubernetes"));
            JobListing job4 = createJob("Job4", List.of("python"));

            Page<JobListing> jobs = new PageImpl<>(List.of(job1, job2, job3, job4));
            when(jobListingRepository.findByTenantIdAndStatus(eq(TENANT_ID), eq(JobStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(jobs);

            // When
            List<JobMatchScore> result = jobMatchingService.getTopMatches(USER_ID, 2);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).matchPercentage()).isEqualTo(100);
            assertThat(result.get(1).matchPercentage()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Skill Matching")
    class SkillMatchingTests {

        @Test
        @DisplayName("should match partial skills")
        void shouldMatchPartialSkills() throws Exception {
            // Given
            setupUserWithSkills(List.of("spring boot", "java 17"));

            UUID jobId = UUID.randomUUID();
            JobListing job = createJob("Dev", List.of("java", "spring"));
            job.setId(jobId);
            when(jobListingRepository.findByIdAndTenantId(jobId, TENANT_ID))
                    .thenReturn(Optional.of(job));

            // When
            JobMatchScore result = jobMatchingService.calculateMatchForJob(USER_ID, jobId);

            // Then
            assertThat(result.matchPercentage()).isEqualTo(100); // Partial match works
        }

        @Test
        @DisplayName("should match skill aliases")
        void shouldMatchSkillAliases() throws Exception {
            // Given
            setupUserWithSkills(List.of("js", "k8s", "postgres"));

            UUID jobId = UUID.randomUUID();
            JobListing job = createJob("Dev", List.of("javascript", "kubernetes", "postgresql"));
            job.setId(jobId);
            when(jobListingRepository.findByIdAndTenantId(jobId, TENANT_ID))
                    .thenReturn(Optional.of(job));

            // When
            JobMatchScore result = jobMatchingService.calculateMatchForJob(USER_ID, jobId);

            // Then
            assertThat(result.matchPercentage()).isEqualTo(100); // Aliases match
        }
    }

    private void setupUserWithSkills(List<String> skills) throws Exception {
        CvParsedData parsedData = new CvParsedData(
                "John Doe", "john@example.com", "+1234567890",
                "Senior Developer", "5 years experience", skills,
                List.of(), List.of(), List.of(), List.of()
        );

        CvDetails cvDetails = CvDetails.builder()
                .status(CvStatus.PARSED)
                .parsedJson(objectMapper.writeValueAsString(parsedData))
                .build();
        cvDetails.setTenantId(TENANT_ID);

        when(cvDetailsRepository.findTopByUserIdAndTenantIdAndStatusOrderByCreatedAtDesc(
                USER_ID, TENANT_ID, CvStatus.PARSED))
                .thenReturn(Optional.of(cvDetails));
    }

    private JobListing createJob(String title, List<String> skills) {
        return JobListing.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .title(title)
                .company("Test Corp")
                .location("Remote")
                .skills(skills.stream().map(String::toLowerCase).toList())
                .status(JobStatus.ACTIVE)
                .build();
    }
}
