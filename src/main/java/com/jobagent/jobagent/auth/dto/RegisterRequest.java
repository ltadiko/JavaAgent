package com.jobagent.jobagent.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Sprint 1.4 — Registration request DTO with Jakarta Validation.
 */
public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be 8-128 characters")
        String password,

        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 255, message = "Name must be 2-255 characters")
        String fullName,

        @NotBlank(message = "Country is required")
        @Size(min = 2, max = 2, message = "Country must be ISO 3166-1 alpha-2 (e.g., US, DE)")
        String country
) {}
