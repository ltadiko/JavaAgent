# 02 — Use Case: Upload CV

## 1. Summary

Authenticated users upload their CV (PDF or DOCX).  
The **CVAnalyzerAgent** (Spring AI) extracts structured information — skills, experience, education, languages — and stores both the raw file and the parsed data.  
A vector embedding of the CV is generated for semantic job matching.  
The CV text is also **chunked by section and ingested into the PgVectorStore** (Spring AI) for RAG-powered motivation letter generation and job match explanations.

---

## 2. Actors

| Actor              | Description                                        |
|--------------------|----------------------------------------------------|
| **User**           | Authenticated job seeker.                          |
| **CVAnalyzerAgent**| Spring AI agent that parses and analyses the CV.   |

---

## 3. Preconditions

- User is authenticated (valid JWT).
- User has a `user_profiles` record.

## 4. Postconditions

- Raw CV file stored in S3 (region-local bucket).
- `cv_details` row created with structured extracted data (encrypted PII fields).
- CV embedding stored in `cv_embeddings` table (pgvector) for semantic matching.
- **CV chunks ingested into `PgVectorStore` (`vector_store` table) with metadata** (tenant_id, cv_id, section) for RAG retrieval.
- Previous CV (if any) is marked as `inactive`; its RAG chunks are removed from the vector store.

---

## 5. Sequence Diagram

```
User            Frontend           Backend (CV Module)     CVAnalyzerAgent (Spring AI)     S3       PostgreSQL / PgVectorStore
 │                │                      │                        │                        │               │
 │ Select file    │                      │                        │                        │               │
 │───────────────►│ POST /api/v1/cv      │                        │                        │               │
 │                │  (multipart/form)    │                        │                        │               │
 │                │─────────────────────►│                        │                        │               │
 │                │                      │  1. Validate file type │                        │               │
 │                │                      │     & size (≤ 10 MB)   │                        │               │
 │                │                      │  2. Upload raw file    │                        │               │
 │                │                      │─────────────────────────────────────────────────►│               │
 │                │                      │◄──────── S3 key ──────────────────────────────── │               │
 │                │                      │  3. Extract text (Tika)│                        │               │
 │                │                      │  4. Send text to AI    │                        │               │
 │                │                      │───────────────────────►│                        │               │
 │                │                      │                        │  Parse: skills,        │               │
 │                │                      │                        │  experience, education │               │
 │                │                      │◄───── structured JSON ─│                        │               │
 │                │                      │  5. Generate embedding │                        │               │
 │                │                      │     (whole CV)         │                        │               │
 │                │                      │───────────────────────►│                        │               │
 │                │                      │◄───── float[] vector ──│                        │               │
 │                │                      │  6. Chunk CV by section│                        │               │
 │                │                      │     (EXPERIENCE,       │                        │               │
 │                │                      │      EDUCATION, SKILLS,│                        │               │
 │                │                      │      PROJECTS, SUMMARY)│                        │               │
 │                │                      │  7. Ingest chunks into │                        │               │
 │                │                      │     PgVectorStore      │                        │               │
 │                │                      │     (with metadata:    │                        │               │
 │                │                      │      tenant_id, cv_id, │                        │               │
 │                │                      │      section, doc_type │                        │               │
 │                │                      │      = "cv_chunk")     │                        │               │
 │                │                      │  8. Persist cv_details │                        │               │
 │                │                      │     + cv_embeddings    │                        │               │
 │                │                      │──────────────────────────────────────────────────────────────────►│
 │                │                      │◄─────────────────────────────────────────────────────── OK ──────│
 │                │◄──── 201 CvSummary ──│                        │                        │               │
 │◄── Show parsed │                      │                        │                        │               │
 │   CV summary   │                      │                        │                        │               │
```

---

## 6. API Endpoints

| Method | Path                       | Auth   | Description                          |
|--------|----------------------------|--------|--------------------------------------|
| POST   | `/api/v1/cv`               | Bearer | Upload a new CV (multipart)          |
| GET    | `/api/v1/cv`               | Bearer | Get current active CV summary        |
| GET    | `/api/v1/cv/{id}/download` | Bearer | Get pre-signed S3 URL for raw file   |
| DELETE | `/api/v1/cv/{id}`          | Bearer | Soft-delete a CV                     |
| GET    | `/api/v1/cv/history`       | Bearer | List all CV versions for the user    |

### 6.1 Upload — Response 201

```json
{
  "cvId": "a1b2c3d4-...",
  "fileName": "jane_doe_cv.pdf",
  "uploadedAt": "2026-02-19T10:30:00Z",
  "parsedSummary": {
    "fullName": "Jane Doe",
    "currentTitle": "Senior Java Developer",
    "yearsOfExperience": 8,
    "skills": ["Java", "Spring Boot", "Kubernetes", "PostgreSQL", "Python"],
    "languages": [
      {"language": "English", "level": "Native"},
      {"language": "Dutch", "level": "B2"}
    ],
    "education": [
      {
        "degree": "MSc Computer Science",
        "institution": "TU Delft",
        "year": 2018
      }
    ],
    "experienceHighlights": [
      "Led migration of monolith to microservices at Acme Corp (2020–2024)",
      "Built real-time data pipeline processing 1M events/day"
    ]
  }
}
```

---

## 7. CVAnalyzerAgent — Spring AI Design

```java
@Component
public class CVAnalyzerAgent {

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;          // PgVectorStore (Spring AI)

    // Prompt template for structured extraction
    @Value("classpath:prompts/cv-parse.st")
    private Resource cvParsePrompt;

    public CvParsedResult parse(String extractedText) {
        // Uses function calling to return structured JSON
        return chatClient.prompt()
            .system(cvParsePrompt)
            .user(extractedText)
            .call()
            .entity(CvParsedResult.class);
    }

    public float[] embed(String extractedText) {
        return embeddingModel.embed(extractedText);
    }

    /**
     * Chunk the CV by section and ingest into PgVectorStore for RAG retrieval.
     * Each chunk is stored with metadata enabling filtered retrieval per tenant/CV.
     */
    public void ingestForRag(UUID cvId, UUID tenantId, CvParsedResult parsed, String fullText) {
        // 1. Remove old chunks for this CV (if re-uploading)
        vectorStore.delete(
            FilterExpressionBuilder.builder()
                .eq("cv_id", cvId.toString())
                .build());

        // 2. Chunk by section
        List<Document> documents = new ArrayList<>();

        // Skills chunk
        documents.add(createChunk(cvId, tenantId, "SKILLS",
            "Skills: " + String.join(", ", parsed.skills())));

        // Experience chunks (one per highlight, or grouped)
        for (String exp : parsed.experienceHighlights()) {
            documents.add(createChunk(cvId, tenantId, "EXPERIENCE", exp));
        }

        // Education chunk
        for (var edu : parsed.education()) {
            documents.add(createChunk(cvId, tenantId, "EDUCATION",
                edu.degree() + " at " + edu.institution() + " (" + edu.year() + ")"));
        }

        // Summary chunk
        documents.add(createChunk(cvId, tenantId, "SUMMARY",
            parsed.fullName() + ", " + parsed.currentTitle()
            + ", " + parsed.yearsOfExperience() + " years of experience"));

        // 3. Ingest into PgVectorStore (embeddings generated automatically)
        vectorStore.add(documents);
    }

    private Document createChunk(UUID cvId, UUID tenantId, String section, String text) {
        var doc = new Document(text);
        doc.getMetadata().put("cv_id", cvId.toString());
        doc.getMetadata().put("tenant_id", tenantId.toString());
        doc.getMetadata().put("section", section);
        doc.getMetadata().put("doc_type", "cv_chunk");
        return doc;
    }
}
```

**Prompt template** (`cv-parse.st`):
```
You are a CV analysis expert. Extract the following structured information from
the CV text provided by the user. Return valid JSON with these fields:
- fullName, currentTitle, yearsOfExperience
- skills (array of strings)
- languages (array of {language, level})
- education (array of {degree, institution, year})
- experienceHighlights (array of strings, max 5 bullet points)

Be precise. If a field is not found, use null.
```

---

## 8. Data Model (subset)

### 8.1 Application-Managed Tables

```sql
CREATE TABLE cv_details (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id),
    tenant_id           UUID NOT NULL,
    file_name           TEXT NOT NULL,
    s3_key              TEXT NOT NULL,
    content_type        VARCHAR(50) NOT NULL,
    file_size_bytes     BIGINT NOT NULL,
    extracted_text      TEXT,                    -- plain text from Tika (encrypted)
    parsed_json         JSONB NOT NULL,          -- structured CV data
    status              VARCHAR(20) DEFAULT 'ACTIVE',  -- ACTIVE, INACTIVE, DELETED
    created_at          TIMESTAMPTZ DEFAULT now(),
    updated_at          TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE cv_embeddings (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cv_id       UUID NOT NULL REFERENCES cv_details(id),
    tenant_id   UUID NOT NULL,
    embedding   vector(1536),                -- pgvector; whole-CV embedding for job ranking
    created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_cv_embeddings_vector ON cv_embeddings
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

### 8.2 Spring AI PgVectorStore Table (auto-managed)

Spring AI's `PgVectorStore` auto-creates and manages the `vector_store` table.  
CV chunks are stored here for **RAG retrieval** during motivation letter generation and job match explanations.

```sql
-- Auto-created by Spring AI PgVectorStore
CREATE TABLE IF NOT EXISTS vector_store (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content   TEXT NOT NULL,                    -- chunk text
    metadata  JSONB NOT NULL DEFAULT '{}',      -- { cv_id, tenant_id, section, doc_type }
    embedding vector(1536)                      -- auto-generated by EmbeddingModel
);

CREATE INDEX idx_vector_store_embedding ON vector_store
    USING hnsw (embedding vector_cosine_ops);
```

**Metadata schema for CV chunks:**
```json
{
  "cv_id": "a1b2c3d4-...",
  "tenant_id": "x9y8z7-...",
  "section": "EXPERIENCE",          // SKILLS, EXPERIENCE, EDUCATION, SUMMARY, PROJECTS
  "doc_type": "cv_chunk"            // cv_chunk, company_knowledge (v2)
}
```

### 8.3 Dual Storage Rationale

| Store             | Purpose                                    | Used By                     |
|-------------------|--------------------------------------------|-----------------------------|
| `cv_embeddings`   | Whole-CV embedding for job-to-CV **ranking** (cosine similarity) | UC-03: Job Search ranking   |
| `vector_store`    | Chunked CV sections for **RAG retrieval** (inject into LLM prompt) | UC-04: Motivation Letter, UC-03: Match explanation |

---

## 9. File Storage Details

| Aspect           | Detail                                                    |
|------------------|-----------------------------------------------------------|
| Bucket naming    | `jobagent-cv-{region}` (e.g., `jobagent-cv-eu`)          |
| Object key       | `{tenant_id}/cv/{cv_id}/{original_filename}`              |
| Encryption       | SSE-S3 (AES-256 server-side)                              |
| Lifecycle        | Objects of DELETED CVs hard-deleted after 30 days.        |
| Max file size    | 10 MB                                                     |
| Allowed types    | `application/pdf`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document` |

---

## 10. Error Handling

| Scenario                        | HTTP Status | Response                          |
|---------------------------------|-------------|-----------------------------------|
| Unsupported file type           | 400         | `{"error": "Only PDF and DOCX are supported"}` |
| File too large                  | 413         | `{"error": "File exceeds 10 MB limit"}`         |
| AI parsing fails (timeout)      | 503         | `{"error": "CV analysis temporarily unavailable"}` — retry with exponential backoff |
| S3 upload failure               | 502         | `{"error": "File storage unavailable"}`          |

---

## 11. Security & Privacy

- Raw CV text is **encrypted at rest** in the `extracted_text` column.
- The `parsed_json` may contain PII (name, address); sensitive leaf values are encrypted using the column-level converter.
- S3 access is via short-lived pre-signed URLs (5-minute TTL); no public bucket access.
- CV data participates in the right-to-erasure flow (see `00-SYSTEM-ARCHITECTURE.md` §6.5).

---

## 12. Testing Strategy

| Level        | Tool / Approach                                                    |
|--------------|--------------------------------------------------------------------|
| Unit         | Mock `ChatClient` + `EmbeddingModel`; test parsing logic.          |
| Integration  | Testcontainers Postgres + LocalStack S3; upload → parse → persist. |
| AI Integration | Testcontainers Ollama (`mistral`); real AI parsing with small CV. |
| AI Contract  | Snapshot tests for prompt template; validate JSON schema output.   |
| E2E          | Docker Compose (Ollama + Postgres + S3) + real small PDF upload.   |
