# 06 — Use Case: View Applications

## 1. Summary

Authenticated users view a dashboard of all their submitted job applications.  
The dashboard shows current status, timeline of events, and allows filtering and sorting.  
Users can also update the status manually (e.g., mark as "Interview scheduled" or "Rejected") and add notes.

---

## 2. Actors

| Actor    | Description                              |
|----------|------------------------------------------|
| **User** | Authenticated job seeker with applications. |

---

## 3. Preconditions

- User is authenticated.
- At least one `applications` record exists (or an empty state is shown).

## 4. Postconditions

- Read-only operation for listing/viewing. No state changes unless the user updates status or adds a note.

---

## 5. Sequence Diagram

```
User          Frontend              Backend (Application Module)        PostgreSQL
 │              │                          │                               │
 │ View apps    │                          │                               │
 │─────────────►│ GET /api/v1/             │                               │
 │              │  applications?status=... │                               │
 │              │─────────────────────────►│                               │
 │              │                          │  SELECT applications           │
 │              │                          │  JOIN job_listings             │
 │              │                          │  JOIN motivation_letters       │
 │              │                          │  WHERE user_id = ? AND tenant  │
 │              │                          │  ORDER BY created_at DESC      │
 │              │                          │──────────────────────────────►│
 │              │                          │◄──── rows ───────────────────│
 │              │◄── 200 PagedApps ────────│                               │
 │◄── Show      │                          │                               │
 │   dashboard  │                          │                               │
 │              │                          │                               │
 │ Click app    │                          │                               │
 │─────────────►│ GET /api/v1/             │                               │
 │              │  applications/{id}       │                               │
 │              │─────────────────────────►│                               │
 │              │                          │  SELECT application + events   │
 │              │                          │──────────────────────────────►│
 │              │                          │◄──── detail + events ────────│
 │              │◄── 200 AppDetail ────────│                               │
 │◄── Show      │                          │                               │
 │   detail +   │                          │                               │
 │   timeline   │                          │                               │
```

---

## 6. API Endpoints

| Method | Path                                           | Auth   | Description                              |
|--------|-------------------------------------------------|--------|------------------------------------------|
| GET    | `/api/v1/applications`                          | Bearer | List all applications (paginated)        |
| GET    | `/api/v1/applications/{id}`                     | Bearer | Get application detail + event timeline  |
| PATCH  | `/api/v1/applications/{id}/status`              | Bearer | Manually update status                   |
| POST   | `/api/v1/applications/{id}/notes`               | Bearer | Add a note to the application            |
| GET    | `/api/v1/applications/stats`                    | Bearer | Get aggregated statistics                |

### 6.1 List Applications — Query Parameters

| Param    | Type   | Required | Description                                  |
|----------|--------|----------|----------------------------------------------|
| `status` | String | No       | Filter by status (comma-separated)           |
| `from`   | Date   | No       | Applications submitted after this date       |
| `to`     | Date   | No       | Applications submitted before this date      |
| `sort`   | String | No       | `date_asc`, `date_desc`, `status`, `company` |
| `page`   | Int    | No       | Page number (default 0)                      |
| `size`   | Int    | No       | Page size (default 20, max 100)              |

### 6.2 List — Response 200

```json
{
  "page": 0,
  "totalPages": 3,
  "totalResults": 47,
  "stats": {
    "total": 47,
    "submitted": 30,
    "interview": 5,
    "offered": 2,
    "rejected": 8,
    "pending": 2
  },
  "applications": [
    {
      "applicationId": "app-001-...",
      "jobTitle": "Senior Java Developer",
      "company": "Acme Corp",
      "location": "Amsterdam, NL",
      "status": "SUBMITTED",
      "applyMethod": "EMAIL",
      "submittedAt": "2026-02-19T12:00:00Z",
      "lastUpdatedAt": "2026-02-19T12:00:00Z",
      "matchScore": 0.92
    }
  ]
}
```

### 6.3 Detail — Response 200

```json
{
  "applicationId": "app-001-...",
  "jobTitle": "Senior Java Developer",
  "company": "Acme Corp",
  "location": "Amsterdam, NL",
  "jobSourceUrl": "https://linkedin.com/jobs/...",
  "status": "INTERVIEW",
  "applyMethod": "EMAIL",
  "confirmationRef": "REF-XYZ-123",
  "submittedAt": "2026-02-19T12:00:00Z",
  "cvSummary": {
    "cvId": "a1b2c3d4-...",
    "fileName": "jane_doe_cv.pdf"
  },
  "letterSummary": {
    "letterId": "ml-001-...",
    "tone": "PROFESSIONAL",
    "wordCount": 342
  },
  "timeline": [
    {
      "eventType": "STATUS_CHANGE",
      "oldStatus": null,
      "newStatus": "PENDING",
      "details": "Application created",
      "createdAt": "2026-02-19T11:59:00Z"
    },
    {
      "eventType": "STATUS_CHANGE",
      "oldStatus": "PENDING",
      "newStatus": "SUBMITTED",
      "details": "Email sent successfully",
      "createdAt": "2026-02-19T12:00:00Z"
    },
    {
      "eventType": "NOTE",
      "details": "Received confirmation email from HR",
      "createdAt": "2026-02-20T09:00:00Z"
    },
    {
      "eventType": "STATUS_CHANGE",
      "oldStatus": "SUBMITTED",
      "newStatus": "INTERVIEW",
      "details": "Interview scheduled for March 5",
      "createdAt": "2026-02-21T14:30:00Z"
    }
  ],
  "notes": [
    {
      "noteId": "n-001-...",
      "text": "Received confirmation email from HR",
      "createdAt": "2026-02-20T09:00:00Z"
    }
  ]
}
```

### 6.4 Update Status — Request

```json
{
  "status": "INTERVIEW",
  "details": "Interview scheduled for March 5"
}
```

**Allowed status transitions:**
```
PENDING → SUBMITTED, FAILED
SUBMITTED → INTERVIEW, REJECTED, WITHDRAWN
INTERVIEW → OFFERED, REJECTED, WITHDRAWN
OFFERED → ACCEPTED, REJECTED, WITHDRAWN
FAILED → PENDING (retry)
```

### 6.5 Statistics — Response 200

```json
{
  "period": "ALL_TIME",
  "total": 47,
  "byStatus": {
    "PENDING": 2,
    "SUBMITTED": 30,
    "INTERVIEW": 5,
    "OFFERED": 2,
    "ACCEPTED": 0,
    "REJECTED": 8,
    "WITHDRAWN": 0,
    "FAILED": 0
  },
  "byMonth": [
    {"month": "2026-01", "count": 15},
    {"month": "2026-02", "count": 32}
  ],
  "averageResponseTimeDays": 4.2,
  "interviewRate": 0.106,
  "offerRate": 0.042
}
```

---

## 7. Dashboard Features

### 7.1 Kanban View

Applications grouped by status columns:

```
┌─────────┐  ┌───────────┐  ┌───────────┐  ┌─────────┐  ┌──────────┐
│ PENDING  │  │ SUBMITTED │  │ INTERVIEW │  │ OFFERED │  │ ACCEPTED │
│  (2)     │  │   (30)    │  │    (5)    │  │   (2)   │  │   (0)    │
│          │  │           │  │           │  │         │  │          │
│ Card 1   │  │ Card 3    │  │ Card 33   │  │ Card 38 │  │          │
│ Card 2   │  │ Card 4    │  │ Card 34   │  │ Card 39 │  │          │
│          │  │ ...       │  │ ...       │  │         │  │          │
└─────────┘  └───────────┘  └───────────┘  └─────────┘  └──────────┘
```

### 7.2 List View

Traditional table with sortable columns and status filters.

### 7.3 Analytics View

Charts showing:
- Applications over time (line chart)
- Status distribution (pie chart)
- Response rate by company/source (bar chart)
- Average time to response (KPI card)

---

## 8. Notifications

| Event                     | Notification Channel                    |
|---------------------------|-----------------------------------------|
| Application submitted     | In-app notification                     |
| Status update (manual)    | In-app (confirmation)                   |
| No response after 7 days  | Email reminder to follow up             |
| Application failed        | In-app + email alert                    |

Notifications are triggered by consuming Kafka events:
- `ApplicationSubmitted`
- `ApplicationStatusChanged`
- `ApplicationFollowUpDue` (scheduled job)

---

## 9. Data Model (references)

This use case reads from tables defined in other use cases:
- `applications` (from UC-05)
- `application_events` (from UC-05)
- `job_listings` (from UC-03)
- `motivation_letters` (from UC-04)
- `cv_details` (from UC-02)

Additional table for notes:

```sql
CREATE TABLE application_notes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id  UUID NOT NULL REFERENCES applications(id),
    tenant_id       UUID NOT NULL,
    user_id         UUID NOT NULL REFERENCES users(id),
    note_text       TEXT NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_application_notes ON application_notes(application_id, tenant_id);
```

---

## 10. Performance Considerations

| Concern                    | Mitigation                                              |
|----------------------------|---------------------------------------------------------|
| Large application list     | Cursor-based pagination for large datasets.             |
| Dashboard load time        | Stats query uses materialized view, refreshed every 5 min. |
| Timeline for single app    | Indexed on `application_id`; typically < 20 events.     |
| Concurrent status updates  | Optimistic locking (`version` column on `applications`). |

---

## 11. Testing Strategy

| Level        | Tool / Approach                                                      |
|--------------|----------------------------------------------------------------------|
| Unit         | Test status transition validation; test stats calculation.            |
| Integration  | Testcontainers Postgres; seed data + query all views.                |
| Contract     | Validate paginated response schema.                                  |
| E2E          | Docker Compose; create applications → view dashboard.                |
