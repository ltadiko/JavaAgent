package com.jobagent.jobagent.common.security;

import com.jobagent.jobagent.common.multitenancy.TenantContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security configuration — Sprint 2.1: JWT Resource Server enabled.
 * Sprint 10.5: Added Swagger UI public access.
 * Sprint 11: TenantContextFilter registered after JWT authentication.
 *
 * <p>Public endpoints:
 * <ul>
 *   <li>POST /api/v1/auth/register - User registration</li>
 *   <li>GET /actuator/** - Health/metrics</li>
 *   <li>GET / - Welcome page</li>
 *   <li>/oauth2/**, /.well-known/** - Auth server</li>
 *   <li>/swagger-ui/**, /v3/api-docs/** - API documentation</li>
 * </ul>
 *
 * <p>Protected endpoints (require valid JWT):
 * <ul>
 *   <li>All other /api/** endpoints</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            TenantContextFilter tenantContextFilter) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable()) // Stateless API — CSRF not needed
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/oauth2/**", "/.well-known/**").permitAll()
                .requestMatchers("/api/v1/auth/register").permitAll()
                .requestMatchers("/api/v1/auth/login").permitAll()
                // Swagger / OpenAPI
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                // All other API endpoints require authentication
                .requestMatchers("/api/**").authenticated()
                // Default: permit (for static resources, error pages, etc.)
                .anyRequest().permitAll()
            )
            // Enable JWT resource server
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            // Add TenantContextFilter AFTER JWT authentication
            .addFilterAfter(tenantContextFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
