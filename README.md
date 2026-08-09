# TalentGraph — Engineering Hiring Intelligence Platform

TalentGraph is an evidence-based hiring intelligence system designed to help engineering recruiters evaluate software engineering candidates using verifiable, deterministic evidence rather than subjective resume claims or black-box AI scores.

---

## 🏛 Architecture Overview

```text
TalentGraph Monorepo
│
├── frontend/             # React (JavaScript ES2022+), Vite, Tailwind CSS, Framer Motion, GSAP
├── backend/              # Java 21, Spring Boot 3, Spring Security, Spring Data JPA
├── docs/                 # System design, domain models (`docs/data-model.md`), and security architecture (`docs/security.md`)
├── infra/                # PostgreSQL Docker setup and initialization scripts
├── docker-compose.yml    # Local container orchestration
└── .env.example          # Environment variable template
```

---

## 🎯 Non-Negotiable Engineering Principles

1. **Deterministic Scoring**: Candidate evidence scores derive from verifiable records (GitHub commits, sandbox execution results, interviewer rubrics). AI must **NEVER** fabricate candidate scores or achievements.
2. **Strict Evidence Attribution**: If evidence does not exist for a skill, TalentGraph explicitly states `"No evidence available"`.
3. **No Mock Business Logic**: No `Math.random()` scores, fake candidate data, or hardcoded analytics pretending to work.
4. **Zero TypeScript**: Pure modern JavaScript ES2022+ on the frontend.
5. **AI Boundary**: OpenRouter LLM interactions are strictly isolated behind an application service interface (`AiProvider`). AI is used solely for extraction and explanation, never authoritative scoring.
6. **Persistence**: PostgreSQL is the single source of truth.
7. **Versioned REST API**: All frontend-backend interaction occurs via explicit `/api/v1/...` endpoints.

---

## 🚀 Quick Start (Local Development)

### 1. Environment Setup
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```

### 2. Start Database
```bash
docker-compose up -d postgres
```

### 3. Run Backend (Spring Boot 3)
```bash
cd backend
mvn spring-boot:run
```
Backend will start on `http://localhost:8080`.
Health endpoint: `http://localhost:8080/api/v1/health`

### 4. Run Frontend (React + Vite)
```bash
cd frontend
npm install
npm run dev
```
Frontend dev server will start on `http://localhost:5173`.

---

## 🧪 Testing

### Backend Unit & Integration Tests
```bash
cd backend
mvn clean test
```

### Frontend Build Verification
```bash
cd frontend
npm run build
```

---

## 📦 System Modules & Implementation Progress

- **Phase 01–04**: Multi-Tenant Workspaces, Real Authentication (JWT + Refresh Tokens + Google OAuth), Recruiter Workspaces, Job Requirements & Evaluation Criteria.
- **Phase 05**: Candidate Intake & Deterministic Resume Processing (PDF/DOCX Apache Tika parsing).
- **Phase 06**: AI Resume Intelligence (OpenRouter isolation, evidence quote validation, skill catalog mapping, audit logging).
- **Phase 07**: Real GitHub Evidence Engine (OAuth state protection, AES-256 token encryption, repository/language/commit sync, Evidence Graph integration).

---

## 📌 Domain Entities (Core Model Outline)

- **Candidate**: Software engineering applicant profile.
- **Organization**: Multi-tenant workspace.
- **Job**: Open engineering requisition with specific requirements.
- **Evidence**: Verifiable record (Resume, GitHub, Assessment, Interview).
- **GithubIdentity**: Authenticated candidate GitHub account (encrypted tokens).
- **EvidenceGraph**: Connected skill network linking evidence items to candidates.
- **HiringDecision**: Auditable record of hiring outcome.
