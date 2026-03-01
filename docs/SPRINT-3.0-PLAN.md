# Sprint 3.0 — CV Upload (File Storage)

**Date:** March 1, 2026  
**Status:** 🔵 IN PROGRESS  
**Goal:** Implement CV file upload with MinIO storage (no AI parsing yet)

---

## Sprint 3 Tasks

### Task 3.1: CvStatus Enum ⏱️ ~5 min
- Create enum for CV processing status

### Task 3.2: CvDetails JPA Entity ⏱️ ~15 min
- Entity for tracking CV uploads
- Fields: user, fileName, contentType, fileSize, s3Key, status, active

### Task 3.3: CvDetails Repository ⏱️ ~10 min
- JPA repository with custom queries

### Task 3.4: FileStorageService Interface ⏱️ ~10 min
- Abstract storage operations

### Task 3.5: MinioFileStorageService ⏱️ ~20 min
- MinIO/S3 implementation

### Task 3.6: CvUploadService ⏱️ ~20 min
- Upload orchestration with validation

### Task 3.7: CvController ⏱️ ~20 min
- REST endpoints for CV operations

### Task 3.8: Tests & Verification ⏱️ ~30 min
- Unit and integration tests

---

## Execution Order

```
Task 3.1 (CvStatus) → Task 3.2 (CvDetails Entity) → Task 3.3 (Repository)
     ↓
Task 3.4 (FileStorageService) → Task 3.5 (MinioFileStorageService)
     ↓
Task 3.6 (CvUploadService) → Task 3.7 (CvController)
     ↓
Task 3.8 (Tests) → Final verification → Commit
```

---

## Estimated Duration: ~2.5 hours
