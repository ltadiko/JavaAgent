package com.jobagent.jobagent.auth.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Generates JWT access tokens for authenticated users.
 * Uses the same RSA key pair as the Authorization Server.
 */
@Service
@Slf4j
public class JwtTokenService {

    private final JWSSigner signer;
    private final String keyId;

    public JwtTokenService(JWKSource<SecurityContext> jwkSource) {
        try {
            JWKSet jwkSet = ((ImmutableJWKSet<SecurityContext>) jwkSource).getJWKSet();
            RSAKey rsaKey = (RSAKey) jwkSet.getKeys().get(0);
            this.signer = new RSASSASigner(rsaKey);
            this.keyId = rsaKey.getKeyID();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize JWT signer", e);
        }
    }

    /**
     * Generate a signed JWT access token for the given user.
     */
    public String generateAccessToken(UUID userId, UUID tenantId, String email, String region) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer("http://localhost:8080")
                    .subject(userId.toString())
                    .claim("tenant_id", tenantId.toString())
                    .claim("email", email)
                    .claim("region", region)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(1800))) // 30 minutes
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyId).build(),
                    claims
            );
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }
}
