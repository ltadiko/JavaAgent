package com.jobagent.jobagent.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

/**
 * Sprint 2.1.2 — Spring Authorization Server configuration.
 *
 * <p>Configures:
 * - JWK source with RSA key pair (dev: generated at startup)
 * - Authorization server endpoints (/oauth2/authorize, /oauth2/token, etc.)
 * - Default SPA client registration (PKCE + authorization_code)
 */
@Configuration
public class AuthorizationServerConfig {

    private static final String SPA_CLIENT_ID = "jobagent-spa";
    private static final String SPA_REDIRECT_URI = "http://localhost:5173/callback";

    /**
     * Authorization Server security filter chain.
     * Handles OAuth2/OIDC endpoints.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        // Apply authorization server defaults
        http
            .securityMatcher("/oauth2/**", "/.well-known/**")
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/oauth2/**", "/.well-known/**").permitAll()
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers("/oauth2/**"))
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                ))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    /**
     * JWK Source with RSA key pair for signing JWTs.
     *
     * <p>Note: For production, integrate with external KMS or load from secure storage.
     * This generates a new key pair at each startup (dev only).
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();

        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * JWT Decoder for resource server validation.
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        try {
            JWKSet jwkSet = ((ImmutableJWKSet<SecurityContext>) jwkSource).getJWKSet();
            RSAKey rsaKey = (RSAKey) jwkSet.getKeys().get(0);
            return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create JWT decoder", e);
        }
    }

    /**
     * Authorization server settings (issuer URL, endpoint paths).
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:8080")
                .build();
    }

    /**
     * Initialize default SPA client if not exists.
     * Uses ApplicationRunner to ensure proper transactional context.
     */
    @Bean
    public org.springframework.boot.ApplicationRunner registeredClientInitializer(
            RegisteredClientRepository registeredClientRepository) {
        return args -> {
            // Check if SPA client already exists
            if (registeredClientRepository.findByClientId(SPA_CLIENT_ID) == null) {
                RegisteredClient spaClient = RegisteredClient.withId(UUID.randomUUID().toString())
                        .clientId(SPA_CLIENT_ID)
                        .clientSecret("{noop}") // Public client (no secret for PKCE SPA)
                        .clientName("JobAgent SPA")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                        .redirectUri(SPA_REDIRECT_URI)
                        .scope("openid")
                        .scope("profile")
                        .scope("email")
                        .clientSettings(ClientSettings.builder()
                                .requireProofKey(true) // PKCE required
                                .requireAuthorizationConsent(false)
                                .build())
                        .tokenSettings(TokenSettings.builder()
                                .accessTokenTimeToLive(Duration.ofMinutes(30))
                                .refreshTokenTimeToLive(Duration.ofHours(8))
                                .reuseRefreshTokens(false)
                                .build())
                        .build();

                registeredClientRepository.save(spaClient);
            }
        };
    }

    /**
     * Generates RSA key pair for JWT signing.
     */
    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key pair", ex);
        }
    }

}
