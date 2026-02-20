package com.jobagent.jobagent.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobagent.jobagent.auth.dto.RegisterRequest;
import com.jobagent.jobagent.auth.dto.RegisterResponse;
import com.jobagent.jobagent.auth.service.UserService;
import com.jobagent.jobagent.common.exception.DuplicateResourceException;
import com.jobagent.jobagent.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Sprint 1.6 — Integration tests for AuthController.
 *
 * <p>Uses standalone MockMvc setup to avoid Spring Boot 4 auto-configuration
 * issues with security, DataSource, and OAuth2 beans in @WebMvcTest slices.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    @SuppressWarnings("removal")
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        MappingJackson2HttpMessageConverter jacksonConverter =
                new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(jacksonConverter)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .build();
    }

    // ─── Helper ────────────────────────────────────────────────────────────

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    private RegisterRequest validRequest() {
        return new RegisterRequest("test@example.com", "password123", "Test User", "DE");
    }

    // ─── 201 — Happy path ──────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/auth/register — valid request → 201 Created")
    void shouldReturn201ForValidRegistration() throws Exception {
        RegisterResponse response = new RegisterResponse(
                UUID.randomUUID(), "test@example.com", "Test User",
                "DE", "EU", Instant.now()
        );
        when(userService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.country").value("DE"))
                .andExpect(jsonPath("$.region").value("EU"))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(userService, times(1)).register(any(RegisterRequest.class));
    }

    // ─── 400 — Validation failures ─────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/auth/register — invalid email → 400")
    void shouldReturn400ForInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RegisterRequest("not-an-email", "password123", "Test User", "DE"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — short password → 400")
    void shouldReturn400ForShortPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RegisterRequest("test@example.com", "abc", "Test User", "DE"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — blank fullName → 400")
    void shouldReturn400ForMissingFullName() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RegisterRequest("test@example.com", "password123", "", "DE"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — country too long (3 chars) → 400")
    void shouldReturn400ForInvalidCountry() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RegisterRequest("test@example.com", "password123", "Test User", "USA"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — empty body → 400")
    void shouldReturn400ForMissingBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    // ─── 409 — Duplicate email ─────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/auth/register — duplicate email → 409 Conflict")
    void shouldReturn409ForDuplicateEmail() throws Exception {
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("User already exists with email: test@example.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate Resource"))
                .andExpect(jsonPath("$.detail").value("User already exists with email: test@example.com"));
    }

    // ─── Content-Type assertion ────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/auth/register — response Content-Type is application/json")
    void shouldReturnJsonContentType() throws Exception {
        RegisterResponse response = new RegisterResponse(
                UUID.randomUUID(), "json@example.com", "JSON User",
                "US", "US", Instant.now()
        );
        when(userService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RegisterRequest("json@example.com", "password123", "JSON User", "US"))))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
