package com.jobagent.jobagent.auth.model;

import com.jobagent.jobagent.common.model.BaseEntity;
import com.jobagent.jobagent.common.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import lombok.*;

/**
 * UserProfile entity — maps to user_profiles table.
 * Sprint 1.2: Stores job preferences and contact info.
 * Sprint 10.4: Will add encryption on phone and address fields.
 */
@Entity
@Table(name = "user_profiles")
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "phone_encrypted")
    private String phone;  // Plaintext (encrypted in Sprint 10.4)

    @Column(name = "address_encrypted")
    private String address;  // Plaintext (encrypted in Sprint 10.4)

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "preferred_job_titles", columnDefinition = "TEXT[]")
    private String[] preferredJobTitles;

    @Column(name = "preferred_locations", columnDefinition = "TEXT[]")
    private String[] preferredLocations;

    @Column(name = "preferred_remote")
    @Builder.Default
    private Boolean preferredRemote = false;

    @Column(name = "preferred_salary_min")
    private Long preferredSalaryMin;

    @Column(name = "preferred_currency", length = 3)
    private String preferredCurrency;  // EUR, USD, GBP
}
