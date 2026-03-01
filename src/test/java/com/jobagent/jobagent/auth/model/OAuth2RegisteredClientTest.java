package com.jobagent.jobagent.auth.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2RegisteredClientTest {

    @Test
    void builderAndGettersShouldWork() {
        OAuth2RegisteredClient client = OAuth2RegisteredClient.builder()
                .id("1")
                .clientId("jobagent-spa")
                .clientSecret("secret")
                .clientName("JobAgent SPA")
                .clientAuthenticationMethods("pkce")
                .authorizationGrantTypes("authorization_code")
                .redirectUris("http://localhost:5173/callback")
                .scopes("openid profile email")
                .clientSettings("{}")
                .tokenSettings("{}")
                .build();

        assertThat(client).isNotNull();
        assertThat(client.getId()).isEqualTo("1");
        assertThat(client.getClientId()).isEqualTo("jobagent-spa");
        assertThat(client.getClientSecret()).isEqualTo("secret");
        assertThat(client.getRedirectUris()).contains("http://localhost:5173/callback");
    }
}
