# 03 — Use Case: Search Jobs

## 1. Summary

Authenticated users search for jobs that match their profile.  
The **JobFinderAgent** (Spring AI) orchestrates multiple job sources — configured job-board scrapers, public APIs, and general web search — to find relevant positions.  
Results are ranked by semantic similarity between the user's CV embedding and the job description embedding.  
For the top results, **RAG retrieval** from the PgVectorStore pulls relevant CV chunks to generate detailed **match explanations** (matched skills, missing skills, relevance reasoning).

---

## 2. Actors

| Actor              | Description                                                    |
|--------------------|----------------------------------------------------------------|
| **User**           | Authenticated job seeker with an uploaded CV.                  |
| **JobFinderAgent** | Spring AI agent that searches, aggregates, and ranks jobs.     |

---

## 3. Preconditions

- User is authenticated.
- User has at least one **active** CV with a generated embedding.
- (Optional) User has configured preferred job titles / locations in their profile.

## 4. Postconditions

- A list of ranked job results is returned.
- Job results are cached in Redis (TTL = 1 hour) for pagination.
- Each job result is persisted in `job_listings` for later reference (motivation letter, apply).

---

## 5. Sequence Diagram

```
User          Frontend         Backend (JobSearch Module)    JobFinderAgent (Spring AI)    Job Sources     Redis    PostgreSQL / PgVectorStore
 │              │                     │                            │                         │              │               │
 │ Search jobs  │                     │                            │                         │              │               │
 │─────────────►│ GET /api/v1/jobs    │                            │                         │              │               │
 │              │  ?q=...&loc=...     │                            │                         │              │               │
 │              │────────────────────►│                            │                         │              │               │
 │              │                     │ 1. Check cache             │                         │              │               │
 │              │                     │────────────────────────────────────────────────────────────────────►│               │
 │              │                     │◄──── cache miss ──────────────────────────────────────────────────│               │
 │              │                     │ 2. Load user CV embedding  │                         │              │               │
 │              │                     │──────────────────────────────────────────────────────────────────────────────────►│
 │              │                     │◄──── embedding[] ──────────────────────────────────────────────────────────────│
 │              │                     │ 3. Delegate to agent       │                         │              │               │
 │              │                     │───────────────────────────►│                         │              │               │
 │              │                     │                            │ 3a. Search configured    │              │               │
 │              │                     │                            │     job boards (tools)   │              │               │
 │              │                     │                            │────────────────────────►│              │               │
 │              │                     │                            │◄── raw job listings ────│              │               │
 │              │                     │                            │ 3b. Web search (tool)    │              │               │
 │              │                     │                            │────────────────────────►│              │               │
 │              │                     │                            │◄── web results ─────────│              │               │
 │              │                     │                            │ 3c. Deduplicate & merge  │              │               │
 │              │                     │                            │ 3d. Generate embeddings  │              │               │
 │              │                     │                            │     for job descriptions │              │               │
 │              │                     │                            │ 3e. Rank by cosine       │              │               │
 │              │                     │                            │     similarity to CV     │              │               │
 │              │                     │◄── ranked job list ────────│                         │              │               │
 │              │                     │                            │                         │              │               │
 │              │                     │ 4. RAG: For top-N results, │                         │              │               │
 │              │                     │    retrieve relevant CV    │                         │              │               │
 │              │                     │    chunks from PgVectorStore                        │              │               │
 │              │                     │──────────────────────────────────────────────────────────────────────────────────►│
 │              │                     │◄──── CV chunks (top-5 per job) ────────────────────────────────────────────────│
 │              │                     │                            │                         │              │               │
 │              │                     │ 5. Generate match          │                         │              │               │
 │              │                     │    explanations via LLM    │                         │              │               │
 │              │                     │    (job desc + CV chunks   │                         │              │               │
 │              │                     │     → matchedSkills,       │                         │              │               │
 │              │                     │       missingSkills,       │                         │              │               │
 │              │                     │       reasoning)           │                         │              │               │
 │              │                     │───────────────────────────►│                         │              │               │
 │              │                     │◄── match explanations ─────│                         │              │               │
 │              │                     │                            │                         │              │               │
 │              │                     │ 6. Cache results           │                         │              │               │
 │              │                     │────────────────────────────────────────────────────────────────────►│               │
 │              │                     │ 7. Persist job_listings    │                         │              │               │
 │              │                     │──────────────────────────────────────────────────────────────────────────────────►│
 │              │◄── 200 JobPage ─────│                            │                         │              │               │
 │◄── Display   │                     │                            │                         │              │               │
 │   results    │                     │                            │                         │              │               │
```

---

## 6. API Endpoints

| Method | Path                        | Auth   | Description                                   |
|--------|-----------------------------|--------|-----------------------------------------------|
| GET    | `/api/v1/jobs`              | Bearer | Search jobs (query params below)              |
| GET    | `/api/v1/jobs/{id}`         | Bearer | Get single job listing detail                 |
| POST   | `/api/v1/jobs/refresh`      | Bearer | Force re-search (bypass cache)                |
| GET    | `/api/v1/jobs/saved`        | Bearer | Get user's saved/bookmarked jobs              |
| POST   | `/api/v1/jobs/{id}/save`    | Bearer | Bookmark a job listing                        |

### 6.1 Search Query Parameters

| Param      | Type   | Required | Description                            |
|------------|--------|----------|----------------------------------------|
| `q`        | String | No       | Free-text search query                 |
| `location` | String | No       | City / country filter                  |
| `remote`   | Boolean| No       | Remote-only filter                     |
| `salary_min`| Long  | No       | Minimum salary filter                  |
| `sources`  | String | No       | Comma-separated source filter          |
| `page`     | Int    | No       | Page number (default 0)               |
| `size`     | Int    | No       | Page size (default 20, max 50)        |

### 6.2 Search — Response 200

```json
{
  "page": 0,
  "totalPages": 5,
  "totalResults": 97,
  "results": [
    {
      "jobId": "j-001-...",
      "title": "Senior Java Developer",
      "company": "Acme Corp",
      "location": "Amsterdam, NL",
      "remote": true,
      "salaryRange": "€80,000 – €100,000",
      "matchScore": 0.92,
      "source": "linkedin",
      "sourceUrl": "https://linkedin.com/jobs/...",
      "postedAt": "2026-02-17T08:00:00Z",
      "descriptionSnippet": "We are looking for a Senior Java Developer to join our…",
      "matchExplanation": {
        "matchedSkills": ["Java", "Spring Boot", "Kubernetes", "PostgreSQL"],
        "missingSkills": ["Terraform"],
        "reasoning": "Strong match — candidate has 8 years of Java/Spring experience including microservices migration and data pipeline work, directly relevant to this role. Only gap is Terraform for infrastructure-as-code."
      }
    }
  ]
}
```

---

## 7. JobFinderAgent — Spring AI Design

### 7.1 Agent Architecture

The `JobFinderAgent` uses Spring AI **function calling** to invoke external tools:

```java
@Component
public class JobFinderAgent {

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;           // PgVectorStore for RAG

    public List<RankedJob> searchAndRank(JobSearchRequest request, float[] cvEmbedding, UUID tenantId) {
        // The agent decides which tools to call based on user query
        ChatResponse response = chatClient.prompt()
            .system("""
                You are a job search agent. Use the available tools to find
                job listings matching the user's criteria. Search multiple
                sources. Return results as JSON.
            """)
            .user(request.toPrompt())
            .functions(
                "searchLinkedIn",
                "searchIndeed",
                "searchGlassdoor",
                "webSearch",
                "searchConfiguredBoards"
            )
            .call()
            .chatResponse();

        List<JobListing> rawJobs = parseJobListings(response);
        List<RankedJob> ranked = rankByCosineSimilarity(rawJobs, cvEmbedding);

        // RAG: For top-N results, generate detailed match explanations
        enrichWithMatchExplanations(ranked.subList(0, Math.min(10, ranked.size())), tenantId);

        return ranked;
    }

    /**
     * RAG-powered match explanation: retrieve relevant CV chunks,
     * inject into prompt alongside job description, generate explanation.
     */
    private void enrichWithMatchExplanations(List<RankedJob> topJobs, UUID tenantId) {
        for (RankedJob job : topJobs) {
            // Retrieve CV chunks most relevant to this job description
            List<Document> cvChunks = vectorStore.similaritySearch(
                SearchRequest.builder()
                    .query(job.description())
                    .topK(5)
                    .similarityThreshold(0.7)
                    .filterExpression("tenant_id == '" + tenantId + "' && doc_type == 'cv_chunk'")
                    .build());

            String cvContext = cvChunks.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n"));

            // Generate match explanation using retrieved CV context
            MatchExplanation explanation = chatClient.prompt()
                .system("""
                    Analyse how well this candidate matches the job.
                    Based ONLY on the CV context provided, identify:
                    - matchedSkills: skills from the CV that match the job
                    - missingSkills: skills required by the job but not in the CV
                    - reasoning: 1-2 sentence explanation of the match quality
                """)
                .user("Job: " + job.description() + "\n\nCV Context:\n" + cvContext)
                .call()
                .entity(MatchExplanation.class);

            job.setMatchExplanation(explanation);
        }
    }
}
```

### 7.2 Registered Tools (Spring AI Functions)

| Function Name          | Description                                              |
|------------------------|----------------------------------------------------------|
| `searchLinkedIn`       | Calls LinkedIn Jobs API with query + location            |
| `searchIndeed`         | Calls Indeed API / scraper                               |
| `searchGlassdoor`     | Calls Glassdoor API / scraper                            |
| `webSearch`            | General web search via Tavily / SerpAPI                  |
| `searchConfiguredBoards` | Calls user-configured custom job board scrapers        |

### 7.3 Ranking Algorithm

1. Generate embedding for each job description.
2. Compute cosine similarity: `similarity(cvEmbedding, jobEmbedding)`.
3. Apply boost factors:
   - +0.05 if location matches user preference.
   - +0.03 if job is remote and user prefers remote.
   - +0.02 per matched skill keyword.
4. Sort descending by final score.

---

## 8. Data Model (subset)

```sql
CREATE TABLE job_listings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    external_id         TEXT,                    -- ID from source platform
    title               TEXT NOT NULL,
    company             TEXT NOT NULL,
    location            TEXT,
    remote              BOOLEAN DEFAULT false,
    salary_range        TEXT,
    description         TEXT NOT NULL,
    source              VARCHAR(50) NOT NULL,    -- linkedin, indeed, web, custom
    source_url          TEXT NOT NULL,
    posted_at           TIMESTAMPTZ,
    expires_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ DEFAULT now(),
    UNIQUE (tenant_id, source, external_id)
);

CREATE TABLE job_embeddings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_listing_id  UUID NOT NULL REFERENCES job_listings(id),
    tenant_id       UUID NOT NULL,
    embedding       vector(1536),
    created_at      TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE saved_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    job_listing_id  UUID NOT NULL REFERENCES job_listings(id),
    tenant_id       UUID NOT NULL,
    saved_at        TIMESTAMPTZ DEFAULT now(),
    UNIQUE (user_id, job_listing_id)
);

CREATE INDEX idx_job_embeddings_vector ON job_embeddings
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

---

## 9. Caching Strategy

| Cache Key Pattern                        | TTL     | Purpose                               |
|------------------------------------------|---------|---------------------------------------|
| `jobs:search:{userId}:{queryHash}`       | 1 hour  | Avoid re-searching same query         |
| `jobs:listing:{jobId}`                   | 24 hours| Cache individual job details          |
| `jobs:embedding:{jobId}`                 | 7 days  | Cache computed job embeddings         |

---

## 10. Configurable Job Sources

Users (or admins) can configure custom job board scrapers:

```sql
CREATE TABLE job_source_configs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    name        VARCHAR(100) NOT NULL,
    base_url    TEXT NOT NULL,
    scraper_type VARCHAR(30) NOT NULL,   -- API, RSS, HTML_SCRAPER
    auth_config JSONB,                    -- encrypted API keys
    enabled     BOOLEAN DEFAULT true,
    created_at  TIMESTAMPTZ DEFAULT now()
);
```

---

## 11. Rate Limiting & Quotas

| Source     | Rate Limit                   | Strategy                         |
|------------|------------------------------|----------------------------------|
| LinkedIn   | 100 req/day (API tier)       | Token bucket in Redis            |
| Indeed     | 50 req/hour                  | Sliding window in Redis          |
| Web Search | 1000 req/day (Tavily plan)   | Daily counter in Redis           |
| Per-user   | 20 searches/hour             | Prevent abuse                    |

---

## 12. Error Handling

| Scenario                        | Behaviour                                               |
|---------------------------------|---------------------------------------------------------|
| One source fails                | Return results from other sources; log error.           |
| All sources fail                | Return 503 with retry-after header.                     |
| AI agent timeout (30s)          | Circuit breaker opens; return cached results if available.|
| No CV embedding found           | Return 400 "Please upload a CV first".                  |

---

## 13. Testing Strategy

| Level        | Tool / Approach                                                      |
|--------------|----------------------------------------------------------------------|
| Unit         | Mock tool functions; test ranking algorithm.                          |
| Integration  | WireMock for external APIs; Testcontainers Postgres + Redis.         |
| AI Integration | Testcontainers Ollama (`mistral`); real agent tool-calling flow.   |
| AI Contract  | Snapshot tests for agent prompts; validate tool-call sequences.       |
| E2E          | Docker Compose (Ollama + WireMock stubs) for full search flow.       |
