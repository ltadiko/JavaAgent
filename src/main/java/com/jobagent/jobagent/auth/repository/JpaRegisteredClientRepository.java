package com.jobagent.jobagent.auth.repository;

import com.jobagent.jobagent.auth.model.OAuth2RegisteredClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@Transactional
public class JpaRegisteredClientRepository implements RegisteredClientRepository {

    private final OAuth2RegisteredClientJpaRepository repo;

    @Autowired
    public JpaRegisteredClientRepository(OAuth2RegisteredClientJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        String id = registeredClient.getId() == null ? UUID.randomUUID().toString() : registeredClient.getId();
        OAuth2RegisteredClient entity = OAuth2RegisteredClient.builder()
                .id(id)
                .clientId(registeredClient.getClientId())
                .clientSecret(registeredClient.getClientSecret())
                .clientName(registeredClient.getClientName())
                .clientAuthenticationMethods(String.join(",", registeredClient.getClientAuthenticationMethods().stream().map(Object::toString).toList()))
                .authorizationGrantTypes(String.join(",", registeredClient.getAuthorizationGrantTypes().stream().map(AuthorizationGrantType::getValue).toList()))
                .redirectUris(String.join(",", registeredClient.getRedirectUris()))
                .scopes(String.join(",", registeredClient.getScopes()))
                .clientSettings(registeredClient.getClientSettings() != null ? registeredClient.getClientSettings().toString() : null)
                .tokenSettings(registeredClient.getTokenSettings() != null ? registeredClient.getTokenSettings().toString() : null)
                .build();
        repo.save(entity);
    }

    @Override
    public RegisteredClient findById(String id) {
        Optional<OAuth2RegisteredClient> opt = repo.findById(id);
        return opt.map(this::toRegisteredClient).orElse(null);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        Optional<OAuth2RegisteredClient> opt = repo.findByClientId(clientId);
        return opt.map(this::toRegisteredClient).orElse(null);
    }

    private RegisteredClient toRegisteredClient(OAuth2RegisteredClient e) {
        RegisteredClient.Builder b = RegisteredClient.withId(e.getId())
                .clientId(e.getClientId())
                .clientSecret(e.getClientSecret())
                .clientName(e.getClientName());

        if (e.getAuthorizationGrantTypes() != null && e.getAuthorizationGrantTypes().contains("authorization_code")) {
            b.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
        }
        if (e.getRedirectUris() != null && !e.getRedirectUris().isBlank()) {
            for (String uri : e.getRedirectUris().split(",")) {
                b.redirectUri(uri.trim());
            }
        }
        if (e.getScopes() != null && !e.getScopes().isBlank()) {
            for (String s : e.getScopes().split(",")) {
                b.scope(s.trim());
            }
        }

        return b.build();
    }
}
