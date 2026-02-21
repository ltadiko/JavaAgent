# JobAgent — Global AI Job Application Platform

One-line: An extensible, multi-tenant Spring Boot application that helps job seekers discover job matches, generates motivation letters, and automates applications — built for cloud deployment and local development (Docker / Rancher / Testcontainers).

Table of contents
- About
- Quickstart (local)
- Architecture (high level)
- Key concepts (multi-tenancy, RAG, encryption, events)
- Project layout
- Development workflows (build, tests, containers)
- Contributing & sprints
- Contacts

About
---
JobAgent is an AI-assisted, multi-tenant job application platform implemented with Java 25 and Spring Boot 4. It is designed to be scalable, reliable, testable locally (docker / Rancher Desktop) and deployable to cloud providers. The platform integrates local/open-source LLMs (Ollama) and a vector store (pgvector) for RAG (retrieval-augmented generation).

Quickstart — Local (developer)
-------------------------------
Prerequisites
- Java 25 (JDK 25)
- Maven 3.9+
- Docker / Rancher Desktop (or Docker Desktop)
- Node 18+ / npm (for frontend)

Fast-build (compile only)
```
# from repository root
make build
```
Start local infra (Postgres, MinIO, Kafka, Ollama, Redis/Valkey)
```
# try auto-detection (script uses Rancher socket or DOCKER_HOST)
make up-local
# or, if you discovered your docker socket path
./scripts/use-rancher-docker.sh /path/to/docker.sock -- docker compose up -d postgres
```
Start backend (local profile)
```
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
Start frontend (in a separate terminal)
```
cd jobagent-ui
npm install
npm run dev
```
Notes
- First-time only (Ollama): pull models inside the Ollama container to avoid on-demand downloads:
```
docker exec jobagent-ollama ollama pull mistral
docker exec jobagent-ollama ollama pull nomic-embed-text
```

Architecture (high level)
-------------------------
- Backend: Spring Boot 4 (Java 25). Modules: `auth`, `cv`, `jobsearch`, `motivation`, `apply`, `application`, and `common` (cross-cutting concerns).
- Database: PostgreSQL 17 + pgvector. Flyway migrations under `src/main/resources/db/migration` (V1..V8). V8 adds RLS policies.
- RAG / Vector DB: `vector_store` table + `pgvector` extension; Spring AI + PgVector integration.
- AI: Ollama for local dev; OpenAI configured for production. Embedding dimensions configurable per profile.
- Multi-tenancy: Tenant enforced at three levels — application (TenantContextFilter), DB session var + RLS, RAG query filter.
- Events: Kafka topics for async communication (job events, CV analyzed, applications events).
- Frontend: Vue 3 + Vite + PrimeVue (scaffolded in `jobagent-ui`).

Key concepts
------------
- Multi-tenant isolation — tenant_id must be set in JWT and is enforced via TenantContextFilter and RLS.
- RAG — vector search over tenant-scoped documents. The application enforces tenant isolation at query time.
- PII encryption — `EncryptedStringConverter` (AES-256-GCM) is used for PII columns where configured.
- Flyway migrations — database schema is versioned in `db/migration`.

Project layout
--------------
See `docs/SPRINT-0-REPORT.md` for detailed layout. Key files:
- `pom.xml` — Maven build
- `docker-compose.yml` — local infrastructure
- `src/main/java` — backend source
- `src/main/resources` — configs, Flyway migrations, prompts
- `jobagent-ui/` — frontend SPA
- `scripts/use-rancher-docker.sh` — helper to point to Rancher Desktop socket
- `Makefile` — convenient targets (build, build-full, up-local, down-local, test)

Development workflows
---------------------
- Build (fast): `make build` (skips tests)
- Full build: `make build-full` (starts local infra then runs `mvn clean install`)
- Start infra: `make up-local` (uses the socket helper)
- Stop infra: `make down-local`

Tests
-----
- Unit tests: JUnit + Spring Boot Test
- Integration tests: use Testcontainers (Postgres with pgvector image) where applicable to keep tests hermetic
- ArchUnit tests enforce module boundaries (note: ArchUnit may need compatibility handling when building with Java 25; see HELP.md)

Contributing & sprints
----------------------
The project follows small sprints and one-story-at-a-time implementation. Sprint 1 focuses on the `auth` use case (register/login). See `docs/SPRINT-0-REPORT.md` and the `docs/SPRINT-BACKLOG.md` for the backlog and sprint tasks.

Contact
-------
Repo owner: `ltadiko` (Lakshmaah Tatikonda) — email: tlaxman88@gmail.com

License
-------
Check the `pom.xml` and `docs/architecture/10-DESIGN-AUDIT-LICENSING.md` for libraries and license notes. The project sources are unspecified in this repo — add your chosen open-source license file at the project root (e.g., `LICENSE`).

---
For more detailed developer help, open `HELP.md`.
