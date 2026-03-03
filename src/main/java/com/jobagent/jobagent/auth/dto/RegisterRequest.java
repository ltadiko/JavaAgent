package com.jobagent.jobagent.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Sprint 1.4 — Registration request DTO with Jakarta Validation.
 */
@Schema(description = "Request payload for user registration")
public record RegisterRequest(
        @Schema(description = "User's email address", example = "john.doe@example.com", format = "email", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @Schema(description = "User's password (8-128 characters)", example = "SecureP@ss123", format = "password", minLength = 8, maxLength = 128, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be 8-128 characters")
        String password,

        @Schema(description = "User's full name", example = "John Doe", minLength = 2, maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 255, message = "Name must be 2-255 characters")
        String fullName,

        @Schema(description = "ISO 3166-1 alpha-2 country code", example = "DE", pattern = "^[A-Z]{2}$", minLength = 2, maxLength = 2, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Country is required")
        @Size(min = 2, max = 2, message = "Country must be ISO 3166-1 alpha-2 (e.g., US, DE)")
        String country
) {}
