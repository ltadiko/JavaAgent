package com.jobagent.jobagent.auth.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 1.2 — Unit test for UserProfile entity.
 */
@DisplayName("UserProfile Entity Tests")
class UserProfileTest {

    @Test
    @DisplayName("Should create empty UserProfile with NoArgsConstructor")
    void shouldCreateEmptyUserProfile() {
        UserProfile profile = new UserProfile();
        assertThat(profile).isNotNull();
        assertThat(profile.getUser()).isNull();
        assertThat(profile.getPhone()).isNull();
    }

    @Test
    @DisplayName("Should build UserProfile with Builder")
    void shouldBuildUserProfile() {
        UserProfile profile = UserProfile.builder()
                .phone("+49123456789")
                .address("Berlin, Germany")
                .linkedinUrl("https://linkedin.com/in/janedoe")
                .preferredJobTitles(new String[]{"Java Developer", "Backend Engineer"})
                .preferredLocations(new String[]{"Berlin", "Amsterdam"})
                .preferredRemote(true)
                .preferredSalaryMin(70000L)
                .preferredCurrency("EUR")
                .build();

        assertThat(profile.getPhone()).isEqualTo("+49123456789");
        assertThat(profile.getAddress()).isEqualTo("Berlin, Germany");
        assertThat(profile.getLinkedinUrl()).isEqualTo("https://linkedin.com/in/janedoe");
        assertThat(profile.getPreferredJobTitles()).containsExactly("Java Developer", "Backend Engineer");
        assertThat(profile.getPreferredLocations()).containsExactly("Berlin", "Amsterdam");
        assertThat(profile.getPreferredRemote()).isTrue();
        assertThat(profile.getPreferredSalaryMin()).isEqualTo(70000L);
        assertThat(profile.getPreferredCurrency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("Should link to User entity")
    void shouldLinkToUser() {
        User user = User.builder()
                .email("test@example.com")
                .emailHash("hash")
                .fullName("Test User")
                .country("DE")
                .region("EU")
                .build();

        UserProfile profile = UserProfile.builder()
                .user(user)
                .build();

        assertThat(profile.getUser()).isNotNull();
        assertThat(profile.getUser().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should set default preferredRemote to false")
    void shouldDefaultPreferredRemoteToFalse() {
        UserProfile profile = UserProfile.builder().build();
        assertThat(profile.getPreferredRemote()).isFalse();
    }

    @Test
    @DisplayName("Should accept null optional fields")
    void shouldAcceptNullOptionalFields() {
        UserProfile profile = UserProfile.builder()
                .phone(null)
                .address(null)
                .linkedinUrl(null)
                .preferredJobTitles(null)
                .preferredLocations(null)
                .preferredSalaryMin(null)
                .preferredCurrency(null)
                .build();

        assertThat(profile.getPhone()).isNull();
        assertThat(profile.getAddress()).isNull();
        assertThat(profile.getLinkedinUrl()).isNull();
        assertThat(profile.getPreferredJobTitles()).isNull();
        assertThat(profile.getPreferredLocations()).isNull();
        assertThat(profile.getPreferredSalaryMin()).isNull();
        assertThat(profile.getPreferredCurrency()).isNull();
    }

    @Test
    @DisplayName("Should extend BaseEntity with tenantId and timestamps")
    void shouldExtendBaseEntity() {
        UUID tenantId = UUID.randomUUID();
        UserProfile profile = new UserProfile();
        profile.setTenantId(tenantId);

        assertThat(profile.getTenantId()).isEqualTo(tenantId);
        assertThat(profile.getId()).isNull();         // Not persisted
        assertThat(profile.getCreatedAt()).isNull();  // Set by @CreationTimestamp
        assertThat(profile.getUpdatedAt()).isNull();  // Set by @UpdateTimestamp
    }

    @Test
    @DisplayName("Should update fields with setters")
    void shouldUpdateFieldsWithSetters() {
        UserProfile profile = new UserProfile();
        profile.setPhone("+1234567890");
        profile.setAddress("New York, USA");
        profile.setLinkedinUrl("https://linkedin.com/in/johndoe");
        profile.setPreferredRemote(true);
        profile.setPreferredSalaryMin(90000L);
        profile.setPreferredCurrency("USD");

        assertThat(profile.getPhone()).isEqualTo("+1234567890");
        assertThat(profile.getAddress()).isEqualTo("New York, USA");
        assertThat(profile.getPreferredRemote()).isTrue();
        assertThat(profile.getPreferredSalaryMin()).isEqualTo(90000L);
        assertThat(profile.getPreferredCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should support different currencies")
    void shouldSupportDifferentCurrencies() {
        String[] currencies = {"EUR", "USD", "GBP", "CHF", "JPY"};
        for (String currency : currencies) {
            UserProfile profile = UserProfile.builder()
                    .preferredCurrency(currency)
                    .build();
            assertThat(profile.getPreferredCurrency()).isEqualTo(currency);
        }
    }

    @Test
    @DisplayName("Should handle empty arrays for preferences")
    void shouldHandleEmptyArrays() {
        UserProfile profile = UserProfile.builder()
                .preferredJobTitles(new String[]{})
                .preferredLocations(new String[]{})
                .build();

        assertThat(profile.getPreferredJobTitles()).isEmpty();
        assertThat(profile.getPreferredLocations()).isEmpty();
    }
}
