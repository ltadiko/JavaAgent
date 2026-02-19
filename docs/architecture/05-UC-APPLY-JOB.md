# 05 — Use Case: Apply to Job

## 1. Summary

The user triggers an automated job application.  
The **ApplyAgent** (Spring AI) takes the user's CV, approved motivation letter, and profile data, then submits the application through the job platform's application mechanism (email, API, or form fill).  
The result (success/failure) is recorded.

---

## 2. Actors

| Actor          | Description                                                    |
|----------------|----------------------------------------------------------------|
| **User**       | Authenticated job seeker with CV, letter, and a target job.    |
| **ApplyAgent** | Spring AI agent that automates the application submission.     |

---

## 3. Preconditions

- User is authenticated.
- An **active** CV exists.
- A motivation letter with status `APPROVED` exists for the target job.
- The target `job_listings` record has a valid `source_url`.

## 4. Postconditions

- An `applications` record is created with status `SUBMITTED` or `FAILED`.
- The motivation letter status is updated to `USED`.
- A Kafka event `ApplicationSubmitted` is published.
- The user is notified of the result.

---

## 5. Sequence Diagram

```
User          Frontend         Backend (Apply Module)       ApplyAgent (Spring AI)        Job Platform       PostgreSQL       Kafka
 │              │                     │                          │                            │                 │               │
 │ "Apply to    │                     │                          │                            │                 │               │
 │  job X"      │                     │                          │                            │                 │               │
 │─────────────►│ POST /api/v1/       │                          │                            │                 │               │
 │              │  applications       │                          │                            │                 │               │
 │              │  { jobId, letterId }│                          │                            │                 │               │
 │              │────────────────────►│                          │                            │                 │               │
 │              │                     │ 1. Validate: CV, letter  │                            │                 │               │
 │              │                     │    approved, no duplicate│                            │                 │               │
 │              │                     │───────────────────────────────────────────────────────────────────────►│               │
 │              │                     │◄──── validation OK ──────────────────────────────────────────────────│               │
 │              │                     │ 2. Create application     │                            │                 │               │
 │              │                     │    record (PENDING)       │                            │                 │               │
 │              │                     │───────────────────────────────────────────────────────────────────────►│               │
 │              │                     │ 3. Delegate to agent      │                            │                 │               │
 │              │                     │──────────────────────────►│                            │                 │               │
 │              │                     │                          │ 3a. Determine apply method  │                 │               │
 │              │                     │                          │     (EMAIL / API / FORM)    │                 │               │
 │              │                     │                          │ 3b. Prepare payload         │                 │               │
 │              │                     │                          │     (CV + letter + profile) │                 │               │
 │              │                     │                          │ 3c. Submit application      │                 │               │
 │              │                     │                          │────────────────────────────►│                 │               │
 │              │                     │                          │◄──── result ───────────────│                 │               │
 │              │                     │◄──── success / failure ──│                            │                 │               │
 │              │                     │ 4. Update application     │                            │                 │               │
 │              │                     │    status (SUBMITTED)     │                            │                 │               │
 │              │                     │───────────────────────────────────────────────────────────────────────►│               │
 │              │                     │ 5. Publish event          │                            │                 │               │
 │              │                     │──────────────────────────────────────────────────────────────────────────────────────►│
 │              │◄── 201 AppDTO ──────│                          │                            │                 │               │
 │◄── Show      │                     │                          │                            │                 │               │
 │   result     │                     │                          │                            │                 │               │
```

---

## 6. API Endpoints

| Method | Path                                     | Auth   | Description                            |
|--------|------------------------------------------|--------|----------------------------------------|
| POST   | `/api/v1/applications`                   | Bearer | Submit a new application               |
| GET    | `/api/v1/applications/{id}`              | Bearer | Get application details                |
| POST   | `/api/v1/applications/{id}/retry`        | Bearer | Retry a failed application             |
| DELETE | `/api/v1/applications/{id}`              | Bearer | Cancel/withdraw an application         |

### 6.1 Submit — Request

```json
{
  "jobId": "j-001-...",
  "letterId": "ml-001-...",
  "cvId": "a1b2c3d4-...",
  "additionalMessage": "Available to start from April 2026"
}
```

### 6.2 Submit — Response 201

```json
{
  "applicationId": "app-001-...",
  "jobId": "j-001-...",
  "status": "SUBMITTED",
  "submittedAt": "2026-02-19T12:00:00Z",
  "applyMethod": "EMAIL",
  "confirmationRef": "REF-XYZ-123",
  "nextSteps": "The employer typically responds within 5 business days."
}
```

---

## 7. ApplyAgent — Spring AI Design

### 7.1 Agent with Tool Calling

```java
@Component
public class ApplyAgent {

    private final ChatClient chatClient;

    public ApplicationResult apply(ApplicationContext context) {
        return chatClient.prompt()
            .system("""
                You are a job application submission agent.
                Determine the best method to apply for this job and use the
                appropriate tool. Methods: EMAIL, API, FORM.
                Always include the CV and motivation letter.
                Return the result as structured JSON.
            """)
            .user(context.toPrompt())
            .functions(
                "sendEmailApplication",
                "submitViaApi",
                "fillWebForm"
            )
            .call()
            .entity(ApplicationResult.class);
    }
}
```

### 7.2 Tool Functions

| Function                | Description                                                        |
|-------------------------|--------------------------------------------------------------------|
| `sendEmailApplication`  | Compose and send an email with CV + letter attachments via SMTP.   |
| `submitViaApi`          | Call the job platform's API (if available) with structured payload. |
| `fillWebForm`           | Use headless browser (Playwright) to fill and submit a web form.   |

### 7.3 Apply Method Resolution

```
1. If job_listings.source = 'linkedin' or 'indeed'
   → Check if platform API supports direct application
   → If yes: submitViaApi
   → If no: sendEmailApplication (if email found in listing)

2. If job_listings has application_url pointing to a form
   → fillWebForm (headless Playwright)

3. Fallback: sendEmailApplication to company HR email
   (extracted from job description by AI)
```

---

## 8. Data Model (subset)

```sql
CREATE TABLE applications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id),
    tenant_id           UUID NOT NULL,
    job_listing_id      UUID NOT NULL REFERENCES job_listings(id),
    cv_id               UUID NOT NULL REFERENCES cv_details(id),
    letter_id           UUID REFERENCES motivation_letters(id),
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    -- PENDING, SUBMITTED, FAILED, WITHDRAWN, INTERVIEW, REJECTED, OFFERED, ACCEPTED
    apply_method        VARCHAR(20),             -- EMAIL, API, FORM
    confirmation_ref    TEXT,
    failure_reason      TEXT,
    additional_message  TEXT,
    submitted_at        TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ DEFAULT now(),
    created_at          TIMESTAMPTZ DEFAULT now(),
    UNIQUE (user_id, job_listing_id)           -- prevent duplicate applications
);

CREATE TABLE application_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id  UUID NOT NULL REFERENCES applications(id),
    tenant_id       UUID NOT NULL,
    event_type      VARCHAR(30) NOT NULL,        -- STATUS_CHANGE, RETRY, NOTE
    old_status      VARCHAR(30),
    new_status      VARCHAR(30),
    details         TEXT,
    created_at      TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_applications_user ON applications(user_id, tenant_id);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_application_events ON application_events(application_id);
```

---

## 9. Idempotency & Duplicate Prevention

- A `UNIQUE (user_id, job_listing_id)` constraint prevents applying to the same job twice.
- The POST endpoint is idempotent: if an application already exists, return 409 Conflict with the existing record.
- Each application attempt is recorded in `application_events` for full audit trail.

---

## 10. Async Processing

For form-fill applications (slow, browser-based):
1. The API immediately returns `202 Accepted` with status `PENDING`.
2. A Kafka message `ApplicationSubmitRequested` is published.
3. A consumer picks it up and runs the ApplyAgent asynchronously.
4. On completion, the application status is updated and a push notification / webhook fires.

```
POST /applications → 202 { status: PENDING }
                      │
                      ▼
              Kafka: ApplicationSubmitRequested
                      │
                      ▼
              Consumer: ApplyAgent.apply()
                      │
                      ▼
              Update applications.status → SUBMITTED / FAILED
              Publish: ApplicationSubmitted / ApplicationFailed
```

---

## 11. Email Application Details

When applying via email:
- **From**: User's email (or a relay: `apply-{userId}@jobagent.com` that forwards replies).
- **To**: HR email extracted from job listing.
- **Subject**: `Application for {jobTitle} — {userName}`
- **Body**: Motivation letter text.
- **Attachments**: CV PDF + motivation letter PDF.
- Sent via **Spring Mail** + SMTP (SendGrid / SES).

---

## 12. Error Handling & Retry

| Scenario                        | Behaviour                                              |
|---------------------------------|--------------------------------------------------------|
| Duplicate application           | 409 Conflict; return existing application.             |
| Letter not approved             | 400 "Motivation letter must be approved before applying." |
| Email send failure              | Mark FAILED; allow retry via `/retry`.                 |
| Form fill timeout (2 min)       | Mark FAILED with reason; allow retry.                  |
| Platform API 429 (rate limited) | Retry with exponential backoff (max 3 attempts).       |

---

## 13. Testing Strategy

| Level        | Tool / Approach                                                      |
|--------------|----------------------------------------------------------------------|
| Unit         | Mock ApplyAgent tools; test method resolution logic.                  |
| Integration  | Testcontainers Postgres + GreenMail (SMTP); test email flow.         |
| AI Integration | Testcontainers Ollama (`mistral`); real agent apply-method resolution. |
| Form fill    | Playwright test against a local mock application form.               |
| Event        | Embedded Kafka (spring-kafka-test); verify event publishing.         |
| E2E          | Docker Compose (Ollama + GreenMail + mock platform); full apply flow.|
