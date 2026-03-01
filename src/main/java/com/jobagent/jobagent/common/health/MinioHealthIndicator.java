package com.jobagent.jobagent.common.health;

import io.minio.MinioClient;
import io.minio.BucketExistsArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Sprint 10.2 — MinIO storage health indicator.
 */
@Component("minio")
@RequiredArgsConstructor
@Slf4j
public class MinioHealthIndicator implements HealthIndicator {

    private final MinioClient minioClient;

    @Override
    public Health health() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket("jobagent-cv").build());
            return Health.up()
                    .withDetail("bucket-cv", exists ? "available" : "missing")
                    .build();
        } catch (Exception e) {
            log.warn("MinIO health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
