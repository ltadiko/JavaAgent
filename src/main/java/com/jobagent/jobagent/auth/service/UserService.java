package com.jobagent.jobagent.auth.service;

import com.jobagent.jobagent.auth.dto.LoginRequest;
import com.jobagent.jobagent.auth.dto.LoginResponse;
import com.jobagent.jobagent.auth.dto.RegisterRequest;
import com.jobagent.jobagent.auth.dto.RegisterResponse;
import com.jobagent.jobagent.auth.model.User;
import com.jobagent.jobagent.auth.model.UserProfile;
import com.jobagent.jobagent.auth.repository.UserProfileRepository;
import com.jobagent.jobagent.auth.repository.UserRepository;
import com.jobagent.jobagent.common.exception.DuplicateResourceException;
import com.jobagent.jobagent.common.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Sprint 1.5 — User registration service.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    /**
     * Authenticate user and return JWT token.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String emailHash = hashEmail(request.email());
        User user = userRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid email or password");
        }

        String token = jwtTokenService.generateAccessToken(
                user.getId(), user.getTenantId(), user.getEmail(), user.getRegion());
        return LoginResponse.bearer(token);
    }

    /**
     * Get user by ID.
     */
    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new com.jobagent.jobagent.common.exception.ResourceNotFoundException("User", userId));
    }

    public RegisterResponse register(RegisterRequest request) {
        // 1. Hash email for lookup
        String emailHash = hashEmail(request.email());

        // 2. Check for duplicate
        if (userRepository.existsByEmailHash(emailHash)) {
            throw new DuplicateResourceException("User already exists with email: " + request.email());
        }

        // 3. Resolve region from country
        String region = RegionResolver.resolveRegion(request.country());

        // 4. Generate tenant ID for new user (registration is unauthenticated)
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        try {
            // 5. Build and save User
            User user = User.builder()
                    .email(request.email().trim().toLowerCase())
                    .emailHash(emailHash)
                    .passwordHash(passwordEncoder.encode(request.password()))
                    .fullName(request.fullName().trim())
                    .country(request.country().toUpperCase().trim())
                    .region(region)
                    .authProvider("LOCAL")
                    .enabled(true)
                    .build();
            user.setTenantId(tenantId);
            userRepository.save(user);

            // 6. Create empty UserProfile linked to user
            UserProfile profile = UserProfile.builder()
                    .user(user)
                    .preferredRemote(false)
                    .build();
            profile.setTenantId(tenantId);
            userProfileRepository.save(profile);

            // 7. Return response
            return new RegisterResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getCountry(),
                    region,
                    user.getCreatedAt()
            );
        } finally {
            // Clear tenant context if it was set for registration
            // (filter will clear it too, but be safe)
            TenantContext.clear();
        }
    }

    /**
     * SHA-256 hash of lowercased, trimmed email.
     */
    static String hashEmail(String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    email.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
