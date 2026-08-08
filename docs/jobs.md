# TalentGraph — Job Management Architecture & Technical Specification

## 1. Executive Overview

Phase 04 establishes the core recruiter domain model for **TalentGraph**. It allows recruiters to define job opportunities, state required skills, establish importance levels, construct weighted evaluation criteria rubrics, and track complete lifecycle changes via immutable audit event logs.

Every job displayed in TalentGraph is sourced directly from PostgreSQL — zero mock arrays or simulated state.

---

## 2. Job Lifecycle State Machine

```
              ┌──────────────┐
              │    DRAFT     │
              └──────┬───────┘
                     │ (Publish Validation)
                     ▼
  ┌──────────┐ ◄────► ┌──────────────┐
  │  PAUSED  │        │     OPEN     │
  └────┬─────┘        └──────┬───────┘
       │                     │
       └──────────┬──────────┘
                  ▼
          ┌──────────────┐
          │    CLOSED    │
          └──────────────┘
```

### Valid Transitions
1. `DRAFT -> OPEN`: Requires passing **Publish Validation** (see Section 5).
2. `DRAFT -> CLOSED`: Archives draft job.
3. `OPEN -> PAUSED`: Suspends incoming candidates/assessments.
4. `PAUSED -> OPEN`: Resumes active recruiting.
5. `OPEN -> CLOSED`: Closes job permanently.
6. `PAUSED -> CLOSED`: Closes job permanently.

### Forbidden Transitions
- `CLOSED -> OPEN`: Throws `409 Conflict` (`Invalid job status transition from CLOSED to OPEN`). Reopening closed jobs is disallowed; a new job must be created.
- `CLOSED -> DRAFT`: Throws `409 Conflict`.
- Updating job text details when status is `CLOSED` throws `409 Conflict`.

---

## 3. Skill Catalog & Deterministic Normalization

Skills are managed globally in the `skills` table to ensure consistent cross-candidate evidence matching.

### Deterministic Normalization Formula
```java
String normalizedName = name.trim().toLowerCase().replaceAll("\\s+", " ");
```

### Duplicate Prevention
Creating a skill with a normalized name that already exists in PostgreSQL throws `409 Conflict` (`DuplicateResourceException`).

---

## 4. Job Requirements & Evaluation Criteria

### Job Requirements
Represent required or preferred qualification criteria (`REQUIRED`, `PREFERRED`) coupled with importance metrics (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`) and optional skill catalog association.

### Evaluation Criteria Rubric
Defines weighted evaluation dimensions (e.g. System Design: 0.60, Code Quality: 0.40).
- `0.0 < weight <= 1.0` enforced per criterion.
- Total sum of weights for a job must equal `1.00` (within `0.001` decimal tolerance) before the job can be published (`DRAFT -> OPEN`).

---

## 5. Publish Validation (`DRAFT -> OPEN`)

Attempting to transition a job from `DRAFT` to `OPEN` triggers strict server-side validation:
1. `title`, `description`, `employmentType` must be present.
2. At least **1 `JobRequirement`** must be associated with the job.
3. At least **1 requirement** must be designated as `REQUIRED`.
4. If `EvaluationCriterion` records exist, their weights **must sum to 1.00** (`Math.abs(totalWeight - 1.0) < 0.001`).

If validation fails, the server responds with `400 Bad Request` or `422 Unprocessable Entity` containing explicit error details.

---

## 6. Multi-Tenant Authorization Rules

All job management endpoints validate user membership via `OrganizationAuthorizationService`:
- `OWNER` / `ADMIN`: Full access to create, edit, publish, pause, close, delete jobs, requirements, and criteria.
- `RECRUITER`: Full operational access to manage jobs for their assigned organization.
- `HIRING_MANAGER`: Read-only access to view jobs, requirements, criteria, and audit logs.
- `INTERVIEWER`: Read-only access to assigned jobs; blocked from creating, editing, or publishing jobs (returns `403 Forbidden`).

Client-supplied `organizationId` is never trusted. Ownership is strictly verified against the authenticated user's persisted `OrganizationMember` records.

---

## 7. Audit Event Logging

All key actions emit an immutable `AuditEvent` record stored in PostgreSQL:
- `JOB_CREATED`: Triggered on job draft creation.
- `JOB_UPDATED`: Triggered on job detail updates.
- `JOB_PUBLISHED`: Triggered on `DRAFT -> OPEN` transition.
- `JOB_PAUSED`: Triggered on `OPEN -> PAUSED` transition.
- `JOB_CLOSED`: Triggered on transition to `CLOSED`.
- `JOB_ARCHIVED`: Triggered on job soft deletion.

Audit logs are retrievable via `GET /api/v1/jobs/{jobId}/activity`.

---

## 8. REST API Endpoints Summary

| Method | Endpoint | Description | Auth Role |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/jobs` | Create draft job | RECRUITER+ |
| `GET` | `/api/v1/jobs` | List organization jobs (paginated, filtered, sorted) | INTERVIEWER+ |
| `GET` | `/api/v1/jobs/{jobId}` | Get job details | INTERVIEWER+ |
| `PUT` | `/api/v1/jobs/{jobId}` | Update job details | RECRUITER+ |
| `PATCH` | `/api/v1/jobs/{jobId}/status` | Transition job status | RECRUITER+ |
| `DELETE` | `/api/v1/jobs/{jobId}` | Soft delete/archive job | RECRUITER+ |
| `GET` | `/api/v1/jobs/{jobId}/requirements` | List job requirements | INTERVIEWER+ |
| `POST` | `/api/v1/jobs/{jobId}/requirements` | Add requirement | RECRUITER+ |
| `PUT` | `/api/v1/jobs/{jobId}/requirements/{id}` | Edit requirement | RECRUITER+ |
| `DELETE` | `/api/v1/jobs/{jobId}/requirements/{id}` | Delete requirement | RECRUITER+ |
| `GET` | `/api/v1/jobs/{jobId}/criteria` | List evaluation criteria | INTERVIEWER+ |
| `POST` | `/api/v1/jobs/{jobId}/criteria` | Add evaluation criterion | RECRUITER+ |
| `PUT` | `/api/v1/jobs/{jobId}/criteria/{id}` | Edit evaluation criterion | RECRUITER+ |
| `DELETE` | `/api/v1/jobs/{jobId}/criteria/{id}` | Delete evaluation criterion | RECRUITER+ |
| `GET` | `/api/v1/jobs/{jobId}/activity` | Fetch job audit trail | INTERVIEWER+ |
| `GET` | `/api/v1/skills` | List normalized skill catalog | Authenticated |
| `POST` | `/api/v1/skills` | Register new normalized skill | Authenticated |
