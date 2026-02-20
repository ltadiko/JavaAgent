package com.jobagent.jobagent.auth.repository;

import com.jobagent.jobagent.auth.model.User;
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
 * Sprint 1.3 — Integration tests for UserRepository.
 * Requires Docker PostgreSQL: docker compose up -d postgres
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
@DisplayName("UserRepository Integration Tests")
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;


    private User savedUser;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("repo.test@example.com")
                .emailHash("sha256_repo_test_hash_" + UUID.randomUUID())
                .passwordHash("bcrypt_password")
                .fullName("Repo Test User")
                .country("DE")
                .region("EU")
                .build();
        savedUser = userRepository.saveAndFlush(user);
    }

    @Test
    @DisplayName("save() should generate UUID id")
    void saveShouldGenerateId() {
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByEmailHash() should return user for existing hash")
    void findByEmailHashShouldReturnUser() {
        Optional<User> found = userRepository.findByEmailHash(savedUser.getEmailHash());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("repo.test@example.com");
        assertThat(found.get().getFullName()).isEqualTo("Repo Test User");
    }

    @Test
    @DisplayName("findByEmailHash() should return empty for unknown hash")
    void findByEmailHashShouldReturnEmptyForUnknown() {
        Optional<User> found = userRepository.findByEmailHash("nonexistent_hash");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByEmailHash() should return true for existing hash")
    void existsByEmailHashShouldReturnTrue() {
        boolean exists = userRepository.existsByEmailHash(savedUser.getEmailHash());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByEmailHash() should return false for unknown hash")
    void existsByEmailHashShouldReturnFalse() {
        boolean exists = userRepository.existsByEmailHash("nonexistent_hash");

        assertThat(exists).isFalse();
    }
}
