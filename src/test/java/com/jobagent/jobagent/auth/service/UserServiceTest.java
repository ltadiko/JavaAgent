package com.jobagent.jobagent.auth.service;

import com.jobagent.jobagent.auth.dto.RegisterRequest;
import com.jobagent.jobagent.auth.dto.RegisterResponse;
import com.jobagent.jobagent.auth.model.User;
import com.jobagent.jobagent.auth.model.UserProfile;
import com.jobagent.jobagent.auth.repository.UserProfileRepository;
import com.jobagent.jobagent.auth.repository.UserRepository;
import com.jobagent.jobagent.common.exception.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Sprint 1.5 — Unit tests for UserService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest(
                "test@example.com", "password123", "Test User", "DE"
        );
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUser() {
        when(userRepository.existsByEmailHash(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            user.setTenantId(UUID.randomUUID());
            return user;
        });
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse response = userService.register(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isNotNull();
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.fullName()).isEqualTo("Test User");
        assertThat(response.country()).isEqualTo("DE");
        assertThat(response.region()).isEqualTo("EU");
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException for existing email")
    void shouldThrowForDuplicateEmail() {
        when(userRepository.existsByEmailHash(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.register(validRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("test@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should encode password with PasswordEncoder")
    void shouldEncodePassword() {
        when(userRepository.existsByEmailHash(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            user.setTenantId(UUID.randomUUID());
            return user;
        });
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.register(validRequest);

        verify(passwordEncoder).encode("password123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("$2a$encoded");
    }

    @Test
    @DisplayName("Should hash email as SHA-256 of lowercase trimmed input")
    void shouldHashEmail() {
        String hash1 = UserService.hashEmail("Test@Example.COM");
        String hash2 = UserService.hashEmail("test@example.com");
        String hash3 = UserService.hashEmail("  test@example.com  ");

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash2).isEqualTo(hash3);
        assertThat(hash1).hasSize(64); // SHA-256 hex = 64 chars
    }

    @Test
    @DisplayName("Should create UserProfile linked to User")
    void shouldCreateUserProfile() {
        when(userRepository.existsByEmailHash(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$encoded");
        UUID tenantId = UUID.randomUUID();
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            user.setTenantId(tenantId);
            return user;
        });
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.register(validRequest);

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(profileCaptor.capture());
        UserProfile savedProfile = profileCaptor.getValue();
        assertThat(savedProfile.getUser()).isNotNull();
        assertThat(savedProfile.getTenantId()).isEqualTo(tenantId);
        assertThat(savedProfile.getPreferredRemote()).isFalse();
    }

    @Test
    @DisplayName("Should resolve region correctly for different countries")
    void shouldResolveRegion() {
        when(userRepository.existsByEmailHash(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            user.setTenantId(UUID.randomUUID());
            return user;
        });
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // US
        RegisterRequest usReq = new RegisterRequest("us@test.com", "password123", "US User", "US");
        RegisterResponse usResp = userService.register(usReq);
        assertThat(usResp.region()).isEqualTo("US");

        // reset for next call
        reset(userRepository);
        when(userRepository.existsByEmailHash(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            user.setTenantId(UUID.randomUUID());
            return user;
        });

        // JP → APAC
        RegisterRequest jpReq = new RegisterRequest("jp@test.com", "password123", "JP User", "JP");
        RegisterResponse jpResp = userService.register(jpReq);
        assertThat(jpResp.region()).isEqualTo("APAC");
    }

    @Test
    @DisplayName("Should store email as lowercase and country as uppercase")
    void shouldNormalizeFields() {
        when(userRepository.existsByEmailHash(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            user.setTenantId(UUID.randomUUID());
            return user;
        });
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequest req = new RegisterRequest("Test@EXAMPLE.com", "password123", "  Test User  ", "de");
        userService.register(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("test@example.com");
        assertThat(captor.getValue().getCountry()).isEqualTo("DE");
        assertThat(captor.getValue().getFullName()).isEqualTo("Test User");
    }
}
