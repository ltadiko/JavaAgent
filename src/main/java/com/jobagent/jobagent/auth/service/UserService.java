package com.jobagent.jobagent.auth.service;

import com.jobagent.jobagent.auth.dto.RegisterRequest;
import com.jobagent.jobagent.auth.dto.RegisterResponse;
import com.jobagent.jobagent.auth.model.User;
import com.jobagent.jobagent.auth.model.UserProfile;
import com.jobagent.jobagent.auth.repository.UserProfileRepository;
import com.jobagent.jobagent.auth.repository.UserRepository;
import com.jobagent.jobagent.common.exception.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

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

    public RegisterResponse register(RegisterRequest request) {
        // 1. Hash email for lookup
        String emailHash = hashEmail(request.email());

        // 2. Check for duplicate
        if (userRepository.existsByEmailHash(emailHash)) {
            throw new DuplicateResourceException("User already exists with email: " + request.email());
        }

        // 3. Resolve region from country
        String region = RegionResolver.resolveRegion(request.country());

        // 4. Build and save User
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
        userRepository.save(user);

        // 5. Create empty UserProfile linked to user
        UserProfile profile = UserProfile.builder()
                .user(user)
                .preferredRemote(false)
                .build();
        profile.setTenantId(user.getTenantId());
        userProfileRepository.save(profile);

        // 6. Return response
        return new RegisterResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getCountry(),
                region,
                user.getCreatedAt()
        );
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
