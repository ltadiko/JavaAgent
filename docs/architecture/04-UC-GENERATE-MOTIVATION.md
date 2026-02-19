# 04 — Use Case: Generate Motivation Letter

## 1. Summary

After finding a matching job, the user requests a **personalised motivation letter**.  
The **MotivationWriterAgent** (Spring AI) uses **RAG (Retrieval-Augmented Generation)** via `PgVectorStore` to retrieve the most relevant CV sections for the specific job, then combines them with the job description to produce a tailored, professional letter.  
The user can review, edit, regenerate, or approve the letter before using it to apply.

---

## 2. Actors

| Actor                      | Description                                                |
|----------------------------|------------------------------------------------------------|
| **User**                   | Authenticated job seeker with CV + selected job.           |
| **MotivationWriterAgent**  | Spring AI agent that generates the motivation letter.      |

---

## 3. Preconditions

- User is authenticated.
- User has an **active** CV (`cv_details` with `parsed_json`).
- A `job_listings` record exists for the target job.

## 4. Postconditions

- A `motivation_letters` record is created (or updated if regenerated).
- The letter text is stored encrypted at rest.
- An optional PDF rendering is stored in S3.

---

## 5. Sequence Diagram

```
User          Frontend         Backend (Motivation Module)    MotivationWriterAgent     PgVectorStore    PostgreSQL       S3
 │              │                     │                              │                     │                │              │
 │ "Generate    │                     │                              │                     │                │              │
 │  letter for  │                     │                              │                     │                │              │
 │  job X"      │                     │                              │                     │                │              │
 │─────────────►│ POST /api/v1/       │                              │                     │                │              │
 │              │  motivation-letters  │                              │                     │                │              │
 │              │  { jobId, cvId,      │                              │                     │                │              │
 │              │    tone, language }  │                              │                     │                │              │
 │              │────────────────────►│                              │                     │                │              │
 │              │                     │ 1. Load CV parsed_json       │                     │                │              │
 │              │                     │ 2. Load job description      │                     │                │              │
 │              │                     │──────────────────────────────────────────────────────────────────────►│              │
 │              │                     │◄──── CV + job data ──────────────────────────────────────────────────│              │
 │              │                     │                              │                     │                │              │
 │              │                     │ 3. Call agent with           │                     │                │              │
 │              │                     │    QuestionAnswerAdvisor     │                     │                │              │
 │              │                     │    (RAG enabled)             │                     │                │              │
 │              │                     │────────────────────────────►│                     │                │              │
 │              │                     │                              │ 3a. RAG Retrieval:  │                │              │
 │              │                     │                              │     Query vector     │                │              │
 │              │                     │                              │     store with job   │                │              │
 │              │                     │                              │     description      │                │              │
 │              │                     │                              │────────────────────►│                │              │
 │              │                     │                              │◄── Top-5 CV chunks ─│                │              │
 │              │                     │                              │    (most relevant    │                │              │
 │              │                     │                              │     to this job)     │                │              │
 │              │                     │                              │                     │                │              │
 │              │                     │                              │ 3b. Augmented prompt:│                │              │
 │              │                     │                              │     System: letter   │                │              │
 │              │                     │                              │       writing rules  │                │              │
 │              │                     │                              │     Context: CV      │                │              │
 │              │                     │                              │       chunks (RAG)   │                │              │
 │              │                     │                              │     User: job desc   │                │              │
 │              │                     │                              │       + tone + lang  │                │              │
 │              │                     │                              │                     │                │              │
 │              │                     │                              │ 3c. LLM generates   │                │              │
 │              │                     │                              │     letter grounded  │                │              │
 │              │                     │                              │     in retrieved CV  │                │              │
 │              │                     │                              │     context          │                │              │
 │              │                     │◄──── letter text ────────────│                     │                │              │
 │              │                     │ 4. Persist                   │                     │                │              │
 │              │                     │──────────────────────────────────────────────────────────────────────►│              │
 │              │                     │ 5. (Optional) Render PDF     │                     │                │              │
 │              │                     │──────────────────────────────────────────────────────────────────────────────────────►│
 │              │◄── 201 LetterDTO ───│                              │                     │                │              │
 │◄── Display   │                     │                              │                     │                │              │
 │   letter for │                     │                              │                     │                │              │
 │   review     │                     │                              │                     │                │              │
```

---

## 6. API Endpoints

| Method | Path                                          | Auth   | Description                              |
|--------|-----------------------------------------------|--------|------------------------------------------|
| POST   | `/api/v1/motivation-letters`                  | Bearer | Generate a new motivation letter         |
| GET    | `/api/v1/motivation-letters/{id}`             | Bearer | Get a specific letter                    |
| PUT    | `/api/v1/motivation-letters/{id}`             | Bearer | Update (user edits the letter text)      |
| POST   | `/api/v1/motivation-letters/{id}/regenerate`  | Bearer | Regenerate with different params         |
| GET    | `/api/v1/motivation-letters/{id}/pdf`         | Bearer | Download PDF version (pre-signed URL)    |
| GET    | `/api/v1/motivation-letters`                  | Bearer | List all letters for the user            |
| DELETE | `/api/v1/motivation-letters/{id}`             | Bearer | Soft-delete a letter                     |

### 6.1 Generate — Request

```json
{
  "jobId": "j-001-...",
  "cvId": "a1b2c3d4-...",
  "tone": "PROFESSIONAL",
  "language": "en",
  "additionalInstructions": "Emphasise my Kubernetes experience"
}
```

**Tone options**: `PROFESSIONAL`, `ENTHUSIASTIC`, `FORMAL`, `CREATIVE`

### 6.2 Generate — Response 201

```json
{
  "letterId": "ml-001-...",
  "jobId": "j-001-...",
  "cvId": "a1b2c3d4-...",
  "status": "DRAFT",
  "tone": "PROFESSIONAL",
  "language": "en",
  "letterText": "Dear Hiring Manager,\n\nI am writing to express my strong interest in the Senior Java Developer position at Acme Corp...\n\n...\n\nSincerely,\nJane Doe",
  "wordCount": 342,
  "generatedAt": "2026-02-19T11:00:00Z",
  "pdfUrl": null
}
```

---

## 7. MotivationWriterAgent — Spring AI Design (RAG-Powered)

### 7.1 Agent with QuestionAnswerAdvisor (RAG)

```java
@Component
public class MotivationWriterAgent {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;     // PgVectorStore (Spring AI)

    @Value("classpath:prompts/motivation-letter.st")
    private Resource motivationPrompt;

    public MotivationLetterResult generate(CvParsedResult cv,
                                            JobListing job,
                                            LetterOptions options,
                                            UUID tenantId) {
        // RAG: QuestionAnswerAdvisor automatically:
        //   1. Embeds the user query (job description)
        //   2. Retrieves top-K similar documents from PgVectorStore
        //   3. Injects them into the prompt as context
        //   4. LLM generates grounded in retrieved CV chunks
        return chatClient.prompt()
            .system(motivationPrompt)
            .user(buildUserPrompt(cv, job, options))
            .advisors(new QuestionAnswerAdvisor(
                vectorStore,
                SearchRequest.builder()
                    .query(job.title() + " " + job.company() + " " + job.description())
                    .topK(5)
                    .similarityThreshold(0.65)
                    .filterExpression(
                        "tenant_id == '" + tenantId + "' && doc_type == 'cv_chunk'")
                    .build(),
                """
                Relevant sections from the candidate's CV:
                {question_answer_context}

                Use ONLY the information above and the candidate profile below.
                Never fabricate qualifications.
                """))
            .call()
            .entity(MotivationLetterResult.class);
    }

    private String buildUserPrompt(CvParsedResult cv, JobListing job, LetterOptions options) {
        return """
            ## Candidate Profile
            Name: %s
            Current Title: %s
            Years of Experience: %d

            ## Target Job
            Title: %s
            Company: %s
            Description: %s

            ## Instructions
            Tone: %s
            Language: %s
            Additional: %s
            """.formatted(
                cv.fullName(), cv.currentTitle(),
                cv.yearsOfExperience(),
                job.title(), job.company(), job.description(),
                options.tone(), options.language(),
                options.additionalInstructions()
            );
    }
}
```

### 7.2 How RAG Improves Letter Quality

| Aspect | Without RAG (prompt-stuffing) | With RAG (PgVectorStore) |
|--------|-------------------------------|--------------------------|
| **CV context** | Full CV parsed JSON dumped into prompt (~2-5 KB) | Only the 5 most relevant CV sections for *this specific job* |
| **Relevance** | LLM must pick relevant parts from full CV | Pre-filtered: only matching experience/skills arrive in prompt |
| **Long CVs** | May exceed token limits for 10+ page CVs | Chunked; always fits in context window |
| **Letter quality** | Generic — mentions all skills equally | Focused — emphasises the experience that matters for this role |
| **Token cost** | Higher (large prompt) | Lower (smaller, targeted prompt) |

### 7.3 RAG Flow Detail

```
1. User requests letter for "Senior Java Developer at Acme Corp"

2. QuestionAnswerAdvisor embeds: "Senior Java Developer Acme Corp [job description...]"

3. PgVectorStore similarity search:
   WHERE tenant_id = '{tenantId}' AND doc_type = 'cv_chunk'
   ORDER BY embedding <=> query_embedding
   LIMIT 5

4. Retrieved chunks (example):
   ├── [0.91] EXPERIENCE: "Led migration of monolith to microservices at Acme Corp (2020-2024)"
   ├── [0.88] SKILLS: "Skills: Java, Spring Boot, Kubernetes, PostgreSQL, Kafka, Docker"
   ├── [0.85] EXPERIENCE: "Built real-time data pipeline processing 1M events/day"
   ├── [0.80] EDUCATION: "MSc Computer Science at TU Delft (2018)"
   └── [0.76] SUMMARY: "Jane Doe, Senior Software Engineer, 8 years of experience"

5. Augmented prompt sent to LLM:
   ┌──────────────────────────────────────────────────────────────┐
   │ SYSTEM: You are an expert career coach... (motivation-letter.st)  │
   │                                                                    │
   │ CONTEXT (injected by RAG advisor):                                │
   │   Relevant sections from the candidate's CV:                       │
   │   - Led migration of monolith to microservices at Acme Corp...    │
   │   - Skills: Java, Spring Boot, Kubernetes, PostgreSQL, Kafka...   │
   │   - Built real-time data pipeline processing 1M events/day        │
   │   - MSc Computer Science at TU Delft (2018)                       │
   │   - Jane Doe, Senior Software Engineer, 8 years of experience     │
   │                                                                    │
   │ USER: [Candidate profile + job description + tone + language]      │
   └──────────────────────────────────────────────────────────────┘

6. LLM generates a letter that specifically highlights:
   - Microservices migration experience (directly relevant)
   - Java/Spring/K8s skills (matched to requirements)
   - Data pipeline work (shows scale experience)
```

**System Prompt** (`motivation-letter.st`):
```
You are an expert career coach and professional letter writer.

Write a motivation letter for a job application. The letter must:
1. Be addressed to "Dear Hiring Manager" unless the company name suggests otherwise.
2. Open with a compelling hook showing genuine interest.
3. Highlight 3-4 key qualifications from the candidate's CV that match the job.
4. Show knowledge of the company.
5. Close with a call to action.
6. Be concise: 250-400 words.
7. Match the requested tone and language.
8. Never fabricate qualifications — only use what's in the CV.

Return ONLY the letter text — no extra commentary.
```

---

## 8. Data Model (subset)

```sql
CREATE TABLE motivation_letters (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id),
    tenant_id           UUID NOT NULL,
    cv_id               UUID NOT NULL REFERENCES cv_details(id),
    job_listing_id      UUID NOT NULL REFERENCES job_listings(id),
    letter_text_encrypted TEXT NOT NULL,          -- AES-256-GCM encrypted
    tone                VARCHAR(20) NOT NULL,
    language            VARCHAR(5) NOT NULL,
    additional_instructions TEXT,
    word_count          INT,
    status              VARCHAR(20) DEFAULT 'DRAFT',  -- DRAFT, APPROVED, USED, DELETED
    pdf_s3_key          TEXT,                    -- S3 key for rendered PDF
    version             INT DEFAULT 1,           -- incremented on regenerate
    generated_at        TIMESTAMPTZ DEFAULT now(),
    updated_at          TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_motivation_letters_user ON motivation_letters(user_id, tenant_id);
CREATE INDEX idx_motivation_letters_job  ON motivation_letters(job_listing_id);
```

---

## 9. PDF Generation

- After the letter text is generated, an **async** process renders it as a PDF.
- Uses **OpenPDF** (open-source iText fork) or **Apache FOP**.
- Template includes a professional header (user name, contact info from profile), date, and the letter body.
- The PDF is stored in S3: `{tenant_id}/letters/{letter_id}/motivation.pdf`.
- A pre-signed URL is returned via the `/pdf` endpoint.

---

## 10. Regeneration Flow

When the user requests regeneration:
1. The existing letter's `version` is incremented.
2. The old text is kept in a `motivation_letter_history` table for audit.
3. The agent is called again with the same or modified params.
4. The new text replaces `letter_text_encrypted`; new PDF is generated.

```sql
CREATE TABLE motivation_letter_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    letter_id       UUID NOT NULL REFERENCES motivation_letters(id),
    version         INT NOT NULL,
    letter_text_encrypted TEXT NOT NULL,
    generated_at    TIMESTAMPTZ NOT NULL,
    UNIQUE (letter_id, version)
);
```

---

## 11. Error Handling

| Scenario                    | HTTP Status | Behaviour                                        |
|-----------------------------|-------------|--------------------------------------------------|
| CV not found / inactive     | 404         | `{"error": "No active CV found"}`                |
| Job listing not found       | 404         | `{"error": "Job listing not found"}`             |
| AI generation timeout (60s) | 503         | Retry once; if still fails, return error.        |
| Content safety filter       | 422         | `{"error": "Generated content did not pass safety filter"}` |

---

## 12. Multi-Language Support

The agent generates letters in the user's requested language.  
Supported languages (v1): English, Dutch, German, French, Spanish.  
The prompt includes: `Language: {language}` — the LLM generates natively in that language.

---

## 13. Testing Strategy

| Level        | Tool / Approach                                                      |
|--------------|----------------------------------------------------------------------|
| Unit         | Mock `ChatClient`; test prompt construction + response parsing.       |
| Integration  | Testcontainers Postgres; full generate → persist flow.               |
| AI Integration | Testcontainers Ollama (`mistral`); real letter generation with sample CV + job. |
| AI Quality   | Golden-file tests: known CV + job → validate letter structure.        |
| PDF          | Unit test PDF rendering with sample text; verify output is valid PDF. |
| E2E          | Docker Compose (Ollama + Postgres); generate letter for sample job.  |
