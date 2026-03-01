package com.jobagent.jobagent.auth.service;

import com.jobagent.jobagent.auth.model.User;
import com.jobagent.jobagent.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Sprint 2.1.1 — Unit tests for JpaUserDetailsService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaUserDetailsService Tests")
class JpaUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JpaUserDetailsService userDetailsService;

    private User testUser;
    private String testEmail;
    private String testEmailHash;

    @BeforeEach
    void setUp() throws Exception {
        testEmail = "test@example.com";
        testEmailHash = hashEmail(testEmail);

        testUser = User.builder()
                .email(testEmail)
                .emailHash(testEmailHash)
                .passwordHash("$2a$10$hashedpassword")
                .fullName("Test User")
                .country("US")
                .region("US")
                .authProvider("LOCAL")
                .enabled(true)
                .build();
        // Set the ID using reflection or setter if available
        testUser.setId(UUID.randomUUID());
        testUser.setTenantId(UUID.randomUUID());
    }

    @Test
    @DisplayName("loadUserByUsername returns UserDetails for existing user")
    void loadUserByUsername_existingUser_returnsUserDetails() {
        // Given
        when(userRepository.findByEmailHash(testEmailHash)).thenReturn(Optional.of(testUser));

        // When
        UserDetails result = userDetailsService.loadUserByUsername(testEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(testEmail);
        assertThat(result.getPassword()).isEqualTo("$2a$10$hashedpassword");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.isAccountNonExpired()).isTrue();
        assertThat(result.isAccountNonLocked()).isTrue();
        assertThat(result.isCredentialsNonExpired()).isTrue();
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("loadUserByUsername throws exception for unknown user")
    void loadUserByUsername_unknownUser_throwsException() {
        // Given
        when(userRepository.findByEmailHash(anyString())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("loadUserByUsername returns disabled UserDetails for disabled user")
    void loadUserByUsername_disabledUser_returnsDisabledUserDetails() {
        // Given
        testUser.setEnabled(false);
        when(userRepository.findByEmailHash(testEmailHash)).thenReturn(Optional.of(testUser));

        // When
        UserDetails result = userDetailsService.loadUserByUsername(testEmail);

        // Then
        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("loadUserByUsername handles null password for social login")
    void loadUserByUsername_socialLoginUser_handlesNullPassword() {
        // Given
        testUser.setPasswordHash(null);
        testUser.setAuthProvider("GOOGLE");
        when(userRepository.findByEmailHash(testEmailHash)).thenReturn(Optional.of(testUser));

        // When
        UserDetails result = userDetailsService.loadUserByUsername(testEmail);

        // Then
        assertThat(result.getPassword()).isEmpty();
    }

    @Test
    @DisplayName("loadUserByUsername handles case-insensitive email")
    void loadUserByUsername_caseInsensitiveEmail_findsUser() {
        // Given - hash should be same for uppercase/lowercase email
        String uppercaseEmail = "TEST@EXAMPLE.COM";
        when(userRepository.findByEmailHash(testEmailHash)).thenReturn(Optional.of(testUser));

        // When
        UserDetails result = userDetailsService.loadUserByUsername(uppercaseEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(testEmail);
    }

    @Test
    @DisplayName("findUserByEmail returns User entity for existing user")
    void findUserByEmail_existingUser_returnsUser() {
        // Given
        when(userRepository.findByEmailHash(testEmailHash)).thenReturn(Optional.of(testUser));

        // When
        User result = userDetailsService.findUserByEmail(testEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(testEmail);
        assertThat(result.getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("findUserByEmail throws exception for unknown user")
    void findUserByEmail_unknownUser_throwsException() {
        // Given
        when(userRepository.findByEmailHash(anyString())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userDetailsService.findUserByEmail("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    /**
     * Helper to generate SHA-256 hash of email for test setup.
     */
    private static String hashEmail(String email) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(email.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
