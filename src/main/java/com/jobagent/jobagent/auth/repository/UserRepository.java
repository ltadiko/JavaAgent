package com.jobagent.jobagent.auth.repository;

import com.jobagent.jobagent.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Sprint 1.3 — User repository with email hash lookup.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailHash(String emailHash);

    boolean existsByEmailHash(String emailHash);
}
