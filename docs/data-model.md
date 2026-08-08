# TalentGraph Data Model & Evidence Graph Architecture

## Overview
TalentGraph uses a **multi-tenant PostgreSQL database** managed by versioned Flyway migrations (`V1__initial_schema.sql`). All entities strictly enforce multi-tenant isolation via `organization_id` foreign key relationships.

The core innovation of TalentGraph is the **Evidence Graph**, which decouples raw candidate proof (resumes, GitHub repos, code submission test results, interview evaluations) from subjective scoring. Scores are deterministically calculated based on verified evidence nodes linked to specific normalized skills and job requirements.

---

## Entity Relationship Diagram

```mermaid
erDiagram
    ORGANIZATION ||--o{ USER_MEMBERSHIP : "has members"
    USER ||--o{ USER_MEMBERSHIP : "belongs to"
    ORGANIZATION ||--o{ JOB : "posts"
    ORGANIZATION ||--o{ CANDIDATE : "manages"
    JOB ||--o{ JOB_REQUIREMENT : "defines"
    SKILL ||--o{ JOB_REQUIREMENT : "maps to"
    SKILL ||--o{ EVIDENCE_SKILL : "supported by"
    CANDIDATE ||--o{ APPLICATION : "submits"
    JOB ||--o{ APPLICATION : "receives"
    CANDIDATE ||--o{ EVIDENCE_SOURCE : "provides"
    EVIDENCE_SOURCE ||--o{ EVIDENCE : "contains"
    EVIDENCE ||--o{ EVIDENCE_SKILL : "extracts"
    
    ORGANIZATION ||--o{ ASSESSMENT : "configures"
    ASSESSMENT ||--o{ ASSESSMENT_QUESTION : "contains"
    ASSESSMENT ||--o{ ASSESSMENT_ATTEMPT : "triggers"
    CANDIDATE ||--o{ ASSESSMENT_ATTEMPT : "undergoes"
    ASSESSMENT_ATTEMPT ||--o{ CODE_SUBMISSION : "records"
    ASSESSMENT_QUESTION ||--o{ CODE_SUBMISSION : "evaluates"
    
    APPLICATION ||--o{ INTERVIEW : "schedules"
    INTERVIEW ||--o{ INTERVIEW_EVALUATION : "produces"
    USER ||--o{ INTERVIEW_EVALUATION : "conducts"
    
    JOB ||--o{ EVALUATION_CRITERIA : "establishes"
    APPLICATION ||--o{ CANDIDATE_EVALUATION : "scores"
    EVALUATION_CRITERIA ||--o{ CANDIDATE_EVALUATION : "guides"
    APPLICATION ||--o{ HIRING_DECISION : "concludes"
    ORGANIZATION ||--o{ AUDIT_EVENT : "logs"

    ORGANIZATION {
        uuid id PK
        string name
        string slug UK
        timestamp created_at
    }

    USER {
        uuid id PK
        string email UK
        string first_name
        string last_name
        string password_hash
        boolean is_active
    }

    CANDIDATE {
        uuid id PK
        uuid organization_id FK
        string email UK
        string first_name
        string last_name
        string linkedin_url
        string github_username
    }

    JOB {
        uuid id PK
        uuid organization_id FK
        string title
        string department
        string status
        string employment_type
    }

    SKILL {
        uuid id PK
        string name
        string normalized_name UK
        string category
    }

    EVIDENCE_SOURCE {
        uuid id PK
        uuid candidate_id FK
        string source_type
        string external_reference
        string source_url
        jsonb metadata_json
    }

    EVIDENCE {
        uuid id PK
        uuid candidate_id FK
        uuid evidence_source_id FK
        string title
        string evidence_type
        string relationship_type
        numeric confidence
        string observed_value
    }

    EVIDENCE_SKILL {
        uuid evidence_id PK,FK
        uuid skill_id PK,FK
        numeric weight
    }

    ASSESSMENT {
        uuid id PK
        uuid organization_id FK
        uuid job_id FK
        string name
        int pass_score
    }

    CODE_SUBMISSION {
        uuid id PK
        uuid assessment_attempt_id FK
        uuid assessment_question_id FK
        string language
        string status
        int test_cases_passed
        int test_cases_total
    }

    INTERVIEW_EVALUATION {
        uuid id PK
        uuid interview_id FK
        uuid interviewer_id FK
        string recommendation
        text overall_notes
    }

    CANDIDATE_EVALUATION {
        uuid id PK
        uuid candidate_id FK
        uuid application_id FK
        uuid criterion_id FK
        int evidence_count
        numeric computed_value
    }

    AUDIT_EVENT {
        uuid id PK
        uuid organization_id FK
        uuid actor_user_id FK
        string entity_type
        uuid entity_id
        string action
        jsonb metadata_json
    }
```

---

## Core Domain Modules

### 1. Multi-Tenant Core (`com.talentgraph.organization`, `com.talentgraph.auth`)
- **`Organization`**: Multi-tenant workspace identified by unique `slug`.
- **`User`**: Account identity across organizations.
- **`OrganizationMember`**: Connects users to organizations with explicit roles (`ADMIN`, `RECRUITER`, `INTERVIEWER`, `HOLDER`).

### 2. Job & Candidate Pipeline (`com.talentgraph.job`, `com.talentgraph.candidate`)
- **`Job`**: Requisition posted by an organization (`DRAFT`, `OPEN`, `PAUSED`, `CLOSED`).
- **`JobRequirement`**: Specific skill requirements mapped to `Skill` with `RequirementType` (`MUST_HAVE`, `NICE_TO_HAVE`) and `Importance` (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`).
- **`Candidate`**: Multi-tenant applicant profile linked to an organization.
- **`Application`**: Application lifecycle tracking candidate against a job (`APPLIED`, `SCREENING`, `ASSESSMENT_PENDING`, `INTERVIEW_SCHEDULED`, `OFFERED`, `HIRED`, `REJECTED`).

### 3. Evidence Graph Data Engine (`com.talentgraph.evidence`)
- **`Skill`**: Standardized skill catalog (`normalized_name`, `category`).
- **`EvidenceSource`**: Provenance container (`RESUME`, `GITHUB_REPO`, `ASSESSMENT_RUN`, `INTERVIEW_FEEDBACK`).
- **`Evidence`**: Granular evidence claim (e.g., repository commit history, parsed work history, test execution metrics).
- **`EvidenceSkill`**: Composite join table mapping `Evidence` to `Skill` with verifiable `weight` (0.0 to 1.0).

### 4. Code Execution & Assessments (`com.talentgraph.assessment`)
- **`Assessment`**: Technical evaluation suite.
- **`AssessmentQuestion`**: Code challenge or problem with test cases and points.
- **`AssessmentAttempt`**: Candidate attempt instance (`IN_PROGRESS`, `SUBMITTED`, `EXPIRED`, `EVALUATED`).
- **`CodeSubmission`**: Automated execution record with language, source code, status (`PASSED`, `FAILED`, `TIME_LIMIT_EXCEEDED`, `COMPILATION_ERROR`), memory, and test case pass rates.

### 5. Structured Interviews & Evaluations (`com.talentgraph.interview`, `com.talentgraph.evaluation`)
- **`Interview`**: Scheduled interview event.
- **`InterviewEvaluation`**: Standardized evaluation with recommendation (`STRONG_YES`, `YES`, `NEUTRAL`, `NO`, `STRONG_NO`).
- **`EvaluationCriterion`**: Weighted criteria established per job.
- **`CandidateEvaluation`**: Deterministic score computation snapshot referencing evidence counts and calculation versions.
- **`HiringDecision`**: Final decision audit record (`OFFER_EXTENDED`, `HIRED`, `REJECTED`, `WITHDRAWN`).

### 6. Audit & Governance (`com.talentgraph.audit`)
- **`AuditEvent`**: Append-only system audit log capturing actor, entity, action, and JSON payload metadata for compliance and AI decision transparency.
