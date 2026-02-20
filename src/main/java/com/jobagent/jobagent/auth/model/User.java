package com.jobagent.jobagent.auth.model;

import com.jobagent.jobagent.common.model.BaseEntity;
import com.jobagent.jobagent.common.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import lombok.*;

/**
 * User entity — maps to users table.
 * Sprint 1.1: Simplified version with plaintext email.
 * Sprint 10.4: Will add email encryption for GDPR compliance.
 */
@Entity
@Table(name = "users")
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(nullable = false)
    private String email;  // Plaintext (encrypted in Sprint 10.4)

    @Column(name = "email_hash", nullable = false, unique = true)
    private String emailHash;  // SHA-256 for fast lookups

    @Column(name = "password_hash")
    private String passwordHash;  // BCrypt (nullable for social login)

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, length = 2)
    private String country;  // ISO 3166-1 alpha-2

    @Column(nullable = false, length = 10)
    private String region;  // EU, US, APAC

    @Column(name = "auth_provider", nullable = false, length = 20)
    @Builder.Default
    private String authProvider = "LOCAL";  // LOCAL, GOOGLE, LINKEDIN

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;
}
