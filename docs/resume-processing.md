# TalentGraph — Resume Processing & Deterministic Evidence Pipeline

## 1. Overview & Security Architecture

Phase 05 introduces deterministic, zero-AI raw text extraction and evidence graph generation for uploaded candidate resumes.

- **File Storage**: Abstracted via `FileStorageService`. Files are saved under `./storage/resumes` using randomly generated UUID storage keys. Path traversal inputs (`..`, `/`, `\`) are strictly sanitized.
- **File Validation**:
  - **Size Limit**: Enforced max 10MB (`MAX_RESUME_SIZE_MB`).
  - **MIME & Extension**: Accepts `application/pdf` (`.pdf`) and `application/vnd.openxmlformats-officedocument.wordprocessingml.document` (`.docx`).
  - **Magic Bytes Validation**: Verifies magic byte headers (`%PDF-` for PDF, `PK\x03\x04` zip header for DOCX) to prevent malicious file extension spoofing.
  - **SHA-256 Hashing**: Calculates raw byte SHA-256 hash for document deduplication and integrity tracking.

---

## 2. Text Extraction Engine

Extracted text is processed without external LLM calls or AI inference:
- **PDF Extraction**: Uses **Apache PDFBox 3.0.2** (`PDFTextStripper`). Scanned PDFs without selectable text raise an exception.
- **DOCX Extraction**: Uses **Apache POI OOXML 5.2.5** (`XWPFWordExtractor`).
- Raw extracted plain text is stored in the `resume_parsed_contents` table.

---

## 3. Deterministic Evidence Graph Generation

`ResumeEvidenceService` transforms extracted text into evidence nodes:
1. Creates an `EvidenceSource` record:
   - `source_type`: `RESUME`
   - `external_reference`: Original filename
   - `source_url`: `/api/v1/candidates/{id}/documents/{docId}`
2. Performs token and phrase boundary matching (`\b<skill>\b`) against existing skills in the catalog:
   - Prevents matching single-character substrings inside words (e.g., "C" inside "cloud").
   - Matches multi-word skill phrases like "Spring Boot" across whitespace boundaries.
3. For each matched catalog skill, creates an `Evidence` node (`evidence_type`: `SKILL_MENTION`, `confidence`: `1.00`) linked to `EvidenceSkill` (`relationship_type`: `DEMONSTRATES`).
4. **No Unsubstantiated Skills**: Skills not explicitly listed in the catalog or missing from text are never invented or hallucinated.

---

## 4. Audit Event Telemetry

Every step of the ingestion pipeline logs immutable audit events to `audit_events`:
- `RESUME_UPLOADED`
- `RESUME_PROCESSING_STARTED`
- `RESUME_PROCESSED`
- `RESUME_PROCESSING_FAILED`
