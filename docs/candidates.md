# TalentGraph — Candidate Management Architecture & Technical Specification

## 1. Executive Overview

Phase 05 establishes the real candidate intake & retention model for **TalentGraph**.
Candidates represent real talent entities attached to multi-tenant organizations.
Every candidate, application, and document is strictly isolated by organization ID and enforced by Spring Security role checks (`OrganizationAuthorizationService`).

---

## 2. Retention Strategy & Soft Archiving

To preserve historical applications, evidence graph nodes, assessment submissions, and immutable audit logs:
- Candidates are **never deleted from PostgreSQL** via `CASCADE DELETE`.
- Instead, candidate records feature an `is_active` boolean field.
- When a candidate is deleted via `DELETE /api/v1/candidates/{candidateId}`, `is_active` is updated to `false`.
- Inactive candidates are omitted from recruiter active searches (`getCandidates`) while retaining all historical evidence nodes and application records for compliance and auditability.

---

## 3. Database Schema

### `candidates` Table
```sql
CREATE TABLE candidates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    location VARCHAR(255),
    linkedin_url VARCHAR(500),
    github_username VARCHAR(100),
    portfolio_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_candidate_org_email UNIQUE (organization_id, email)
);
```

### `applications` Table
```sql
CREATE TABLE applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL REFERENCES candidates(id) ON DELETE RESTRICT,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE RESTRICT,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    source VARCHAR(50) NOT NULL DEFAULT 'RECRUITER',
    applied_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_application_candidate_job UNIQUE (candidate_id, job_id)
);
```

---

## 4. Multi-Tenant Authorization Checks

All candidate endpoints execute multi-tenant authorization:
1. `POST /api/v1/candidates` — Requires `RECRUITER` role in target organization.
2. `GET /api/v1/candidates` — Requires `INTERVIEWER` role in target organization. Returns paginated candidates.
3. `GET /api/v1/candidates/{id}` — Requires `INTERVIEWER` role in candidate's organization. Cross-organization access returns `403 Forbidden`.
4. `POST /api/v1/jobs/{jobId}/applications` — Verifies `Candidate.organization_id == Job.organization_id`. Cross-organization applications are rejected with `409 Conflict`.
