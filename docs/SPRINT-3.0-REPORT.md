# Sprint 3.0 — CV Upload (Final Report)

**Date:** March 1, 2026  
**Status:** ✅ COMPLETE  
**Duration:** ~1.5 hours  
**Tests:** 128 total (all passing) — 11 new tests

---

## Objectives Achieved

- ✅ CvStatus enum for CV processing states
- ✅ CvDetails JPA entity for tracking uploaded CVs
- ✅ CvDetailsRepository with custom queries
- ✅ FileStorageService interface for abstracted storage
- ✅ MinioFileStorageService implementation (S3-compatible)
- ✅ CvUploadService with validation and orchestration
- ✅ CvController REST endpoints
- ✅ Unit tests for entity and service

---

## Implementation Summary

### 1. CvStatus Enum ✅
- **File:** `src/main/java/com/jobagent/jobagent/cv/model/CvStatus.java`
- **Values:** UPLOADED, PARSING, PARSED, FAILED

### 2. CvDetails JPA Entity ✅
- **File:** `src/main/java/com/jobagent/jobagent/cv/model/CvDetails.java`
- **Fields:**
  - `user` (ManyToOne) - Link to User entity
  - `fileName`, `contentType`, `fileSize` - File metadata
  - `s3Key` - Storage location
  - `status` - CvStatus enum (default: UPLOADED)
  - `active` - Boolean (default: true)
  - `parsedJson` - JSONB for parsed CV data
  - `parsedAt`, `errorMessage` - Processing metadata
- **Tests:** 4 unit tests in `CvDetailsTest.java`

### 3. CvDetailsRepository ✅
- **File:** `src/main/java/com/jobagent/jobagent/cv/repository/CvDetailsRepository.java`
- **Methods:**
  - `findByUserIdAndActiveTrue()` - Get active CV
  - `findByUserIdOrderByCreatedAtDesc()` - CV history
  - `deactivateAllByUserId()` - Bulk deactivation

### 4. FileStorageService ✅
- **File:** `src/main/java/com/jobagent/jobagent/cv/service/FileStorageService.java`
- **Interface methods:**
  - `upload()`, `download()`, `delete()`
  - `generatePresignedDownloadUrl()`
  - `exists()`

### 5. MinioFileStorageService ✅
- **File:** `src/main/java/com/jobagent/jobagent/cv/service/MinioFileStorageService.java`
- **Features:**
  - S3-compatible file operations
  - Presigned URL generation
  - Custom `StorageException` for error handling

### 6. CvUploadService ✅
- **File:** `src/main/java/com/jobagent/jobagent/cv/service/CvUploadService.java`
- **Features:**
  - File validation (PDF, DOCX only, max 10MB)
  - S3 key generation: `cv/{tenantId}/{userId}/{uuid}.{ext}`
  - Previous CV deactivation on new upload
  - Custom `CvUploadException` for error handling
- **Tests:** 7 unit tests in `CvUploadServiceTest.java`

### 7. CvController ✅
- **File:** `src/main/java/com/jobagent/jobagent/cv/controller/CvController.java`
- **Endpoints:**

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/cv` | Upload new CV (multipart) |
| GET | `/api/v1/cv` | Get active CV summary |
| GET | `/api/v1/cv/history` | Get CV history |
| GET | `/api/v1/cv/{id}/download` | Get presigned download URL |
| DELETE | `/api/v1/cv/{id}` | Soft-delete CV |

### 8. DTOs ✅
- `CvUploadResponse` - Upload result
- `CvSummaryResponse` - CV details summary
- `CvDownloadResponse` - Presigned URL

### 9. Exception Handling ✅
- Added `CvUploadException` handler to `GlobalExceptionHandler`
- Added `StorageException` handler for MinIO errors

---

## Test Summary

| Test Class | Tests | Status |
|------------|-------|--------|
| CvDetails Entity Tests | 4 | ✅ NEW |
| CvUploadService Tests | 7 | ✅ NEW |
| *All previous tests* | 117 | ✅ |
| **Total** | **128** | **✅** |

---

## API Examples

```bash
# Upload CV (requires JWT)
curl -X POST http://localhost:8080/api/v1/cv \
  -H "Authorization: Bearer <JWT>" \
  -F "file=@resume.pdf"

# Response:
# {
#   "id": "uuid",
#   "fileName": "resume.pdf",
#   "contentType": "application/pdf",
#   "fileSize": 102400,
#   "status": "UPLOADED",
#   "createdAt": "2026-03-01T16:00:00Z"
# }

# Get active CV
curl http://localhost:8080/api/v1/cv \
  -H "Authorization: Bearer <JWT>"

# Get download URL
curl http://localhost:8080/api/v1/cv/{id}/download \
  -H "Authorization: Bearer <JWT>"

# Response:
# {
#   "downloadUrl": "https://minio.../cv/...",
#   "expiresInMinutes": 15
# }
```

---

## Files Created

| Category | Files |
|----------|-------|
| **Model** | CvStatus.java, CvDetails.java |
| **Repository** | CvDetailsRepository.java |
| **Service** | FileStorageService.java, MinioFileStorageService.java, CvUploadService.java |
| **Controller** | CvController.java |
| **DTO** | CvUploadResponse.java, CvSummaryResponse.java, CvDownloadResponse.java |
| **Tests** | CvDetailsTest.java, CvUploadServiceTest.java |
| **Docs** | SPRINT-3.0-PLAN.md |

---

## Next Steps (Sprint 4)

Sprint 4 will focus on **CV AI Parsing** with Spring AI + Ollama:
1. CV Text Extraction (Apache Tika)
2. CvParserAgent (Spring AI ChatClient)
3. CV Embedding Service
4. CV RAG Ingestion Service
5. CV Processing Orchestrator

---

## Commit Message

```
feat(cv): implement CV upload with MinIO storage (Sprint 3.0)

- CvDetails entity with status tracking
- CvDetailsRepository with custom queries
- FileStorageService abstraction
- MinioFileStorageService implementation
- CvUploadService with validation (PDF/DOCX, max 10MB)
- CvController REST endpoints
- 128 tests passing (11 new)

Co-authored-by: GitHub Copilot
```
