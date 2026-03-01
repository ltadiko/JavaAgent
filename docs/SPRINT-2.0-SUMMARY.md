# Sprint 2.0 — Spring Authorization Server (Planned)

**Date (planned start):** February 21, 2026  
**Status:** 🔵 Planned — Authorization Server (JWT + PKCE)  
**Duration (estimate):** 3–5 working days

---

## Summary

Sprint 2 implements an embedded Spring Authorization Server with JPA-backed RegisteredClient storage and PKCE support for the SPA, RSA JWK signing, and a token customizer that adds tenant-aware claims (`tenant_id`, `region`, `user_id`). The backend will be secured with JWT resource-server configuration. This sprint prepares stable authentication and tenant propagation required by Sprint 3 (CV Upload) and other business flows.

---

## What Will Be Done (Plan)

### 1. OAuth2 Registered Client Entity
- File: `src/main/java/com/jobagent/jobagent/auth/model/OAuth2RegisteredClient.java`
- Purpose: Persist registered clients to DB (Flyway V2 schema)
- Key fields: `id`, `clientId`, `clientSecret`, `clientName`, `clientAuthenticationMethods`, `authorizationGrantTypes`, `redirectUris`, `scopes`, `clientSettings`, `tokenSettings`

### 2. JPA-backed RegisteredClientRepository
- File: `src/main/java/com/jobagent/jobagent/auth/repository/JpaRegisteredClientRepository.java`
- Purpose: Convert between Spring Security `RegisteredClient` and the `OAuth2RegisteredClient` entity; provide `save()`, `findByClientId()`, `findById()`.

### 3. Authorization Server Configuration
- File: `src/main/java/com/jobagent/jobagent/auth/config/AuthorizationServerConfig.java`
- Purpose: Wire `RegisteredClientRepository`, `OAuth2AuthorizationService`, generate JWK RSA keypair (dev), ProviderSettings, register `jobagent-spa` client with PKCE and redirect `http://localhost:5173/callback`.
- Token settings: access token TTL = 30m, refresh TTL = 8h.

### 4. Token Customizer (add tenant claims)
- File: `src/main/java/com/jobagent/jobagent/auth/security/TokenCustomizer.java`
- Purpose: Add `tenant_id`, `user_id`, `region` claims into the access token. Authoritative values retrieved from the `User` record.

### 5. TenantContextFilter update
- File: `src/main/java/com/jobagent/jobagent/common/multitenancy/TenantContextFilter.java` (update)
- Purpose: Extract `tenant_id` from `JwtAuthenticationToken`, set `TenantContext` ThreadLocal and MDC for request scope.

### 6. Resource Server (SecurityConfig) update
- File: `src/main/java/com/jobagent/jobagent/common/security/SecurityConfig.java` (update)
- Purpose: Replace `anyRequest().permitAll()` with `http.oauth2ResourceServer().jwt()` config and the following route rules:
  - Public: `/actuator/**`, `/api/v1/auth/register`, `/`, static files
  - Protected: all other `/api/**` require a valid JWT

### 7. Tests (unit + integration)
- Unit tests:
  - `OAuth2RegisteredClientTest` (entity)
  - `JpaRegisteredClientRepositoryTest`
  - `AuthorizationServerConfigTest` (beans present, JWK generation, client config)
  - `TokenCustomizerTest` (claims injection)
  - `TenantContextFilterTest`
- Integration/E2E:
  - `AuthControllerIntegrationTest` (existing, re-run)
  - `AuthE2ESmokeTest` (Testcontainers Postgres): issue token (programmatic issuance in test) → call protected endpoint → assert tenant context + DB write

### 8. Docs & client snippet
- Update `docs/architecture/01-UC-REGISTER-LOGIN.md` with PKCE sequence diagram and token customization step
- Add `jobagent-ui/README-auth.md` with PKCE client example and callback handling snippet

---

## Deliverables (files to be created/modified)

Created (planned)
- `src/main/java/com/jobagent/jobagent/auth/model/OAuth2RegisteredClient.java`
- `src/main/java/com/jobagent/jobagent/auth/repository/JpaRegisteredClientRepository.java`
- `src/main/java/com/jobagent/jobagent/auth/config/AuthorizationServerConfig.java`
- `src/main/java/com/jobagent/jobagent/auth/security/TokenCustomizer.java`
- `src/test/java/com/jobagent/jobagent/auth/model/OAuth2RegisteredClientTest.java`
- `src/test/java/com/jobagent/jobagent/auth/repository/JpaRegisteredClientRepositoryTest.java`
- `src/test/java/com/jobagent/jobagent/auth/config/AuthorizationServerConfigTest.java`
- `src/test/java/com/jobagent/jobagent/auth/security/TokenCustomizerTest.java`
- `src/test/java/com/jobagent/jobagent/auth/AuthE2ESmokeTest.java` (Testcontainers)
- `jobagent-ui/README-auth.md`
- `docs/SPRINT-2.0-REPORT.md` (to be produced after implementation)

Modified (planned)
- `src/main/resources/db/migration/V2__create_oauth2_auth_server_tables.sql` (should already exist)
- `src/main/java/com/jobagent/jobagent/common/multitenancy/TenantContextFilter.java`
- `src/main/java/com/jobagent/jobagent/common/security/SecurityConfig.java`
- `docs/architecture/01-UC-REGISTER-LOGIN.md`

---

## Tests & Acceptance Criteria

Acceptance checklist (must pass)
- [ ] `OAuth2RegisteredClient` entity defined and unit-tested.
- [ ] `JpaRegisteredClientRepository` persists and retrieves `RegisteredClient` correctly.
- [ ] `RegisteredClientRepository`, `OAuth2AuthorizationService`, and `JWKSource` beans are present and configured.
- [ ] `jobagent-spa` client registered and requires PKCE with redirect `http://localhost:5173/callback`.
- [ ] Tokens include `tenant_id`, `user_id`, and `region` claims.
- [ ] `TenantContextFilter` populates `TenantContext` during request handling (cleared after).
- [ ] `SecurityConfig` enforces JWT validation; protected endpoints return 401/200 as expected.
- [ ] Integration E2E test passes: token issuance → protected call → DB write shows `tenant_id`.
- [ ] Documentation updated and `docs/SPRINT-2.0-REPORT.md` created after implementation.

Smoke/local test commands
- Fast compile:
```bash
make build
```
- Unit test example:
```bash
mvn -Dtest=com.jobagent.jobagent.auth.model.OAuth2RegisteredClientTest test
```
- Integration/E2E (requires Docker available to Testcontainers):
```bash
mvn -Dtest=com.jobagent.jobagent.auth.AuthE2ESmokeTest test
```
- Re-run auth controller integration:
```bash
mvn -Dtest=com.jobagent.jobagent.auth.controller.AuthControllerIntegrationTest test
```

---

## Risks & Notes

1. JWK key management:
   - Dev: generate RSA key at startup
   - Prod: integrate KMS or external JWK endpoint (documented)
2. PKCE test automation:
   - Programmatic token issuance in tests recommended (avoid brittle full-browser PKCE).
3. ArchUnit & Java 25:
   - ArchUnit may not parse Java 25 classfiles — if tests fail due to that, run unit tests excluding ArchUnit or set test-compile target for compatibility.
4. Security: never trust tenant claim from client; always derive `tenant_id` from authoritative DB user record when adding claims or enforcing tenant context.

---

## Git / Commit Guidance

Per-step commit messages (small commits, one target per commit):
- `feat(auth): add OAuth2RegisteredClient JPA entity`
- `feat(auth): add JpaRegisteredClientRepository`
- `feat(auth): wire AuthorizationServerConfig (JWK, provider settings)`
- `feat(auth): add TokenCustomizer to inject tenant claims`
- `fix(security): TenantContextFilter extracts tenant from Jwt`
- `test(auth): add AuthE2ESmokeTest (Testcontainers)`
- `docs: add SPRINT-2.0-SUMMARY.md and update architecture doc`

Final merge commit message (when Sprint complete):
```
chore(sprint-2): implement Spring Authorization Server (JPA clients, PKCE, token customization)
- OAuth2RegisteredClient entity
- JpaRegisteredClientRepository
- AuthorizationServerConfig (JWK + ProviderSettings)
- TokenCustomizer adds tenant claims
- TenantContextFilter updates
- SecurityConfig resource-server enabled
- Unit + Integration tests
- Docs updated
```

---

## Next Steps (after you confirm)
1. I will implement Task 1 (create `OAuth2RegisteredClient` entity + unit test), commit and push the change, then stop for your review and test run.
2. After your approval of Task 1 result, I will continue with Task 2, and so on, one small task per commit.

To save the file locally yourself
1. Create the path if necessary:
```bash
mkdir -p docs
```
2. Save the content above to:
```bash
cat > docs/SPRINT-2.0-SUMMARY.md <<'EOF'
# (paste the exact content from above here)
EOF
```
3. Commit & push:
```bash
git add docs/SPRINT-2.0-SUMMARY.md
git commit -m "docs: add SPRINT-2.0-SUMMARY.md - plan for Spring Authorization Server"
git push origin main
```

Would you like me to (A) save & commit `docs/SPRINT-2.0-SUMMARY.md` now (I will commit with the repo-local git identity), or (B) you will paste and commit locally?
