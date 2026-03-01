package com.jobagent.jobagent.auth.model;

import com.jobagent.jobagent.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 1.1 — Integration test for User entity.
 * Tests JPA persistence, tenant_id auto-assignment, and database constraints.
 * Uses Testcontainers for PostgreSQL.
 */
@Transactional
@DisplayName("User Entity Integration Tests")
class UserIntegrationTest extends AbstractIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        super.setUpTenantContext();
    }

    @Test
    @DisplayName("Should persist User to database")
    void shouldPersistUserToDatabase() {
        // Given
        User user = User.builder()
            .email("integration.test@example.com")
            .emailHash("sha256_integration_hash")
            .passwordHash("bcrypt_password")
            .fullName("Integration Test User")
            .country("US")
            .region("US")
            .authProvider("LOCAL")
            .enabled(true)
            .build();

        // When
        entityManager.persist(user);
        entityManager.flush();

        // Then
        assertThat(user.getId()).isNotNull();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("integration.test@example.com");
        assertThat(user.getEmailHash()).isEqualTo("sha256_integration_hash");
    }

    @Test
    @DisplayName("Should set tenant_id from TenantContext on persist")
    void shouldAutoGenerateTenantId() {
        // Given
        User user = User.builder()
            .email("tenant.test@example.com")
            .emailHash("sha256_tenant_hash")
            .passwordHash("bcrypt_password")
            .fullName("Tenant Test User")
            .country("DE")
            .region("EU")
            .build();

        // tenantId not set explicitly on entity
        assertThat(user.getTenantId()).isNull();

        // When
        entityManager.persist(user);
        entityManager.flush();

        // Then - tenant_id should be set from TenantContext by TenantEntityListener
        assertThat(user.getTenantId()).isNotNull();
        assertThat(user.getTenantId()).isEqualTo(testTenantId);
    }

    @Test
    @DisplayName("Should retrieve User by ID")
    void shouldRetrieveUserById() {
        // Given
        User user = User.builder()
            .email("retrieve.test@example.com")
            .emailHash("sha256_retrieve_hash")
            .passwordHash("bcrypt_password")
            .fullName("Retrieve Test User")
            .country("FR")
            .region("EU")
            .build();

        entityManager.persist(user);
        entityManager.flush();
        UUID userId = user.getId();

        // Clear persistence context
        entityManager.clear();

        // When
        User retrievedUser = entityManager.find(User.class, userId);

        // Then
        assertThat(retrievedUser).isNotNull();
        assertThat(retrievedUser.getId()).isEqualTo(userId);
        assertThat(retrievedUser.getEmail()).isEqualTo("retrieve.test@example.com");
        assertThat(retrievedUser.getFullName()).isEqualTo("Retrieve Test User");
    }

    @Test
    @DisplayName("Should update User")
    void shouldUpdateUser() {
        // Given
        User user = User.builder()
            .email("update.test@example.com")
            .emailHash("sha256_update_hash")
            .passwordHash("bcrypt_password")
            .fullName("Update Test User")
            .country("JP")
            .region("APAC")
            .enabled(true)
            .build();

        entityManager.persist(user);
        entityManager.flush();
        UUID userId = user.getId();

        // When
        user.setFullName("Updated Name");
        user.setEnabled(false);
        entityManager.flush();
        entityManager.clear();

        // Then
        User updatedUser = entityManager.find(User.class, userId);
        assertThat(updatedUser.getFullName()).isEqualTo("Updated Name");
        assertThat(updatedUser.getEnabled()).isFalse();
        assertThat(updatedUser.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should persist User with null passwordHash for social login")
    void shouldPersistSocialLoginUser() {
        // Given
        User user = User.builder()
            .email("google.user@example.com")
            .emailHash("sha256_google_hash")
            .passwordHash(null)  // Social login
            .fullName("Google User")
            .country("US")
            .region("US")
            .authProvider("GOOGLE")
            .build();

        // When
        entityManager.persist(user);
        entityManager.flush();

        // Then
        assertThat(user.getId()).isNotNull();
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getAuthProvider()).isEqualTo("GOOGLE");
    }

    @Test
    @DisplayName("Should enforce unique email_hash constraint")
    void shouldEnforceUniqueEmailHashConstraint() {
        // Given
        String duplicateHash = "sha256_duplicate_hash_" + UUID.randomUUID();

        User user1 = User.builder()
            .email("user1@example.com")
            .emailHash(duplicateHash)
            .passwordHash("password1")
            .fullName("User 1")
            .country("US")
            .region("US")
            .build();

        User user2 = User.builder()
            .email("user2@example.com")
            .emailHash(duplicateHash)  // Same hash
            .passwordHash("password2")
            .fullName("User 2")
            .country("US")
            .region("US")
            .build();

        // When
        entityManager.persist(user1);
        entityManager.flush();

        // Then - should throw exception on duplicate email_hash
        try {
            entityManager.persist(user2);
            entityManager.flush();
            assertThat(false).as("Expected constraint violation").isTrue();
        } catch (Exception e) {
            // Expected - unique constraint violation
            assertThat(e.getMessage()).containsAnyOf("email_hash", "constraint", "unique", "duplicate");
        }
    }

    @Test
    @DisplayName("Should persist multiple users with same tenant context")
    void shouldPersistMultipleUsersWithDifferentTenants() {
        // Given - both users will get the same tenant_id from TenantContext
        User user1 = User.builder()
            .email("tenant1@example.com")
            .emailHash("sha256_tenant1_hash_" + UUID.randomUUID())
            .passwordHash("password1")
            .fullName("Tenant 1 User")
            .country("US")
            .region("US")
            .build();

        User user2 = User.builder()
            .email("tenant2@example.com")
            .emailHash("sha256_tenant2_hash_" + UUID.randomUUID())
            .passwordHash("password2")
            .fullName("Tenant 2 User")
            .country("DE")
            .region("EU")
            .build();

        // When
        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();

        // Then - both should have the same tenant_id from TenantContext
        assertThat(user1.getTenantId()).isNotNull();
        assertThat(user2.getTenantId()).isNotNull();
        // Both users should belong to the same tenant (from TenantContext)
        assertThat(user1.getTenantId()).isEqualTo(testTenantId);
        assertThat(user2.getTenantId()).isEqualTo(testTenantId);
    }

    @Test
    @DisplayName("Should set default values from builder")
    void shouldSetDefaultValuesFromBuilder() {
        // Given
        User user = User.builder()
            .email("defaults@example.com")
            .emailHash("sha256_defaults_hash_" + UUID.randomUUID())
            .fullName("Defaults User")
            .country("BR")
            .region("LATAM")
            .build();

        // When
        entityManager.persist(user);
        entityManager.flush();

        // Then - defaults should be applied
        assertThat(user.getAuthProvider()).isEqualTo("LOCAL");
        assertThat(user.getEnabled()).isTrue();
    }
}
