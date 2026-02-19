# 01 — Use Case: Register / Login

## 1. Summary

Allow users to create an account and authenticate.  
The system supports email/password registration and OAuth 2.1 social login (Google, LinkedIn).  
On registration the user selects their **country**, which determines the **data region** for all their data.

---

## 2. Actors

| Actor       | Description                           |
|-------------|---------------------------------------|
| **User**    | A job seeker using the platform.      |
| **System**  | JobAgent backend + Auth server.       |

---

## 3. Preconditions

- None (public endpoint).

## 4. Postconditions

- User record exists in the regional PostgreSQL database.
- A JWT access token + refresh token pair is issued.
- A `user_profiles` row is created with country and empty profile fields.

---

## 5. Flow

### 5.1 Registration (Email / Password)

```
User                        Frontend                    Backend (Auth Module)           PostgreSQL
 │  Fill registration form    │                              │                              │
 │ ──────────────────────────►│  POST /api/v1/auth/register  │                              │
 │                            │ ────────────────────────────►│                              │
 │                            │                              │  Validate input              │
 │                            │                              │  Hash password (bcrypt)      │
 │                            │                              │  Determine region from country│
 │                            │                              │  INSERT users, user_profiles  │
 │                            │                              │ ────────────────────────────►│
 │                            │                              │◄─────── OK ─────────────────│
 │                            │                              │  Generate JWT (access+refresh)│
 │                            │◄──── 201 { tokens, user } ──│                              │
 │◄─── Show dashboard ───────│                              │                              │
```

### 5.2 Login (Email / Password)

```
User                        Frontend                    Backend (Auth Module)           PostgreSQL
 │  Enter credentials         │                              │                              │
 │ ──────────────────────────►│  POST /api/v1/auth/login     │                              │
 │                            │ ────────────────────────────►│                              │
 │                            │                              │  Lookup user by email         │
 │                            │                              │ ────────────────────────────►│
 │                            │                              │◄──── user row ──────────────│
 │                            │                              │  Verify bcrypt hash           │
 │                            │                              │  Generate JWT (access+refresh)│
 │                            │◄──── 200 { tokens, user } ──│                              │
 │◄─── Show dashboard ───────│                              │                              │
```

### 5.3 Social Login (OAuth 2.1 — Google / LinkedIn)

The embedded **Spring Authorization Server** acts as the OpenID Connect provider for the platform.  
It supports **federated identity** via Google and LinkedIn using Spring Security's OAuth2 Client.

```
User          Frontend          Spring Auth Server          Google / LinkedIn         PostgreSQL
 │              │                     │                            │                      │
 │ "Login with  │                     │                            │                      │
 │  Google"     │                     │                            │                      │
 │─────────────►│ GET /oauth2/        │                            │                      │
 │              │  authorization/     │                            │                      │
 │              │  google             │                            │                      │
 │              │────────────────────►│                            │                      │
 │              │                     │  302 → Google OAuth consent│                      │
 │              │◄────────────────────│                            │                      │
 │              │──── redirect ───────────────────────────────────►│                      │
 │              │                     │                            │                      │
 │ Consent      │◄──── callback + auth code ──────────────────────│                      │
 │              │─────────────────────────────────────────────────►│                      │
 │              │                     │◄── code exchange ─────────│                      │
 │              │                     │    (id_token + userinfo)   │                      │
 │              │                     │                            │                      │
 │              │                     │  Lookup / create user      │                      │
 │              │                     │───────────────────────────────────────────────────►│
 │              │                     │◄──── user row ────────────────────────────────────│
 │              │                     │  Issue JWT (access+refresh)│                      │
 │              │◄── 302 + tokens ────│                            │                      │
 │◄── Dashboard │                     │                            │                      │
```

1. Frontend redirects to the **Spring Authorization Server** `/oauth2/authorization/{provider}`.
2. The Auth Server (acting as an OAuth2 Client to Google/LinkedIn) handles redirect → provider → callback.
3. On first login the system auto-creates a `users` + `user_profiles` record; region is inferred from the user's locale or they are prompted to select a country.
4. The Auth Server issues its own JWT (access + refresh) — the platform never exposes third-party tokens.

---

## 6. API Endpoints

### 6.1 Application Endpoints

| Method | Path                              | Auth   | Description                        |
|--------|-----------------------------------|--------|------------------------------------|
| POST   | `/api/v1/auth/register`           | Public | Register with email + password     |
| POST   | `/api/v1/auth/login`              | Public | Login with email + password        |
| POST   | `/api/v1/auth/refresh`            | Public | Refresh access token               |
| POST   | `/api/v1/auth/logout`             | Bearer | Invalidate refresh token           |

### 6.2 Spring Authorization Server Endpoints (auto-configured)

| Method | Path                                          | Auth   | Description                                |
|--------|-----------------------------------------------|--------|--------------------------------------------|
| GET    | `/.well-known/openid-configuration`           | Public | OIDC discovery document                    |
| GET    | `/oauth2/jwks`                                | Public | JSON Web Key Set for token verification    |
| GET    | `/oauth2/authorize`                           | Public | Authorization endpoint (code flow)         |
| POST   | `/oauth2/token`                               | Client | Token endpoint (code exchange, refresh)    |
| POST   | `/oauth2/revoke`                              | Client | Token revocation                           |
| GET    | `/oauth2/authorization/{provider}`            | Public | Start federated social login (Google, LinkedIn) |
| GET    | `/userinfo`                                   | Bearer | OpenID Connect UserInfo endpoint           |

### 6.3 Register — Request / Response

**Request**
```json
{
  "email": "user@example.com",
  "password": "S3cur3!Pass",
  "fullName": "Jane Doe",
  "country": "NL"
}
```

**Response 201**
```json
{
  "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "email": "user@example.com",
  "fullName": "Jane Doe",
  "region": "EU",
  "accessToken": "eyJ...",
  "refreshToken": "dGhpcyBpcyBhIH..."
}
```

---

## 7. Data Model (subset)

```sql
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL DEFAULT gen_random_uuid(),
    email_encrypted TEXT NOT NULL,          -- AES-256-GCM encrypted
    email_hash      TEXT NOT NULL UNIQUE,   -- SHA-256 for lookup
    password_hash   TEXT,                   -- bcrypt (null for social login)
    full_name       TEXT NOT NULL,
    country         VARCHAR(2) NOT NULL,    -- ISO 3166-1 alpha-2
    region          VARCHAR(10) NOT NULL,   -- EU, US, APAC …
    auth_provider   VARCHAR(20) DEFAULT 'LOCAL',  -- LOCAL, GOOGLE, LINKEDIN
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE user_profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    tenant_id       UUID NOT NULL,
    phone_encrypted TEXT,
    address_encrypted TEXT,
    linkedin_url    TEXT,
    preferred_job_titles TEXT[],
    preferred_locations  TEXT[],
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);
```

---

## 8. Security Considerations

| Concern                | Mitigation                                                      |
|------------------------|-----------------------------------------------------------------|
| Brute-force login      | Rate limit: 5 attempts / minute per IP (Redis counter).         |
| Password storage       | bcrypt with cost factor 12.                                     |
| Token leakage          | Short-lived access tokens (15 min); refresh tokens rotated.     |
| Email enumeration      | Generic error message: "Invalid credentials".                   |
| Region assignment      | Immutable after registration; migration requires support ticket. |

---

## 9. Testing Strategy

| Level        | Tool / Approach                                                  |
|--------------|------------------------------------------------------------------|
| Unit         | JUnit 5 + Mockito — service layer logic, password hashing.       |
| Integration  | Testcontainers (Postgres) — repository + full auth flow.         |
| Contract     | Spring Cloud Contract — API schema validation.                   |
| E2E (local)  | Docker Compose + REST Assured.                                   |

---

## 10. Decisions

| # | Question                                                              | Decision                                                              |
|---|-----------------------------------------------------------------------|-----------------------------------------------------------------------|
| 1 | Should we support MFA (TOTP) in v1 or defer to v2?                   | **Defer to v2.** Focus on core auth flows first.                      |
| 2 | Do we want a separate Keycloak instance or embed Spring Auth Server?  | **Embed Spring Authorization Server.** See §11 for rationale.         |

---

## 11. Spring Authorization Server — Architecture

### 11.1 Why Embedded (not Keycloak)

| Criterion            | Embedded Spring Auth Server                        | Keycloak                                    |
|----------------------|----------------------------------------------------|---------------------------------------------|
| **Deployment**       | Runs inside the same Spring Boot app (single JVM)  | Separate JVM / container to operate          |
| **Customisation**    | Full control via Java code; custom token claims, federated identity mappers | Admin UI + SPI plugins (less flexible)     |
| **Data residency**   | Uses the same regional PostgreSQL — no extra DB    | Needs its own DB per region                  |
| **Scalability**      | Scales with the app (stateless JWT)                | Must scale independently                     |
| **Maintenance**      | Managed as part of the app; same CI/CD pipeline    | Separate upgrade / patch cycle               |
| **Library fit**      | Native Spring Security 6 + Spring Boot 4 integration | Adapter library; version compatibility risk |
| **Footprint**        | ~0 extra memory (embedded)                         | ~512 MB+ heap per Keycloak instance          |

### 11.2 Module Structure

```
com.jobagent.jobagent.auth
├── config/
│   ├── AuthorizationServerConfig.java     ← @Configuration for Spring Auth Server
│   ├── SecurityFilterChainConfig.java     ← Resource server + login page filter chains
│   ├── FederatedIdentityConfig.java       ← Google & LinkedIn OAuth2 Client config
│   ├── TokenCustomizer.java              ← Add region, tenant_id claims to JWT
│   └── CorsConfig.java
├── federation/
│   ├── FederatedIdentityUserHandler.java  ← Auto-create user on first social login
│   └── UserRepositoryOAuth2UserService.java ← Map OAuth2User to local User entity
├── controller/
│   ├── AuthController.java               ← /register, /login, /refresh, /logout
│   └── UserInfoController.java           ← /userinfo (OIDC)
├── service/
│   ├── UserService.java
│   ├── JpaRegisteredClientRepository.java ← Store OAuth2 clients in PostgreSQL
│   └── JpaOAuth2AuthorizationService.java ← Store authorizations in PostgreSQL
├── repository/
│   ├── UserRepository.java
│   ├── UserProfileRepository.java
│   ├── OAuth2ClientRepository.java
│   └── OAuth2AuthorizationRepository.java
├── model/
│   ├── User.java
│   ├── UserProfile.java
│   ├── OAuth2RegisteredClient.java
│   └── OAuth2Authorization.java
└── dto/
    ├── RegisterRequest.java
    ├── LoginRequest.java
    └── AuthResponse.java
```

### 11.3 Key Configuration

```java
@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain authServerFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults());           // Enable OIDC
        http.oauth2Login(login -> login                 // Federated identity
            .loginPage("/login")
        );
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain resourceServerFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(
            JpaRegisteredClientRepository jpaRepo) {
        return jpaRepo;  // Clients stored in PostgreSQL
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        // RSA key pair for signing JWTs — loaded from KMS in prod, file in local
        RSAKey rsaKey = loadRsaKey();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (selector, context) -> selector.select(jwkSet);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
            .issuer("https://auth.jobagent.com")   // per-region issuer in prod
            .build();
    }
}
```

### 11.4 Custom JWT Claims

The `TokenCustomizer` adds platform-specific claims to every JWT:

```java
@Component
public class TokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Override
    public void customize(JwtEncodingContext context) {
        if (context.getTokenType() == OAuth2TokenType.ACCESS_TOKEN) {
            User user = loadUser(context.getPrincipal());
            context.getClaims()
                .claim("tenant_id", user.getTenantId().toString())
                .claim("region", user.getRegion())
                .claim("country", user.getCountry())
                .claim("user_id", user.getId().toString());
        }
    }
}
```

**Sample decoded access token:**
```json
{
  "iss": "https://auth-eu.jobagent.com",
  "sub": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "aud": "jobagent-client",
  "exp": 1740000000,
  "iat": 1739999100,
  "tenant_id": "a1b2c3d4-...",
  "region": "EU",
  "country": "NL",
  "user_id": "f47ac10b-...",
  "scope": "openid profile"
}
```

### 11.5 Federated Identity (Social Login)

```java
@Configuration
public class FederatedIdentityConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(
            googleClientRegistration(),
            linkedinClientRegistration()
        );
    }

    private ClientRegistration googleClientRegistration() {
        return ClientRegistration.withRegistrationId("google")
            .clientId("${GOOGLE_CLIENT_ID}")
            .clientSecret("${GOOGLE_CLIENT_SECRET}")
            .scope("openid", "profile", "email")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://oauth2.googleapis.com/token")
            .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
            .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .build();
    }

    private ClientRegistration linkedinClientRegistration() {
        return ClientRegistration.withRegistrationId("linkedin")
            .clientId("${LINKEDIN_CLIENT_ID}")
            .clientSecret("${LINKEDIN_CLIENT_SECRET}")
            .scope("openid", "profile", "email")
            .authorizationUri("https://www.linkedin.com/oauth/v2/authorization")
            .tokenUri("https://www.linkedin.com/oauth/v2/accessToken")
            .userInfoUri("https://api.linkedin.com/v2/userinfo")
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .build();
    }
}
```

### 11.6 Authorization Data Model (additional tables)

```sql
-- Spring Authorization Server stores clients and authorizations in PostgreSQL
-- (standard schema from spring-authorization-server)

CREATE TABLE oauth2_registered_client (
    id                          VARCHAR(100) PRIMARY KEY,
    client_id                   VARCHAR(100) NOT NULL,
    client_id_issued_at         TIMESTAMPTZ DEFAULT now(),
    client_secret               VARCHAR(200),
    client_secret_expires_at    TIMESTAMPTZ,
    client_name                 VARCHAR(200) NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types   VARCHAR(1000) NOT NULL,
    redirect_uris               VARCHAR(1000),
    post_logout_redirect_uris   VARCHAR(1000),
    scopes                      VARCHAR(1000) NOT NULL,
    client_settings             VARCHAR(2000) NOT NULL,
    token_settings              VARCHAR(2000) NOT NULL
);

CREATE TABLE oauth2_authorization (
    id                          VARCHAR(100) PRIMARY KEY,
    registered_client_id        VARCHAR(100) NOT NULL,
    principal_name              VARCHAR(200) NOT NULL,
    authorization_grant_type    VARCHAR(100) NOT NULL,
    authorized_scopes           VARCHAR(1000),
    attributes                  TEXT,
    state                       VARCHAR(500),
    authorization_code_value    TEXT,
    authorization_code_issued_at TIMESTAMPTZ,
    authorization_code_expires_at TIMESTAMPTZ,
    authorization_code_metadata TEXT,
    access_token_value          TEXT,
    access_token_issued_at      TIMESTAMPTZ,
    access_token_expires_at     TIMESTAMPTZ,
    access_token_metadata       TEXT,
    access_token_type           VARCHAR(100),
    access_token_scopes         VARCHAR(1000),
    oidc_id_token_value         TEXT,
    oidc_id_token_issued_at     TIMESTAMPTZ,
    oidc_id_token_expires_at    TIMESTAMPTZ,
    oidc_id_token_metadata      TEXT,
    refresh_token_value         TEXT,
    refresh_token_issued_at     TIMESTAMPTZ,
    refresh_token_expires_at    TIMESTAMPTZ,
    refresh_token_metadata      TEXT,
    user_code_value             TEXT,
    user_code_issued_at         TIMESTAMPTZ,
    user_code_expires_at        TIMESTAMPTZ,
    user_code_metadata          TEXT,
    device_code_value           TEXT,
    device_code_issued_at       TIMESTAMPTZ,
    device_code_expires_at      TIMESTAMPTZ,
    device_code_metadata        TEXT
);

CREATE TABLE oauth2_authorization_consent (
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name       VARCHAR(200) NOT NULL,
    authorities          VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);
```

### 11.7 Token Lifecycle

| Token            | Lifetime | Storage                                          |
|------------------|----------|--------------------------------------------------|
| Access Token     | 15 min   | Stateless JWT (not stored server-side)           |
| Refresh Token    | 7 days   | `oauth2_authorization` table (rotated on use)    |
| Authorization Code | 5 min | `oauth2_authorization` table (single-use)        |
| ID Token         | 15 min   | Stateless JWT (returned with access token)       |

### 11.8 Per-Region Issuer

Each regional deployment uses a distinct issuer URL so tokens cannot cross regions:

| Region | Issuer URL                         |
|--------|------------------------------------|
| EU     | `https://auth-eu.jobagent.com`     |
| US     | `https://auth-us.jobagent.com`     |
| APAC   | `https://auth-apac.jobagent.com`   |

The API Gateway validates the `iss` claim matches the expected region.
