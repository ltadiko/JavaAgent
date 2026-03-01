# Sprint 4.0 — CV AI Parsing (Final Report)

**Date:** March 1, 2026  
**Status:** ✅ COMPLETE  
**Duration:** ~1.5 hours  
**Tests:** 135+ total (7+ new tests)

---

## Objectives Achieved

- ✅ CvTextExtractor using Apache Tika for PDF/DOCX text extraction
- ✅ CvParsedData structured DTO for parsed CV information
- ✅ CvParserAgent using Spring AI ChatClient with Ollama
- ✅ CvProcessingService to orchestrate the parsing pipeline
- ✅ Async processing with thread pool configuration
- ✅ Circuit breaker and retry patterns for AI resilience
- ✅ Integration with CvUploadService for automatic processing

---

## Implementation Summary

### 1. CvTextExtractor ✅
- **File:** `src/main/java/com/jobagent/jobagent/cv/service/CvTextExtractor.java`
- **Features:**
  - Apache Tika integration for text extraction
  - Supports PDF and DOCX files
  - Max 100KB text extraction limit
  - Custom `CvParsingException` for error handling
- **Tests:** 3 unit tests in `CvTextExtractorTest.java`

### 2. CvParsedData DTO ✅
- **File:** `src/main/java/com/jobagent/jobagent/cv/dto/CvParsedData.java`
- **Fields:**
  - `fullName`, `email`, `phone`, `currentTitle`, `summary`
  - `skills` - List of skills
  - `experience` - List of ExperienceEntry records
  - `education` - List of EducationEntry records
  - `languages`, `certifications`
- **Nested records:**
  - `ExperienceEntry(company, title, location, startDate, endDate, description)`
  - `EducationEntry(institution, degree, field, startDate, endDate)`

### 3. CvParserAgent ✅
- **File:** `src/main/java/com/jobagent/jobagent/cv/service/CvParserAgent.java`
- **Features:**
  - Spring AI ChatClient integration
  - Uses prompt template from `prompts/cv-parse.st`
  - `@CircuitBreaker` and `@Retry` for resilience
  - JSON extraction from AI response (handles markdown code blocks)
  - Fallback returns empty CvParsedData on failure
  - Text truncation for large CVs (max 15K chars)
- **Tests:** 4 unit tests in `CvParserAgentTest.java`

### 4. CvProcessingService ✅
- **File:** `src/main/java/com/jobagent/jobagent/cv/service/CvProcessingService.java`
- **Features:**
  - Orchestrates: Download → Extract → Parse → Update
  - Updates CV status: UPLOADED → PARSING → PARSED/FAILED
  - Stores parsed JSON in CvDetails entity
  - Async processing via `processAsync()`
  - Reprocess capability for AI model updates
  - Error handling with status and message storage

### 5. CvProcessingConfig ✅
- **File:** `src/main/java/com/jobagent/jobagent/cv/config/CvProcessingConfig.java`
- **Features:**
  - `@EnableAsync` configuration
  - Thread pool: 2-5 threads, queue capacity 100
  - Graceful shutdown with 60s timeout

### 6. Prompt Template ✅
- **File:** `src/main/resources/prompts/cv-parse.st`
- **Features:**
  - Structured JSON output format
  - Comprehensive extraction rules
  - Handles missing fields gracefully

### 7. Integration Updates ✅
- CvUploadService triggers async processing after upload
- GlobalExceptionHandler handles CvParsingException
- @Lazy injection to prevent circular dependencies

---

## Processing Pipeline

```
┌──────────────────────────────────────────────────────────────┐
│                    CV Processing Pipeline                     │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  1. Upload CV → CvUploadService                              │
│     ├── Validate (PDF/DOCX, max 10MB)                        │
│     ├── Store in MinIO                                       │
│     ├── Save CvDetails (status=UPLOADED)                     │
│     └── Trigger processAsync()                               │
│                                                               │
│  2. Process CV → CvProcessingService (async)                 │
│     ├── Update status → PARSING                              │
│     ├── Download from MinIO                                  │
│     ├── Extract text (Tika)                                  │
│     ├── Parse with AI (Ollama)                              │
│     ├── Store parsed JSON                                    │
│     └── Update status → PARSED (or FAILED)                   │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

---

## AI Integration

```yaml
# Ollama Configuration (application-local.yml)
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: llama3.2
```

---

## Test Summary

| Test Class | Tests | Status |
|------------|-------|--------|
| CvTextExtractor Tests | 3 | ✅ NEW |
| CvParserAgent Tests | 4 | ✅ NEW |
| *All previous tests* | 128 | ✅ |
| **Total** | **135+** | **✅** |

---

## Files Created/Modified

| Category | Files |
|----------|-------|
| **Service** | CvTextExtractor.java, CvParserAgent.java, CvProcessingService.java |
| **Config** | CvProcessingConfig.java |
| **DTO** | CvParsedData.java |
| **Prompt** | cv-parse.st (updated) |
| **Tests** | CvTextExtractorTest.java, CvParserAgentTest.java |
| **Modified** | CvUploadService.java, GlobalExceptionHandler.java |
| **Docs** | SPRINT-4.0-PLAN.md |

---

## Next Steps (Sprint 5)

Sprint 5 will focus on **Job Search** functionality:
1. JobListing entity
2. Job search with external APIs
3. Job matching based on CV skills
4. RAG integration for semantic search

---

## Commit Message

```
feat(cv): implement CV AI parsing with Spring AI + Ollama (Sprint 4.0)

- CvTextExtractor with Apache Tika for PDF/DOCX
- CvParsedData structured DTO
- CvParserAgent with Spring AI ChatClient
- CvProcessingService with async pipeline
- Circuit breaker and retry resilience patterns
- Updated cv-parse.st prompt template
- 135+ tests passing

Co-authored-by: GitHub Copilot
```
