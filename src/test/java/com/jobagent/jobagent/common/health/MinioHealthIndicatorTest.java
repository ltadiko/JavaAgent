package com.jobagent.jobagent.common.health;

import io.minio.MinioClient;
import io.minio.BucketExistsArgs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Sprint 10.6 — Unit tests for MinioHealthIndicator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MinioHealthIndicator Tests")
class MinioHealthIndicatorTest {

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private MinioHealthIndicator indicator;

    @Test
    @DisplayName("Reports UP when MinIO is available")
    void health_available_returnsUp() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("bucket-cv", "available");
    }

    @Test
    @DisplayName("Reports UP with missing bucket")
    void health_bucketMissing_returnsUp() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("bucket-cv", "missing");
    }

    @Test
    @DisplayName("Reports DOWN when MinIO throws")
    void health_exception_returnsDown() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
    }
}
