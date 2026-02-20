package com.jobagent.jobagent.auth.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 1.4 — Registration response DTO.
 */
public record RegisterResponse(
        UUID userId,
        String email,
        String fullName,
        String country,
        String region,
        Instant createdAt
) {}
