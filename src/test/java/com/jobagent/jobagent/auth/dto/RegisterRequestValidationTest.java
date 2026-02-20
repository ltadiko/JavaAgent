package com.jobagent.jobagent.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 1.4 — Validation tests for RegisterRequest.
 */
@DisplayName("RegisterRequest Validation Tests")
class RegisterRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private RegisterRequest validRequest() {
        return new RegisterRequest("user@example.com", "password123", "John Doe", "US");
    }

    @Test
    @DisplayName("Should pass for valid request")
    void shouldPassForValidRequest() {
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(validRequest());
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail for blank email")
    void shouldFailForBlankEmail() {
        RegisterRequest req = new RegisterRequest("", "password123", "John Doe", "US");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("Should fail for null email")
    void shouldFailForNullEmail() {
        RegisterRequest req = new RegisterRequest(null, "password123", "John Doe", "US");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("Should fail for invalid email format")
    void shouldFailForInvalidEmailFormat() {
        RegisterRequest req = new RegisterRequest("not-an-email", "password123", "John Doe", "US");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("Should fail for blank password")
    void shouldFailForBlankPassword() {
        RegisterRequest req = new RegisterRequest("user@example.com", "", "John Doe", "US");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Should fail for short password (less than 8 chars)")
    void shouldFailForShortPassword() {
        RegisterRequest req = new RegisterRequest("user@example.com", "abc", "John Doe", "US");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Should fail for blank full name")
    void shouldFailForBlankFullName() {
        RegisterRequest req = new RegisterRequest("user@example.com", "password123", "", "US");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
    }

    @Test
    @DisplayName("Should fail for country code too short")
    void shouldFailForCountryTooShort() {
        RegisterRequest req = new RegisterRequest("user@example.com", "password123", "John Doe", "U");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("country"));
    }

    @Test
    @DisplayName("Should fail for country code too long")
    void shouldFailForCountryTooLong() {
        RegisterRequest req = new RegisterRequest("user@example.com", "password123", "John Doe", "USA");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("country"));
    }

    @Test
    @DisplayName("Should report multiple violations at once")
    void shouldReportMultipleViolations() {
        RegisterRequest req = new RegisterRequest("", "", "", "");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertThat(violations.size()).isGreaterThanOrEqualTo(4);
    }
}
