package com.jobagent.jobagent.auth.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 1.1 — Unit test for User entity.
 * Tests entity creation, builder pattern, and field assignments.
 */
@DisplayName("User Entity Tests")
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    @DisplayName("Should create empty User with NoArgsConstructor")
    void shouldCreateEmptyUser() {
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getFullName()).isNull();
    }

    @Test
    @DisplayName("Should create User with AllArgsConstructor")
    void shouldCreateUserWithAllArgs() {
        UUID tenantId = UUID.randomUUID();
        User user = new User(
            "john.doe@example.com",
            "hash_of_email",
            "bcrypt_password_hash",
            "John Doe",
            "US",
            "US",
            "LOCAL",
            true
        );

        assertThat(user.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(user.getEmailHash()).isEqualTo("hash_of_email");
        assertThat(user.getPasswordHash()).isEqualTo("bcrypt_password_hash");
        assertThat(user.getFullName()).isEqualTo("John Doe");
        assertThat(user.getCountry()).isEqualTo("US");
        assertThat(user.getRegion()).isEqualTo("US");
        assertThat(user.getAuthProvider()).isEqualTo("LOCAL");
        assertThat(user.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should build User with Builder pattern")
    void shouldBuildUserWithBuilder() {
        UUID tenantId = UUID.randomUUID();

        User user = User.builder()
            .email("jane.smith@example.com")
            .emailHash("sha256_hash_of_jane_email")
            .passwordHash("bcrypt_jane_password")
            .fullName("Jane Smith")
            .country("DE")
            .region("EU")
            .authProvider("LOCAL")
            .enabled(true)
            .build();

        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo("jane.smith@example.com");
        assertThat(user.getEmailHash()).isEqualTo("sha256_hash_of_jane_email");
        assertThat(user.getPasswordHash()).isEqualTo("bcrypt_jane_password");
        assertThat(user.getFullName()).isEqualTo("Jane Smith");
        assertThat(user.getCountry()).isEqualTo("DE");
        assertThat(user.getRegion()).isEqualTo("EU");
        assertThat(user.getAuthProvider()).isEqualTo("LOCAL");
        assertThat(user.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should set default authProvider to LOCAL")
    void shouldSetDefaultAuthProvider() {
        User user = User.builder()
            .email("test@example.com")
            .emailHash("hash")
            .fullName("Test User")
            .country("US")
            .region("US")
            .build();

        assertThat(user.getAuthProvider()).isEqualTo("LOCAL");
    }

    @Test
    @DisplayName("Should set default enabled to true")
    void shouldSetDefaultEnabled() {
        User user = User.builder()
            .email("test@example.com")
            .emailHash("hash")
            .fullName("Test User")
            .country("US")
            .region("US")
            .build();

        assertThat(user.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should allow null passwordHash for social login")
    void shouldAllowNullPasswordHashForSocialLogin() {
        User user = User.builder()
            .email("social@example.com")
            .emailHash("hash")
            .passwordHash(null)  // OAuth login
            .fullName("Social User")
            .country("FR")
            .region("EU")
            .authProvider("GOOGLE")
            .build();

        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getAuthProvider()).isEqualTo("GOOGLE");
    }

    @Test
    @DisplayName("Should support different auth providers")
    void shouldSupportDifferentAuthProviders() {
        String[] providers = {"LOCAL", "GOOGLE", "LINKEDIN"};

        for (String provider : providers) {
            User user = User.builder()
                .email("user@example.com")
                .emailHash("hash")
                .fullName("Test User")
                .country("US")
                .region("US")
                .authProvider(provider)
                .build();

            assertThat(user.getAuthProvider()).isEqualTo(provider);
        }
    }

    @Test
    @DisplayName("Should support different regions")
    void shouldSupportDifferentRegions() {
        String[] regions = {"EU", "US", "APAC", "LATAM", "MENA"};

        for (String region : regions) {
            User user = User.builder()
                .email("user@example.com")
                .emailHash("hash")
                .fullName("Test User")
                .country("XX")
                .region(region)
                .build();

            assertThat(user.getRegion()).isEqualTo(region);
        }
    }

    @Test
    @DisplayName("Should use ISO country codes")
    void shouldUseISOCountryCodes() {
        String[][] countryRegions = {
            {"US", "US"},
            {"DE", "EU"},
            {"FR", "EU"},
            {"JP", "APAC"},
            {"BR", "LATAM"}
        };

        for (String[] pair : countryRegions) {
            User user = User.builder()
                .email("user@example.com")
                .emailHash("hash")
                .fullName("Test User")
                .country(pair[0])
                .region(pair[1])
                .build();

            assertThat(user.getCountry()).isEqualTo(pair[0]);
            assertThat(user.getRegion()).isEqualTo(pair[1]);
        }
    }

    @Test
    @DisplayName("Should update fields with setters")
    void shouldUpdateFieldsWithSetters() {
        user.setEmail("updated@example.com");
        user.setEmailHash("new_hash");
        user.setFullName("Updated Name");
        user.setEnabled(false);

        assertThat(user.getEmail()).isEqualTo("updated@example.com");
        assertThat(user.getEmailHash()).isEqualTo("new_hash");
        assertThat(user.getFullName()).isEqualTo("Updated Name");
        assertThat(user.getEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should extend BaseEntity")
    void shouldExtendBaseEntity() {
        UUID tenantId = UUID.randomUUID();
        user.setTenantId(tenantId);

        assertThat(user.getTenantId()).isEqualTo(tenantId);
        assertThat(user.getId()).isNull();  // Not persisted yet
        assertThat(user.getCreatedAt()).isNull();  // Set by @CreationTimestamp
        assertThat(user.getUpdatedAt()).isNull();  // Set by @UpdateTimestamp
    }
}
