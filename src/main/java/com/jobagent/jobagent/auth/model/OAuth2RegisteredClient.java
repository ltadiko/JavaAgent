package com.jobagent.jobagent.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity representing a persisted OAuth2 registered client.
 * Matches the typical Spring Authorization Server schema (V2 migration).
 * This is intentionally minimal and stores some collections as JSON/text for now.
 */
@Entity
@Table(name = "oauth2_registered_client")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuth2RegisteredClient {

    @Id
    @Column(name = "id", nullable = false, length = 100)
    private String id;

    @Column(name = "client_id", nullable = false, unique = true, length = 100)
    private String clientId;

    @Column(name = "client_secret", length = 100)
    private String clientSecret;

    @Column(name = "client_name", length = 200)
    private String clientName;

    // We store these collection-like fields as JSON/text for now; conversion utilities will be added later
    @Lob
    @Column(name = "client_authentication_methods")
    private String clientAuthenticationMethods;

    @Lob
    @Column(name = "authorization_grant_types")
    private String authorizationGrantTypes;

    @Lob
    @Column(name = "redirect_uris")
    private String redirectUris;

    @Lob
    @Column(name = "scopes")
    private String scopes;

    @Lob
    @Column(name = "client_settings")
    private String clientSettings;

    @Lob
    @Column(name = "token_settings")
    private String tokenSettings;

}
