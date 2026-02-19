package com.jobagent.jobagent.common.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures MinIO / S3 client using app.storage.* properties.
 */
@Configuration
@Slf4j
public class StorageConfig {

    @Bean
    public MinioClient minioClient(AppProperties props) {
        AppProperties.Storage storage = props.getStorage();
        MinioClient client = MinioClient.builder()
                .endpoint(storage.getEndpoint())
                .credentials(storage.getAccessKey(), storage.getSecretKey())
                .region(storage.getRegion())
                .build();

        // Ensure buckets exist on startup (local dev convenience)
        ensureBucket(client, storage.getBucket().getCv());
        ensureBucket(client, storage.getBucket().getLetters());

        return client;
    }

    private void ensureBucket(MinioClient client, String bucketName) {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created S3 bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.warn("Could not ensure bucket '{}' exists: {}", bucketName, e.getMessage());
        }
    }
}
