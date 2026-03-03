package com.jobagent.jobagent.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 1.4 — Registration response DTO.
 */
@Schema(description = "Response returned after successful user registration")
public record RegisterResponse(
        @Schema(description = "Unique user identifier", example = "550e8400-e29b-41d4-a716-446655440000", format = "uuid")
        UUID userId,

        @Schema(description = "Registered email address", example = "john.doe@example.com", format = "email")
        String email,

        @Schema(description = "User's full name", example = "John Doe")
        String fullName,

        @Schema(description = "ISO 3166-1 alpha-2 country code", example = "DE")
        String country,

        @Schema(description = "Data residency region (e.g., eu-west, us-east)", example = "eu-west")
        String region,

        @Schema(description = "Account creation timestamp", example = "2026-03-03T10:15:30Z", format = "date-time")
        Instant createdAt
) {}
