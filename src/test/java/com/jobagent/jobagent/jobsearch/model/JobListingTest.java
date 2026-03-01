package com.jobagent.jobagent.jobsearch.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 5.8 — Unit tests for JobListing entity.
 */
@DisplayName("JobListing Entity Tests")
class JobListingTest {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("should build with all fields")
        void shouldBuildWithAllFields() {
            UUID tenantId = UUID.randomUUID();

            JobListing job = JobListing.builder()
                    .tenantId(tenantId)
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
                    .sourceUrl("https://jobs.example.com/123")
                    .externalId("EXT-123")
                    .status(JobStatus.ACTIVE)
                    .build();

            assertThat(job.getTenantId()).isEqualTo(tenantId);
            assertThat(job.getTitle()).isEqualTo("Senior Java Developer");
            assertThat(job.getCompany()).isEqualTo("Tech Corp");
            assertThat(job.getLocation()).isEqualTo("Amsterdam, NL");
            assertThat(job.getSkills()).containsExactly("java", "spring", "postgresql");
            assertThat(job.getSalaryMin()).isEqualByComparingTo("70000");
            assertThat(job.getSalaryMax()).isEqualByComparingTo("90000");
            assertThat(job.getEmploymentType()).isEqualTo(EmploymentType.FULL_TIME);
            assertThat(job.getRemoteType()).isEqualTo(RemoteType.HYBRID);
            assertThat(job.getStatus()).isEqualTo(JobStatus.ACTIVE);
        }

        @Test
        @DisplayName("should have default status ACTIVE")
        void shouldHaveDefaultStatusActive() {
            JobListing job = JobListing.builder()
                    .tenantId(UUID.randomUUID())
                    .title("Developer")
                    .company("Corp")
                    .build();

            assertThat(job.getStatus()).isEqualTo(JobStatus.ACTIVE);
        }

        @Test
        @DisplayName("should have default currency EUR")
        void shouldHaveDefaultCurrencyEur() {
            JobListing job = JobListing.builder()
                    .tenantId(UUID.randomUUID())
                    .title("Developer")
                    .company("Corp")
                    .build();

            assertThat(job.getSalaryCurrency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("should have default empty skills list")
        void shouldHaveDefaultEmptySkillsList() {
            JobListing job = JobListing.builder()
                    .tenantId(UUID.randomUUID())
                    .title("Developer")
                    .company("Corp")
                    .build();

            assertThat(job.getSkills()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("Expiration Tests")
    class ExpirationTests {

        @Test
        @DisplayName("should detect expired job")
        void shouldDetectExpiredJob() {
            JobListing job = JobListing.builder()
                    .tenantId(UUID.randomUUID())
                    .title("Developer")
                    .company("Corp")
                    .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                    .build();

            assertThat(job.isExpired()).isTrue();
        }

        @Test
        @DisplayName("should detect non-expired job")
        void shouldDetectNonExpiredJob() {
            JobListing job = JobListing.builder()
                    .tenantId(UUID.randomUUID())
                    .title("Developer")
                    .company("Corp")
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .build();

            assertThat(job.isExpired()).isFalse();
        }

        @Test
        @DisplayName("should handle null expiration")
        void shouldHandleNullExpiration() {
            JobListing job = JobListing.builder()
                    .tenantId(UUID.randomUUID())
                    .title("Developer")
                    .company("Corp")
                    .expiresAt(null)
                    .build();

            assertThat(job.isExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("Availability Tests")
    class AvailabilityTests {

        @Test
        @DisplayName("should be available when active and not expired")
        void shouldBeAvailableWhenActiveAndNotExpired() {
            JobListing job = JobListing.builder()
                    .tenantId(UUID.randomUUID())
                    .title("Developer")
                    .company("Corp")
                    .status(JobStatus.ACTIVE)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .build();

            assertThat(job.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("should not be available when expired")
        void shouldNotBeAvailableWhenExpired() {
            JobListing job = JobListing.builder()
                    .tenantId(UUID.randomUUID())
                    .title("Developer")
                    .company("Corp")
                    .status(JobStatus.ACTIVE)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                    .build();

            assertThat(job.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("should not be available when status is not ACTIVE")
        void shouldNotBeAvailableWhenNotActive() {
            JobListing job = JobListing.builder()
                    .tenantId(UUID.randomUUID())
                    .title("Developer")
                    .company("Corp")
                    .status(JobStatus.FILLED)
                    .build();

            assertThat(job.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Skills Tests")
    class SkillsTests {

        @Test
        @DisplayName("should add skill")
        void shouldAddSkill() {
            JobListing job = JobListing.builder()
                    .tenantId(UUID.randomUUID())
                    .title("Developer")
                    .company("Corp")
                    .build();

            job.addSkill("Java");
            job.addSkill("Spring");

            assertThat(job.getSkills()).contains("java", "spring");
        }

        @Test
        @DisplayName("should normalize skills to lowercase")
        void shouldNormalizeSkillsToLowercase() {
            JobListing job = JobListing.builder()
                    .tenantId(UUID.randomUUID())
                    .title("Developer")
                    .company("Corp")
                    .build();

            job.addSkill("JAVA");
            job.addSkill("Spring Boot");

            assertThat(job.getSkills()).contains("java", "spring boot");
        }

        @Test
        @DisplayName("should ignore blank skills")
        void shouldIgnoreBlankSkills() {
            JobListing job = JobListing.builder()
                    .tenantId(UUID.randomUUID())
                    .title("Developer")
                    .company("Corp")
                    .build();

            job.addSkill("Java");
            job.addSkill("");
            job.addSkill("   ");
            job.addSkill(null);

            assertThat(job.getSkills()).containsExactly("java");
        }
    }

    @Nested
    @DisplayName("Salary Range Tests")
    class SalaryRangeTests {

        @Test
        @DisplayName("should format full salary range")
        void shouldFormatFullSalaryRange() {
            JobListing job = JobListing.builder()
                    .tenantId(UUID.randomUUID())
                    .title("Developer")
                    .company("Corp")
                    .salaryMin(new BigDecimal("70000"))
                    .salaryMax(new BigDecimal("90000"))
                    .salaryCurrency("EUR")
                    .build();

            assertThat(job.getSalaryRange()).contains("EUR 70000", "EUR 90000");
        }

        @Test
        @DisplayName("should handle only min salary")
        void shouldHandleOnlyMinSalary() {
            JobListing job = JobListing.builder()
                    .tenantId(UUID.randomUUID())
                    .title("Developer")
                    .company("Corp")
                    .salaryMin(new BigDecimal("70000"))
                    .salaryCurrency("EUR")
                    .build();

            assertThat(job.getSalaryRange()).isEqualTo("EUR 70000");
        }

        @Test
        @DisplayName("should return null when no salary")
        void shouldReturnNullWhenNoSalary() {
            JobListing job = JobListing.builder()
                    .tenantId(UUID.randomUUID())
                    .title("Developer")
                    .company("Corp")
                    .build();

            assertThat(job.getSalaryRange()).isNull();
        }
    }
}
