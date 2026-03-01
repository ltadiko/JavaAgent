# Sprint 2.1 — Complete Spring Authorization Server Implementation

**Date:** March 1, 2026  
**Status:** 🔵 PLANNED  
**Goal:** Complete remaining Sprint 2 tasks to enable JWT authentication

---

## Prerequisites Check

Before starting, verify:
- [x] PostgreSQL running via Docker Compose
- [x] All 101 tests passing (verified March 1, 2026)
- [x] OAuth2RegisteredClient entity exists
- [x] JpaRegisteredClientRepository exists
- [x] Flyway migrations V1-V10 applied

---

## Sprint 2.1 Tasks (Detailed)

### Task 2.1.1: JpaUserDetailsService ⏱️ ~20 min
**Goal:** Implement Spring Security UserDetailsService backed by our User entity.

**Files to create:**
- `src/main/java/com/jobagent/jobagent/auth/service/JpaUserDetailsService.java`

**Implementation:**
```java
@Service
public class JpaUserDetailsService implements UserDetailsService {
    // Lookup user by email_hash (SHA-256 of lowercase email)
    // Return Spring Security UserDetails with authorities
}
```

**Test:** `JpaUserDetailsServiceTest.java`
- [ ] Known email → UserDetails with correct username
- [ ] Unknown email → UsernameNotFoundException
- [ ] Disabled user → UserDetails.enabled = false

**Verification:**
```bash
mvn -Dtest=JpaUserDetailsServiceTest test
```

---

### Task 2.1.2: AuthorizationServerConfig ⏱️ ~30 min
**Goal:** Configure Spring Authorization Server with JPA clients, PKCE, JWK.

**Files to create:**
- `src/main/java/com/jobagent/jobagent/auth/config/AuthorizationServerConfig.java`

**Configuration:**
- Wire `JpaRegisteredClientRepository` as `RegisteredClientRepository` bean
- Create `JWKSource` with RSA key pair (dev: generate at startup)
- Configure `ProviderSettings` with issuer URL
- Register default `jobagent-spa` client programmatically (PKCE, authorization_code)

**Test:** `AuthorizationServerConfigTest.java`
- [ ] Context loads with authorization server beans
- [ ] `jobagent-spa` client exists and requires PKCE
- [ ] JWK endpoint accessible

**Verification:**
```bash
mvn -Dtest=AuthorizationServerConfigTest test
```

---

### Task 2.1.3: TokenCustomizer ⏱️ ~15 min
**Goal:** Add tenant-aware claims to JWT access tokens.

**Files to create:**
- `src/main/java/com/jobagent/jobagent/auth/security/TokenCustomizer.java`

**Claims to add:**
| Claim | Source |
|-------|--------|
| `user_id` | User.id |
| `tenant_id` | User.tenantId |
| `region` | User.region |

**Test:** `TokenCustomizerTest.java`
- [ ] Customizer adds `user_id` claim
- [ ] Customizer adds `tenant_id` claim
- [ ] Customizer adds `region` claim

**Verification:**
```bash
mvn -Dtest=TokenCustomizerTest test
```

---

### Task 2.1.4: Update TenantContextFilter for JWT ⏱️ ~15 min
**Goal:** Extract tenant claims from JWT and populate TenantContext.

**Files to update:**
- `src/main/java/com/jobagent/jobagent/common/multitenancy/TenantContextFilter.java`

**Logic:**
1. Check if `SecurityContextHolder` has `JwtAuthenticationToken`
2. Extract `tenant_id` claim from JWT
3. Set in `TenantContext.setTenantId()`
4. Set in MDC for logging

**Test:** `TenantContextFilterTest.java`
- [ ] JWT with tenant_id → TenantContext populated
- [ ] No authentication → TenantContext empty (public endpoints)
- [ ] After request → TenantContext cleared

**Verification:**
```bash
mvn -Dtest=TenantContextFilterTest test
```

---

### Task 2.1.5: Update SecurityConfig for Resource Server ⏱️ ~20 min
**Goal:** Configure JWT resource server and define route security rules.

**Files to update:**
- `src/main/java/com/jobagent/jobagent/common/security/SecurityConfig.java`

**Security Rules:**
| Path Pattern | Auth Required | Notes |
|--------------|---------------|-------|
| `POST /api/v1/auth/register` | No | Public registration |
| `GET /actuator/**` | No | Health/metrics |
| `GET /` | No | Welcome |
| `/oauth2/**` | No | Auth server endpoints |
| `/.well-known/**` | No | OIDC discovery |
| `GET /api/v1/**` | JWT | Protected |
| `POST /api/v1/**` | JWT | Protected |
| `PUT /api/v1/**` | JWT | Protected |
| `DELETE /api/v1/**` | JWT | Protected |

**Test:** `SecurityConfigTest.java`
- [ ] Public endpoints accessible without token
- [ ] Protected endpoints return 401 without token

**Verification:**
```bash
mvn -Dtest=SecurityConfigTest test
```

---

### Task 2.1.6: Integration E2E Smoke Test ⏱️ ~25 min
**Goal:** End-to-end test for registration → login → protected endpoint.

**Files to create:**
- `src/test/java/com/jobagent/jobagent/auth/AuthE2EIntegrationTest.java`

**Test Scenarios:**
1. Register new user → 201
2. Get access token (programmatic, not full PKCE flow)
3. Call protected endpoint with token → 200
4. Call protected endpoint without token → 401
5. Verify JWT contains tenant claims

**Verification:**
```bash
mvn -Dtest=AuthE2EIntegrationTest test
```

---

### Task 2.1.7: Documentation & Sprint Report ⏱️ ~10 min
**Goal:** Update documentation with completed work.

**Files to update:**
- `docs/SPRINT-2.0-REPORT.md` → Final status
- `docs/architecture/01-UC-REGISTER-LOGIN.md` → Update sequence diagram

---

## Execution Order

```
Task 2.1.1 → Test → Commit
     ↓
Task 2.1.2 → Test → Commit  
     ↓
Task 2.1.3 → Test → Commit
     ↓
Task 2.1.4 → Test → Commit
     ↓
Task 2.1.5 → Test → Commit
     ↓
Task 2.1.6 → Test → Commit
     ↓
Task 2.1.7 → Final verification → Commit
     ↓
Full test suite → Push
```

---

## Quality Gates

Before marking Sprint 2.1 complete:
- [ ] All unit tests pass: `mvn test -Dtest='!*IntegrationTest'`
- [ ] All integration tests pass: `mvn test`
- [ ] No compile warnings (except Lombok)
- [ ] Checkstyle/SpotBugs clean (if configured)
- [ ] API manual smoke test: register → login → call protected endpoint

---

## Estimated Duration

| Task | Time |
|------|------|
| 2.1.1 JpaUserDetailsService | 20 min |
| 2.1.2 AuthorizationServerConfig | 30 min |
| 2.1.3 TokenCustomizer | 15 min |
| 2.1.4 TenantContextFilter | 15 min |
| 2.1.5 SecurityConfig | 20 min |
| 2.1.6 E2E Test | 25 min |
| 2.1.7 Documentation | 10 min |
| **Total** | **~2.5 hours** |

---

## Notes

1. **JWK Key Management:** For MVP, generate RSA key at startup. Production will use external KMS.
2. **PKCE Testing:** Use programmatic token generation in tests to avoid brittle browser automation.
3. **Multi-tenancy:** Always derive `tenant_id` from DB user record, never trust client claims.

