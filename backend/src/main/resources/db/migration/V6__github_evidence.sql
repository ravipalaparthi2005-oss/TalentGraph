-- V6: GitHub Evidence Engine Schema
-- Stores authenticated GitHub identities, sync runs, repository metadata, language distributions, commits, and pull requests.

-- 1. GITHUB IDENTITIES
CREATE TABLE github_identities (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    candidate_id            UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    github_user_id          BIGINT NOT NULL UNIQUE,
    login                   VARCHAR(150) NOT NULL,
    profile_url             VARCHAR(500) NOT NULL,
    access_token_encrypted  TEXT NOT NULL,
    token_expires_at        TIMESTAMP WITH TIME ZONE,
    scope                   VARCHAR(255),
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    connected_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_synced_at          TIMESTAMP WITH TIME ZONE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_gh_identities_candidate_id ON github_identities(candidate_id);
CREATE INDEX idx_gh_identities_github_user_id ON github_identities(github_user_id);
CREATE INDEX idx_gh_identities_login ON github_identities(login);

-- 2. GITHUB SYNC RUNS
CREATE TABLE github_sync_runs (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    github_identity_id      UUID NOT NULL REFERENCES github_identities(id) ON DELETE CASCADE,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at            TIMESTAMP WITH TIME ZONE,
    repositories_processed  INT NOT NULL DEFAULT 0,
    observations_created    INT NOT NULL DEFAULT 0,
    error_code              VARCHAR(50),
    error_message           TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_gh_sync_runs_identity_id ON github_sync_runs(github_identity_id);
CREATE INDEX idx_gh_sync_runs_status ON github_sync_runs(status);

-- 3. GITHUB REPOSITORIES
CREATE TABLE github_repositories (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    github_identity_id      UUID NOT NULL REFERENCES github_identities(id) ON DELETE CASCADE,
    github_repository_id    BIGINT NOT NULL UNIQUE,
    owner_login             VARCHAR(150) NOT NULL,
    name                    VARCHAR(200) NOT NULL,
    full_name               VARCHAR(350) NOT NULL,
    html_url                VARCHAR(500) NOT NULL,
    description             TEXT,
    is_private              BOOLEAN NOT NULL DEFAULT FALSE,
    is_fork                 BOOLEAN NOT NULL DEFAULT FALSE,
    default_branch          VARCHAR(100),
    language                VARCHAR(100),
    stars_count             INT NOT NULL DEFAULT 0,
    forks_count             INT NOT NULL DEFAULT 0,
    created_at_github       TIMESTAMP WITH TIME ZONE,
    updated_at_github       TIMESTAMP WITH TIME ZONE,
    pushed_at_github        TIMESTAMP WITH TIME ZONE,
    last_synced_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_gh_repos_identity_id ON github_repositories(github_identity_id);
CREATE INDEX idx_gh_repos_github_id ON github_repositories(github_repository_id);
CREATE INDEX idx_gh_repos_language ON github_repositories(language);

-- 4. GITHUB REPOSITORY LANGUAGES
CREATE TABLE github_repository_languages (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    repository_id           UUID NOT NULL REFERENCES github_repositories(id) ON DELETE CASCADE,
    language_name           VARCHAR(100) NOT NULL,
    bytes_count             BIGINT NOT NULL DEFAULT 0,
    observed_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_gh_repo_language UNIQUE (repository_id, language_name)
);

CREATE INDEX idx_gh_repo_languages_repo_id ON github_repository_languages(repository_id);

-- 5. GITHUB COMMITS
CREATE TABLE github_commits (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    repository_id           UUID NOT NULL REFERENCES github_repositories(id) ON DELETE CASCADE,
    github_commit_sha       VARCHAR(64) NOT NULL,
    author_login            VARCHAR(150),
    author_email            VARCHAR(255),
    message                 TEXT NOT NULL,
    commit_url              VARCHAR(500) NOT NULL,
    committed_at            TIMESTAMP WITH TIME ZONE,
    observed_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_gh_repo_commit_sha UNIQUE (repository_id, github_commit_sha)
);

CREATE INDEX idx_gh_commits_repo_id ON github_commits(repository_id);
CREATE INDEX idx_gh_commits_sha ON github_commits(github_commit_sha);
CREATE INDEX idx_gh_commits_author ON github_commits(author_login);

-- 6. GITHUB PULL REQUESTS
CREATE TABLE github_pull_requests (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    repository_id           UUID NOT NULL REFERENCES github_repositories(id) ON DELETE CASCADE,
    github_pr_id            BIGINT NOT NULL,
    number                  INT NOT NULL,
    title                   TEXT NOT NULL,
    state                   VARCHAR(50) NOT NULL,
    author_login            VARCHAR(150),
    html_url                VARCHAR(500) NOT NULL,
    created_at_github       TIMESTAMP WITH TIME ZONE,
    merged_at               TIMESTAMP WITH TIME ZONE,
    observed_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_gh_repo_pr UNIQUE (repository_id, github_pr_id)
);

CREATE INDEX idx_gh_prs_repo_id ON github_pull_requests(repository_id);
CREATE INDEX idx_gh_prs_author ON github_pull_requests(author_login);
