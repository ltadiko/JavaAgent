package com.jobagent.jobagent.cv.service;

import java.io.InputStream;

/**
 * Sprint 3.4 — Abstract file storage operations.
 *
 * <p>Implementations: MinioFileStorageService (local/dev), S3FileStorageService (prod)
 */
public interface FileStorageService {

    /**
     * Upload a file to storage.
     *
     * @param key         the storage key (path)
     * @param inputStream the file content
     * @param contentType the MIME type
     * @param size        the file size in bytes
     */
    void upload(String key, InputStream inputStream, String contentType, long size);

    /**
     * Download a file from storage.
     *
     * @param key the storage key
     * @return the file content as InputStream
     */
    InputStream download(String key);

    /**
     * Generate a presigned URL for downloading a file.
     *
     * @param key              the storage key
     * @param expirationMinutes how long the URL is valid
     * @return the presigned URL
     */
    String generatePresignedDownloadUrl(String key, int expirationMinutes);

    /**
     * Delete a file from storage.
     *
     * @param key the storage key
     */
    void delete(String key);

    /**
     * Check if a file exists.
     *
     * @param key the storage key
     * @return true if the file exists
     */
    boolean exists(String key);
}
