package com.jobagent.jobagent.auth.repository;

import com.jobagent.jobagent.auth.model.User;
import com.jobagent.jobagent.auth.model.UserProfile;
import com.jobagent.jobagent.common.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 1.3 — Integration tests for UserProfileRepository.
 * Requires Docker PostgreSQL: docker compose up -d postgres
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
@DisplayName("UserProfileRepository Integration Tests")
class UserProfileRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    private User savedUser;
    private UUID testTenantId;

    @BeforeEach
    void setUp() {
        // Set up tenant context for tests - TenantEntityListener requires this
        testTenantId = UUID.randomUUID();
        TenantContext.setTenantId(testTenantId);

        User user = User.builder()
                .email("profile.repo@example.com")
                .emailHash("sha256_profile_repo_hash_" + UUID.randomUUID())
                .passwordHash("bcrypt_password")
                .fullName("Profile Repo User")
                .country("NL")
                .region("EU")
                .build();
        savedUser = userRepository.saveAndFlush(user);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("save() should persist UserProfile linked to User")
    void saveShouldPersistProfile() {
        UserProfile profile = UserProfile.builder()
                .user(savedUser)
                .phone("+31612345678")
                .linkedinUrl("https://linkedin.com/in/test")
                .preferredRemote(true)
                .preferredSalaryMin(60000L)
                .preferredCurrency("EUR")
                .build();
        profile.setTenantId(savedUser.getTenantId());

        UserProfile saved = userProfileRepository.saveAndFlush(profile);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getId()).isEqualTo(savedUser.getId());
        assertThat(saved.getPhone()).isEqualTo("+31612345678");
    }

    @Test
    @DisplayName("findByUserId() should return profile for existing user")
    void findByUserIdShouldReturnProfile() {
        UserProfile profile = UserProfile.builder()
                .user(savedUser)
                .preferredRemote(false)
                .build();
        profile.setTenantId(savedUser.getTenantId());
        userProfileRepository.saveAndFlush(profile);

        Optional<UserProfile> found = userProfileRepository.findByUserId(savedUser.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    @DisplayName("findByUserId() should return empty for non-existent user")
    void findByUserIdShouldReturnEmptyForUnknown() {
        Optional<UserProfile> found = userProfileRepository.findByUserId(UUID.randomUUID());

        assertThat(found).isEmpty();
    }
}
