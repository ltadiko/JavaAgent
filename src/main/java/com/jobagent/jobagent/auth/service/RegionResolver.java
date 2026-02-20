package com.jobagent.jobagent.auth.service;

import java.util.Map;

/**
 * Sprint 1.5 — Maps ISO 3166-1 alpha-2 country codes to data regions.
 */
public final class RegionResolver {

    private RegionResolver() {} // Utility class

    private static final Map<String, String> COUNTRY_TO_REGION = Map.ofEntries(
            // ── EU ──
            Map.entry("DE", "EU"), Map.entry("FR", "EU"), Map.entry("NL", "EU"),
            Map.entry("BE", "EU"), Map.entry("IT", "EU"), Map.entry("ES", "EU"),
            Map.entry("PT", "EU"), Map.entry("AT", "EU"), Map.entry("IE", "EU"),
            Map.entry("FI", "EU"), Map.entry("SE", "EU"), Map.entry("DK", "EU"),
            Map.entry("PL", "EU"), Map.entry("CZ", "EU"), Map.entry("RO", "EU"),
            Map.entry("GR", "EU"), Map.entry("HU", "EU"), Map.entry("BG", "EU"),
            Map.entry("HR", "EU"), Map.entry("SK", "EU"), Map.entry("SI", "EU"),
            Map.entry("LT", "EU"), Map.entry("LV", "EU"), Map.entry("EE", "EU"),
            Map.entry("CY", "EU"), Map.entry("MT", "EU"), Map.entry("LU", "EU"),
            Map.entry("CH", "EU"), Map.entry("NO", "EU"), Map.entry("GB", "EU"),
            // ── US ──
            Map.entry("US", "US"), Map.entry("CA", "US"),
            // ── APAC ──
            Map.entry("JP", "APAC"), Map.entry("KR", "APAC"), Map.entry("CN", "APAC"),
            Map.entry("IN", "APAC"), Map.entry("AU", "APAC"), Map.entry("NZ", "APAC"),
            Map.entry("SG", "APAC"), Map.entry("MY", "APAC"), Map.entry("TH", "APAC"),
            Map.entry("PH", "APAC"), Map.entry("ID", "APAC"), Map.entry("VN", "APAC"),
            Map.entry("TW", "APAC"), Map.entry("HK", "APAC"),
            // ── LATAM ──
            Map.entry("BR", "LATAM"), Map.entry("MX", "LATAM"), Map.entry("AR", "LATAM"),
            Map.entry("CO", "LATAM"), Map.entry("CL", "LATAM"), Map.entry("PE", "LATAM"),
            // ── MENA ──
            Map.entry("AE", "MENA"), Map.entry("SA", "MENA"), Map.entry("EG", "MENA"),
            Map.entry("IL", "MENA"), Map.entry("TR", "MENA"), Map.entry("QA", "MENA"),
            Map.entry("KW", "MENA"), Map.entry("BH", "MENA"), Map.entry("OM", "MENA")
    );

    /**
     * Resolve a country code to a data region.
     * Defaults to "EU" for unknown countries.
     */
    public static String resolveRegion(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return "EU";
        }
        return COUNTRY_TO_REGION.getOrDefault(countryCode.toUpperCase().trim(), "EU");
    }
}
