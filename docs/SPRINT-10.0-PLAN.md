# Sprint 10.0 — Production Hardening Plan

**Date:** March 1, 2026  
**Sprint Goal:** Production readiness — health checks, OpenAPI docs, rate limiting, security tightening

---

## Tasks

### Task 10.1: OpenAPI / Swagger Documentation
- Add springdoc-openapi dependency
- Annotate controllers with @Tag, @Operation
- Serve at /swagger-ui.html

### Task 10.2: Custom Health Indicators
- PostgreSQL connection health
- MinIO storage health
- Ollama AI service health

### Task 10.3: Rate Limiting (Resilience4j)
- Auth endpoints: 10 req/min
- AI endpoints: 5 req/min
- Search endpoints: 30 req/min

### Task 10.4: Application Info Endpoint
- Git info, build info in actuator/info

### Task 10.5: Security Tightening
- Swagger UI public access
- Rate limit error handling

### Task 10.6: Unit Tests

---

## Success Criteria
- [ ] Swagger UI accessible at /swagger-ui.html
- [ ] Health endpoints show component status
- [ ] Rate limiting configured
- [ ] Build passes
