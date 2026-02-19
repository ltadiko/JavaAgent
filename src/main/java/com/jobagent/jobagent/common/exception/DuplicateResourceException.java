package com.jobagent.jobagent.common.exception;

/**
 * Thrown when a duplicate resource is detected (e.g., applying to the same job twice).
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
