package com.jobagent.jobagent.auth.repository;

import com.jobagent.jobagent.common.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 2.2 — Integration tests for JpaRegisteredClientRepository.
 * Requires Docker PostgreSQL: docker compose up -d postgres
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
@DisplayName("JpaRegisteredClientRepository Integration Tests")
class JpaRegisteredClientRepositoryIntegrationTest {

    @Autowired
    private OAuth2RegisteredClientJpaRepository repo;

    @Autowired
    private JpaRegisteredClientRepository adapter;

    private UUID testTenantId;

    @BeforeEach
    void setUp() {
        // Set up tenant context for tests - TenantEntityListener requires this
        testTenantId = UUID.randomUUID();
        TenantContext.setTenantId(testTenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("save() should persist RegisteredClient entity")
    void saveShouldPersistRegisteredClient() {
        String clientId = "integration-spa-" + UUID.randomUUID();
        RegisteredClient rc = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret("secret")
                .clientName("Integration SPA")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:5173/callback")
                .scope("openid")
                .build();

        adapter.save(rc);

        Optional<com.jobagent.jobagent.auth.model.OAuth2RegisteredClient> found = repo.findByClientId(clientId);
        assertThat(found).isPresent();
        assertThat(found.get().getClientId()).isEqualTo(clientId);
    }

    @Test
    @DisplayName("findByClientId() should return empty for unknown client")
    void findByClientIdShouldReturnEmptyForUnknown() {
        Optional<com.jobagent.jobagent.auth.model.OAuth2RegisteredClient> found = repo.findByClientId("nonexistent-client");
        assertThat(found).isEmpty();
    }
}
