# TalentGraph Phase 07 — Real GitHub Evidence Engine

## 1. System Vision

The **GitHub Evidence Engine** collects verifiable engineering evidence directly from candidate GitHub accounts and integrates it into TalentGraph's multi-tenant Evidence Graph.

### Core Architectural Principle

> **GitHub activity is raw, observed evidence — not proof of skill by itself.**

For example:
- **Observed Fact**: Candidate authored 42 commits and repository reports 125,000 bytes of Java code.
- **Evidence Node**: `EvidenceType.REPOSITORY` linked to existing `Java` catalog skill with `DEMONSTRATES` relationship.
- **Invariant**: The system **never** creates synthetic ratings such as "Candidate has 95% Java proficiency" or "Candidate is an expert developer."

---

## 2. Security & Privacy Model

### OAuth Access Token Security
- GitHub OAuth access tokens are encrypted at rest using **AES-256-GCM** via `GithubTokenEncryptionService`.
- Raw access tokens are **NEVER** returned in any REST API response or logged.
- The `GITHUB_CLIENT_SECRET` exists only in server environment variables — never exposed to Vite/React.

### OAuth State CSRF Protection
- OAuth state tokens (`GithubOAuthStateService`) are HMAC-SHA256 signed.
- Payload includes `candidateId`, initiating `actorUserId`, timestamp, and a cryptographic nonce.
- Prevents CSRF attacks and candidate account substitution.

### Candidate Identity Uniqueness
- One GitHub account cannot be linked to multiple candidate profiles (`uk_gh_user_id`).
- Mandatory recruiter organization authorization (`RECRUITER` role) for connect/sync/disconnect operations.

---

## 3. Database Schema (`V6__github_evidence.sql`)

| Table Name | Purpose | Primary Keys & Constraints |
|------------|---------|----------------------------|
| `github_identities` | Linked candidate GitHub OAuth accounts | `id`, `github_user_id` UNIQUE, `candidate_id` FK |
| `github_sync_runs` | Sync audit execution log | `id`, `github_identity_id` FK, `status` |
| `github_repositories` | Synchronized repository metadata | `id`, `github_repository_id` UNIQUE, `github_identity_id` FK |
| `github_repository_languages` | Language byte distribution | `id`, `repository_id` FK, `(repository_id, language_name)` UNIQUE |
| `github_commits` | Verified candidate commit history | `id`, `repository_id` FK, `(repository_id, github_commit_sha)` UNIQUE |
| `github_pull_requests` | Observed pull requests | `id`, `repository_id` FK, `(repository_id, github_pr_id)` UNIQUE |

---

## 4. Evidence Graph Mapping

Observations are mapped to the Evidence Graph via `GithubEvidenceMapperService`:

1. **Language Evidence**:
   - Matches repository language byte distributions against existing `Skill` catalog entries (`findByNormalizedName`).
   - Does **NOT** pollute the global skill catalog with raw external language strings.
   - Creates `Evidence` with `EvidenceType.REPOSITORY` and `EvidenceRelationshipType.DEMONSTRATES`.

2. **Commit Evidence**:
   - Filters commits where `authorLogin` matches the candidate's verified GitHub identity.
   - Creates `Evidence` with `EvidenceType.COMMIT_ACTIVITY`.

3. **Idempotency**:
   - Uses `sourceReference` (e.g. `GITHUB:lang:<repo_id>:<lang>`) to prevent duplicate evidence nodes across re-sync runs.

---

## 5. API Endpoints

| Endpoint | Method | Role Required | Description |
|----------|--------|---------------|-------------|
| `/api/v1/candidates/{id}/github` | GET | `RECRUITER` | Get connection status & latest sync summary |
| `/api/v1/candidates/{id}/github/connect` | GET | `RECRUITER` | Initiate OAuth flow & return authorization URL |
| `/api/v1/github/oauth/callback` | GET | Public (State-signed) | Process OAuth callback & link GitHub account |
| `/api/v1/candidates/{id}/github/sync` | POST | `RECRUITER` | Trigger GitHub evidence synchronization |
| `/api/v1/candidates/{id}/github/repositories` | GET | `RECRUITER` | List synchronized repositories & languages |
| `/api/v1/candidates/{id}/github` | DELETE | `RECRUITER` | Disconnect GitHub identity |

---

## 6. Audit Log Events

Captured via `AuditEventService`:
- `GITHUB_CONNECTED`
- `GITHUB_SYNC_STARTED`
- `GITHUB_SYNC_COMPLETED`
- `GITHUB_SYNC_FAILED`
- `GITHUB_DISCONNECTED`
