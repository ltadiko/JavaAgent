package com.jobagent.jobagent.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security configuration — Sprint 0: all endpoints open.
 *
 * TODO: Sprint 1 — Re-enable JWT authentication:
 *   1. Add Authorization Server filter chain (@Order(1))
 *   2. Configure oauth2ResourceServer(oauth2 -> oauth2.jwt(...)) with issuer-uri
 *   3. Change .anyRequest().permitAll() → .anyRequest().authenticated()
 *   4. Add JWT token customizer (tenant_id, region claims)
 *   5. Configure spring.security.oauth2.resourceserver.jwt.issuer-uri in YAML
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable()) // Stateless API — CSRF not needed
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // TODO: Sprint 1 — restrict to authenticated after JWT is configured
                .anyRequest().permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
