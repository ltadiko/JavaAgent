package com.jobagent.jobagent.cv.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Sprint 3.5 — MinIO/S3 file storage implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;

    @Value("${app.storage.cv-bucket:jobagent-cv}")
    private String cvBucket;

    @Override
    public void upload(String key, InputStream inputStream, String contentType, long size) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(cvBucket)
                    .object(key)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
            log.debug("Uploaded file to {}/{}", cvBucket, key);
        } catch (Exception e) {
            log.error("Failed to upload file to {}/{}: {}", cvBucket, key, e.getMessage());
            throw new StorageException("Failed to upload file: " + key, e);
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(cvBucket)
                    .object(key)
                    .build());
        } catch (Exception e) {
            log.error("Failed to download file {}/{}: {}", cvBucket, key, e.getMessage());
            throw new StorageException("Failed to download file: " + key, e);
        }
    }

    @Override
    public String generatePresignedDownloadUrl(String key, int expirationMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(cvBucket)
                    .object(key)
                    .method(Method.GET)
                    .expiry(expirationMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for {}/{}: {}", cvBucket, key, e.getMessage());
            throw new StorageException("Failed to generate presigned URL: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(cvBucket)
                    .object(key)
                    .build());
            log.debug("Deleted file {}/{}", cvBucket, key);
        } catch (Exception e) {
            log.error("Failed to delete file {}/{}: {}", cvBucket, key, e.getMessage());
            throw new StorageException("Failed to delete file: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(cvBucket)
                    .object(key)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Custom exception for storage operations.
     */
    public static class StorageException extends RuntimeException {
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
