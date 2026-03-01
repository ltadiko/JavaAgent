package com.jobagent.jobagent.auth.security;

import com.jobagent.jobagent.auth.model.User;
import com.jobagent.jobagent.auth.service.JpaUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenCustomizer Tests")
class TokenCustomizerTest {

    @Mock
    private JpaUserDetailsService userDetailsService;

    @Mock
    private JwtEncodingContext context;

    @Mock
    private Authentication authentication;

    @Mock
    private JwtClaimsSet.Builder claimsBuilder;

    @InjectMocks
    private TokenCustomizer tokenCustomizer;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .emailHash("hash")
                .fullName("Test User")
                .country("US")
                .region("US")
                .authProvider("LOCAL")
                .enabled(true)
                .build();
        testUser.setId(UUID.randomUUID());
        testUser.setTenantId(UUID.randomUUID());
    }

    @Test
    @DisplayName("customize adds tenant claims to access token")
    void customize_accessToken_addsTenantClaims() {
        when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);
        when(context.getPrincipal()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        when(context.getClaims()).thenReturn(claimsBuilder);
        when(claimsBuilder.claim(anyString(), any())).thenReturn(claimsBuilder);
        when(userDetailsService.findUserByEmail("test@example.com")).thenReturn(testUser);

        tokenCustomizer.customize(context);

        verify(claimsBuilder).claim("user_id", testUser.getId().toString());
        verify(claimsBuilder).claim("tenant_id", testUser.getTenantId().toString());
        verify(claimsBuilder).claim("region", "US");
    }

    @Test
    @DisplayName("customize skips non-access tokens")
    void customize_nonAccessToken_skipsCustomization() {
        OAuth2TokenType refreshTokenType = new OAuth2TokenType("refresh_token");
        when(context.getTokenType()).thenReturn(refreshTokenType);

        tokenCustomizer.customize(context);

        verify(context, never()).getClaims();
        verify(userDetailsService, never()).findUserByEmail(anyString());
    }

    @Test
    @DisplayName("customize handles user not found gracefully")
    void customize_userNotFound_doesNotFail() {
        when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);
        when(context.getPrincipal()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("unknown@example.com");
        when(userDetailsService.findUserByEmail("unknown@example.com"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        tokenCustomizer.customize(context);

        verify(context, never()).getClaims();
    }
}
