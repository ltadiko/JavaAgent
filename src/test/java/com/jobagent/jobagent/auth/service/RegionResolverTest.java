package com.jobagent.jobagent.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 1.5 — Unit tests for RegionResolver.
 */
@DisplayName("RegionResolver Tests")
class RegionResolverTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "DE, EU", "FR, EU", "NL, EU", "BE, EU", "IT, EU", "ES, EU",
            "GB, EU", "CH, EU", "NO, EU", "SE, EU", "PL, EU", "AT, EU"
    })
    @DisplayName("Should resolve EU countries")
    void shouldResolveEuCountries(String country, String expectedRegion) {
        assertThat(RegionResolver.resolveRegion(country)).isEqualTo(expectedRegion);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({"US, US", "CA, US"})
    @DisplayName("Should resolve US region countries")
    void shouldResolveUsCountries(String country, String expectedRegion) {
        assertThat(RegionResolver.resolveRegion(country)).isEqualTo(expectedRegion);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "JP, APAC", "KR, APAC", "CN, APAC", "IN, APAC", "AU, APAC", "SG, APAC"
    })
    @DisplayName("Should resolve APAC countries")
    void shouldResolveApacCountries(String country, String expectedRegion) {
        assertThat(RegionResolver.resolveRegion(country)).isEqualTo(expectedRegion);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({"BR, LATAM", "MX, LATAM", "AR, LATAM"})
    @DisplayName("Should resolve LATAM countries")
    void shouldResolveLatamCountries(String country, String expectedRegion) {
        assertThat(RegionResolver.resolveRegion(country)).isEqualTo(expectedRegion);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({"AE, MENA", "SA, MENA", "TR, MENA"})
    @DisplayName("Should resolve MENA countries")
    void shouldResolveMenaCountries(String country, String expectedRegion) {
        assertThat(RegionResolver.resolveRegion(country)).isEqualTo(expectedRegion);
    }

    @Test
    @DisplayName("Should default to EU for unknown country")
    void shouldDefaultToEuForUnknown() {
        assertThat(RegionResolver.resolveRegion("ZZ")).isEqualTo("EU");
        assertThat(RegionResolver.resolveRegion("XX")).isEqualTo("EU");
    }

    @Test
    @DisplayName("Should be case-insensitive")
    void shouldBeCaseInsensitive() {
        assertThat(RegionResolver.resolveRegion("de")).isEqualTo("EU");
        assertThat(RegionResolver.resolveRegion("us")).isEqualTo("US");
        assertThat(RegionResolver.resolveRegion("jp")).isEqualTo("APAC");
    }

    @Test
    @DisplayName("Should handle null and blank")
    void shouldHandleNullAndBlank() {
        assertThat(RegionResolver.resolveRegion(null)).isEqualTo("EU");
        assertThat(RegionResolver.resolveRegion("")).isEqualTo("EU");
        assertThat(RegionResolver.resolveRegion("  ")).isEqualTo("EU");
    }
}
