# Sprint 4.0 — CV AI Parsing (Spring AI + Ollama)

**Date:** March 1, 2026  
**Status:** 🔵 IN PROGRESS  
**Goal:** Parse uploaded CVs using AI to extract structured data

---

## Sprint 4 Tasks

### Task 4.1: CV Text Extraction (Apache Tika) ⏱️ ~15 min
- Extract text from PDF/DOCX files

### Task 4.2: CvParsedData DTO ⏱️ ~10 min
- Structured data model for parsed CV

### Task 4.3: CvParserAgent (Spring AI) ⏱️ ~25 min
- AI agent to parse CV text into structured data

### Task 4.4: CV Processing Service ⏱️ ~20 min
- Orchestrate the parsing pipeline

### Task 4.5: Async Processing with Events ⏱️ ~15 min
- Background processing after upload

### Task 4.6: Tests & Verification ⏱️ ~25 min
- Unit and integration tests

---

## Execution Order

```
Task 4.1 (Tika Extractor) → Task 4.2 (DTO)
     ↓
Task 4.3 (AI Parser) → Task 4.4 (Processing Service)
     ↓
Task 4.5 (Async Events) → Task 4.6 (Tests)
     ↓
Final verification → Commit
```

---

## Estimated Duration: ~2 hours
