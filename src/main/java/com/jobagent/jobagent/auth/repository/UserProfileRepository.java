package com.jobagent.jobagent.auth.repository;

import com.jobagent.jobagent.auth.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Sprint 1.3 — UserProfile repository.
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUserId(UUID userId);
}
