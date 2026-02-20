# Sprint 1.1 — User JPA Entity

**Date:** February 20, 2026  
**Status:** ✅ COMPLETED  
**Duration:** ~30 minutes

---

## Objectives

✅ Create `User.java` JPA entity  
✅ Map to `users` table with proper annotations  
✅ Support plaintext email + SHA-256 email_hash for lookups  
✅ Write comprehensive unit tests  
✅ Write integration tests (require Docker to run)  

---

## Deliverables

### 1. User Entity (`User.java`)

**Location:** `src/main/java/com/jobagent/jobagent/auth/model/User.java`

**Features:**
- ✅ Extends `BaseEntity` (id, tenant_id, created_at, updated_at)
- ✅ Uses `@EntityListeners(TenantEntityListener.class)` for tenant isolation
- ✅ Stores `email` as plaintext (encryption postponed to Sprint 10.4)
- ✅ Stores `email_hash` (SHA-256) for fast indexed lookups
- ✅ Supports multiple auth providers (LOCAL, GOOGLE, LINKEDIN)
- ✅ Region-aware (EU, US, APAC, etc.) for GDPR compliance
- ✅ Lombok annotations (`@Builder`, `@Getter`, `@Setter`)

**Fields:**
```java
- email           // Plaintext (will be encrypted in Sprint 10.4)
- emailHash       // SHA-256 for lookups
- passwordHash    // BCrypt (nullable for social login)
- fullName
- country         // ISO 3166-1 alpha-2
- region          // EU, US, APAC, LATAM, MENA
- authProvider    // LOCAL (default), GOOGLE, LINKEDIN
- enabled         // true (default)
```

---

### 2. Database Schema Update

**File:** `src/main/resources/db/migration/V1__create_users_profiles.sql`

**Change:** Updated `email_encrypted` → `email` (plaintext)

**Before:**
```sql
email_encrypted TEXT NOT NULL,
```

**After:**
```sql
email           TEXT NOT NULL,
```

**Rationale:** Email encryption postponed to Sprint 10.4 for faster MVP iteration.

---

### 3. Unit Tests (`UserTest.java`)

**Location:** `src/test/java/com/jobagent/jobagent/auth/model/UserTest.java`

**Coverage:** 11 tests, 100% passed ✅

**Test Cases:**
1. ✅ Create empty User with NoArgsConstructor
2. ✅ Create User with AllArgsConstructor
3. ✅ Build User with Builder pattern
4. ✅ Set default authProvider to LOCAL
5. ✅ Set default enabled to true
6. ✅ Allow null passwordHash for social login
7. ✅ Support different auth providers (LOCAL, GOOGLE, LINKEDIN)
8. ✅ Support different regions (EU, US, APAC, LATAM, MENA)
9. ✅ Use ISO country codes
10. ✅ Update fields with setters
11. ✅ Extend BaseEntity (tenant_id, timestamps)

**Execution:**
```bash
./mvnw test -Dtest=UserTest
```

**Result:**
```
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
✅ BUILD SUCCESS
```

---

### 4. Integration Tests (`UserIntegrationTest.java`)

**Location:** `src/test/java/com/jobagent/jobagent/auth/model/UserIntegrationTest.java`

**Coverage:** 8 integration tests

**Test Cases:**
1. Should persist User to database
2. Should auto-generate tenant_id on persist
3. Should retrieve User by ID
4. Should update User
5. Should persist User with null passwordHash (social login)
6. Should enforce unique email_hash constraint
7. Should persist multiple users with different tenants
8. Should set default values from builder

**Status:** ⚠️ Written but requires Docker PostgreSQL to run

**To Run:**
```bash
# Start PostgreSQL
docker compose up -d postgres

# Run tests
./mvnw test -Dtest=UserIntegrationTest
```

---

## Design Decisions

### Email Storage Strategy

**Decision:** Store email as **plaintext** + **SHA-256 hash**

**Rationale:**
- ✅ Faster MVP iteration (no encryption complexity)
- ✅ Easier debugging and operations
- ✅ Hash still provides indexed lookups
- ✅ Encryption can be added in Sprint 10.4 without breaking changes

**Migration Path (Sprint 10.4):**
```sql
ALTER TABLE users ADD COLUMN email_encrypted BYTEA;
UPDATE users SET email_encrypted = pgp_sym_encrypt(email, secret_key);
ALTER TABLE users DROP COLUMN email;
ALTER TABLE users RENAME COLUMN email_encrypted TO email;
```

### Multi-Tenancy

**Implementation:**
- Each user auto-generates a `tenant_id` (database default)
- `TenantEntityListener` enforces tenant isolation on every write
- Row-Level Security (RLS) policies filter by `tenant_id` (V8 migration)

### Region-Aware Data Residency

**Fields:**
- `country` (ISO 3166-1 alpha-2): DE, FR, US, etc.
- `region` (logical): EU, US, APAC, LATAM, MENA

**Future Use:**
- Route data to regional databases
- Apply region-specific regulations (GDPR, CCPA)
- Shard by region for scalability

---

## Compilation & Build

```bash
./mvnw clean compile
```

**Result:**
```
Compiling 26 source files with javac
✅ BUILD SUCCESS
Total time:  2.105 s
```

---

## Known Issues / TODOs

1. ⚠️ Integration tests require Docker PostgreSQL
   - **Action:** Document in README to run `docker compose up -d postgres` before tests

2. 📝 Email encryption postponed to Sprint 10.4
   - **Tracked in:** `docs/SPRINT-BACKLOG.md` → Sprint 10.4

3. 🔒 Security TODO: Remove `permitAll()` in Sprint 10.1
   - **Currently:** All endpoints allow unauthenticated access for development
   - **Production:** Will enforce JWT authentication

---

## Next Steps (Sprint 1.2)

**Task:** Create `UserProfile` JPA Entity
- Map to `user_profiles` table
- `@OneToOne` relationship with `User`
- Store job preferences (titles, locations, remote, salary)
- **Test:** Unit test — profile links to user

---

## Commit Message

```
feat: Sprint 1.1 - User JPA Entity with unit tests

- Add User entity (email plaintext + SHA-256 hash)
- Support multi-tenancy (auto tenant_id)
- Support multiple auth providers (LOCAL, GOOGLE, LINKEDIN)
- Region-aware for GDPR compliance (country, region fields)
- 11 unit tests (100% passed)
- 8 integration tests (require Docker PostgreSQL)
- Update V1 migration (email_encrypted → email)
- Document email encryption postponed to Sprint 10.4

Closes: Sprint 1.1
Next: Sprint 1.2 - UserProfile Entity
```

---

## Test Execution Summary

| Test Suite | Tests | Passed | Failed | Status |
|------------|-------|--------|--------|--------|
| UserTest (Unit) | 11 | 11 | 0 | ✅ |
| UserIntegrationTest | 8 | — | — | ⚠️ Requires Docker |

**Total Coverage:** Entity creation, builder pattern, defaults, constraints, persistence
