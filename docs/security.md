# TalentGraph Security & Multi-Tenant Authorization Specification

## 1. Authentication Architecture

TalentGraph supports two primary authentication channels:
1. **Email & Password Authentication**: Validated with BCrypt password hashing (cost factor 10).
2. **Google OAuth 2.0 Integration**: Managed by Spring Security OAuth2 Client (`/oauth2/authorization/google`).

### Email Normalization
All user emails are normalized (`trim().toLowerCase()`) on registration, login, and OAuth matching. The database enforces a `UNIQUE` constraint on `users.email`.

---

## 2. JWT & Refresh Token Architecture

### Token Split & Storage
- **Access Token**: Short-lived (15 minutes), signed with HMAC-SHA256 (`JWT_SECRET`). It is returned in JSON responses and stored in-memory by the frontend (never stored in `localStorage` or `sessionStorage`).
- **Refresh Token**: Long-lived (7 days), generated as a 64-byte secure random string, hashed using SHA-256, and stored in the `refresh_tokens` database table. It is delivered to the browser as an `HttpOnly`, `Path=/api/v1/auth`, `SameSite=Lax` cookie (`talentgraph_refresh_token`).

### Token Rotation & Reuse Detection
When `/api/v1/auth/refresh` is invoked:
1. The incoming raw refresh token is hashed with SHA-256.
2. The database record is queried by `token_hash`.
3. If the token is **already revoked** (indicating token theft or replay attack), the system automatically revokes **all active tokens** associated with that user session.
4. If valid, the old token is marked `revoked_at = NOW()`, linked to `replaced_by_token_id`, and a new refresh token is issued and set in the response cookie.

---

## 3. Multi-Tenant Authorization Engine

Every protected resource in TalentGraph belongs to an `Organization`.

### Server-Side Ownership Resolution
- **No Untrusted Headers**: The backend never trusts client-supplied headers such as `X-Organization-ID` or request params.
- **Entity Resolution**: When accessing `/api/v1/candidates/{candidateId}`, the backend loads the `Candidate` entity from PostgreSQL, inspects `candidate.getOrganization().getId()`, and checks `OrganizationAuthorizationService.requireOrganizationMember(currentUser, candidateOrgId)`.

### Role Permission Hierarchy

```text
  OWNER (Rank 50)
    └── ADMIN (Rank 40)
          └── RECRUITER (Rank 30)
                └── HIRING_MANAGER (Rank 20)
                      └── INTERVIEWER (Rank 10)
```

- **`OWNER`**: Full organization access, member management, org settings, hiring decisions, audit log access.
- **`ADMIN`**: Manage jobs, candidates, assessments, interviews, evaluations, members, audit access.
- **`RECRUITER`**: Create & manage jobs, candidates, applications, assessments, view interview results.
- **`HIRING_MANAGER`**: View jobs, assigned candidates, review evidence/assessments, participate in hiring decisions.
- **`INTERVIEWER`**: View assigned interviews, submit rubrics/evaluations, view candidate info required for assigned interview.

---

## 4. IDOR Prevention & Automated Verification

Insecure Direct Object Reference (IDOR) attacks are prevented by `OrganizationAuthorizationService.verifyResourceAccess(User user, UUID resourceOrgId)`.

### Automated Verification Test
The backend includes a mandatory security test (`AuthenticationTest.testIdorCandidateAccessCrossOrganizationRejection`):
- User A is a `RECRUITER` in Organization A.
- Candidate B belongs to Organization B.
- User A attempts `GET /api/v1/candidates/{candidateB.id}` using a valid Bearer token for User A.
- Result: **HTTP 403 Forbidden** (`Access denied: User is not a member of this organization`).

---

## 5. CORS, Security Headers & CSRF Reasoning

### CORS Configuration
- Configured dynamically from `${frontend.url:http://localhost:5173}` using `CorsConfiguration`.
- `allowCredentials(true)` enabled for secure HTTP-Only cookies.

### Security Headers
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy: strict-origin-when-cross-origin`

### CSRF Protection Reasoning
REST API authentication uses stateless Bearer tokens in the `Authorization` header for protected endpoints (`/api/v1/**`), making API requests immune to standard cross-site request forgery. Refresh token endpoints (`/api/v1/auth/refresh`) use `SameSite=Lax` HTTP-Only cookies restricted strictly to the `/api/v1/auth` path.

---

## 6. Future Security Hardening Tasks

1. **Bucket-backed Rate Limiting**: Adding Redis-backed rate limiting (e.g. Bucket4j / Resilience4j) on `/api/v1/auth/login` and `/api/v1/auth/register`.
2. **WebAuthn / FIDO2 Passkeys**: Multi-factor authentication support for `OWNER` and `ADMIN` users.
3. **Audit Event Alerting**: Automated notifications when consecutive failed refresh token attempts or unauthorized IDOR access attempts occur.
