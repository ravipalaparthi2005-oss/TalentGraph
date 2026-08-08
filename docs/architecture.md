# TalentGraph Architecture & Evidence Graph Specification

## 1. System Vision

TalentGraph provides engineering recruitment teams with transparent, auditable evidence graphs for technical candidates. Instead of relying on resume keyword matching or arbitrary AI percentage ratings, TalentGraph builds a deterministic **Evidence Graph** connecting candidate skills to four primary evidence pillars:

1. **Resume Evidence** (Parsed skills, employment history, verifiable projects)
2. **GitHub Repository Evidence** (Real commit volume, language distribution, pull request history, repo quality)
3. **Coding Assessment Evidence** (Sandboxed Judge0 execution outcomes, memory usage, test pass rates)
4. **Structured Technical Interview Evidence** (Scored rubric evaluations by technical interviewers)

---

## 2. Core Architectural Components

### Backend Layer (Java 21 + Spring Boot 3)

The backend follows a modular monolith architecture organized by domain boundaries:

```text
com.talentgraph
├── config        # Web, Security, CORS, and OpenApi configuration
├── security      # Security filters, JWT providers, user details
├── common        # Shared DTOs, Exception handlers, Health endpoints
├── auth          # Authentication & user registration handlers
├── organization  # Workspaces and recruiter membership
├── job           # Job requirements and skill weight definitions
├── candidate     # Candidate profile lifecycle management
├── evidence      # Evidence intake, parsing, and graph node mapping
├── assessment    # Coding challenge definitions and submissions
├── interview     # Technical interviewer rubrics and evaluations
├── evaluation    # Deterministic ranking and score calculation engine
└── audit         # Immutable audit logging of hiring decisions
```

### AI Boundary Principle

```text
+-----------------------+     Extract / Explain     +-------------------+
|  Deterministic Engine | <-----------------------> |    AiProvider     |
|   (Spring Boot 3)     |                           |    (OpenRouter)   |
+-----------------------+                           +-------------------+
            |                                                 |
            | Aggregates                                      | Text Processing
            v                                                 v
  +------------------+                              +-------------------+
  |  Persisted Score |                              | Structured Output |
  |   (PostgreSQL)   |                              |    (Validated)    |
  +------------------+                              +-------------------+
```

- AI providers are hidden behind `AiProvider` interface.
- OpenRouter handles natural language extraction (resumes) and natural language explanations ("Why did candidate X receive score Y?").
- Application scoring algorithms remain 100% deterministic and auditable.

---

## 3. API Conventions

- All REST endpoints are prefixed with `/api/v1/`.
- Standardized response envelope (`ApiResponse<T>`):
```json
{
  "success": true,
  "data": { ... },
  "message": "Operation completed successfully",
  "timestamp": "2026-08-08T20:00:00Z"
}
```
- Standardized error response envelope:
```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Candidate with ID 123 was not found",
    "details": []
  },
  "timestamp": "2026-08-08T20:00:00Z"
}
```
