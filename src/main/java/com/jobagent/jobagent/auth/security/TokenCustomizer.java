package com.jobagent.jobagent.auth.security;

import com.jobagent.jobagent.auth.model.User;
import com.jobagent.jobagent.auth.service.JpaUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

/**
 * Sprint 2.1.3 — JWT Token Customizer for tenant-aware claims.
 *
 * <p>Adds custom claims to access tokens:
 * <ul>
 *   <li>{@code user_id} - UUID of the user</li>
 *   <li>{@code tenant_id} - UUID of the user's tenant</li>
 *   <li>{@code region} - Geographic region (EU, US, APAC, etc.)</li>
 * </ul>
 *
 * <p>These claims are essential for multi-tenant data isolation and RLS.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final JpaUserDetailsService userDetailsService;

    @Override
    public void customize(JwtEncodingContext context) {
        // Only customize access tokens
        if (!"access_token".equals(context.getTokenType().getValue())) {
            return;
        }

        String username = context.getPrincipal().getName();

        try {
            User user = userDetailsService.findUserByEmail(username);

            // Add tenant-aware claims
            context.getClaims().claim("user_id", user.getId().toString());
            context.getClaims().claim("tenant_id", user.getTenantId().toString());
            context.getClaims().claim("region", user.getRegion());

            log.debug("Added tenant claims to token for user: {}, tenant: {}, region: {}",
                    user.getId(), user.getTenantId(), user.getRegion());

        } catch (UsernameNotFoundException e) {
            log.warn("Could not find user for token customization: {}", username);
            // Don't fail token generation, just skip custom claims
        }
    }
}
