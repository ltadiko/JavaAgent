package com.jobagent.jobagent.common.security;

import com.jobagent.jobagent.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 2.1.5 — Security configuration integration tests.
 * Uses Testcontainers for PostgreSQL.
 */
@AutoConfigureMockMvc
@DisplayName("SecurityConfig Integration Tests")
class SecurityConfigIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET / is accessible without authentication")
    void rootEndpoint_noAuth_returns200() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /actuator/health is accessible without authentication")
    void healthEndpoint_noAuth_returnsWithoutUnauthorized() throws Exception {
        // Health may return 503 if dependencies (Redis, MinIO) are unavailable
        // The key assertion is that it doesn't return 401 (unauthorized)
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().is(org.hamcrest.Matchers.not(401)));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register is accessible without authentication")
    void registerEndpoint_noAuth_acceptsRequest() throws Exception {
        // Even with invalid body, should not return 401 (might return 400 for validation)
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest()); // Bad request, not unauthorized
    }

    @Test
    @DisplayName("GET /api/v1/cv without token returns 401")
    void protectedEndpoint_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/cv"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/jobs without token returns 401")
    void jobsEndpoint_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/applications without token returns 401")
    void applicationsEndpoint_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
