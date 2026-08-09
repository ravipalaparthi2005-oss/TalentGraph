-- V5: AI Resume Intelligence schema
-- Tracks AI extraction runs, extracted skill/experience/project/education/certification proposals

CREATE TABLE ai_analysis_runs (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    document_id     UUID NOT NULL REFERENCES candidate_documents(id) ON DELETE RESTRICT,
    provider        VARCHAR(50)  NOT NULL,
    model           VARCHAR(200) NOT NULL,
    analysis_type   VARCHAR(50)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    prompt_version  VARCHAR(50)  NOT NULL,
    input_hash      VARCHAR(64)  NOT NULL,
    started_at      TIMESTAMP WITH TIME ZONE,
    completed_at    TIMESTAMP WITH TIME ZONE,
    error_code      VARCHAR(50),
    error_message   TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_runs_document_id   ON ai_analysis_runs(document_id);
CREATE INDEX idx_ai_runs_input_hash    ON ai_analysis_runs(input_hash);
CREATE INDEX idx_ai_runs_status        ON ai_analysis_runs(status);
CREATE INDEX idx_ai_runs_created_at    ON ai_analysis_runs(created_at);

-- Extracted skill proposals from AI (validated against catalog before becoming Evidence)
CREATE TABLE ai_extracted_skills (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    run_id          UUID NOT NULL REFERENCES ai_analysis_runs(id) ON DELETE CASCADE,
    skill_name      VARCHAR(200) NOT NULL,
    evidence_quote  TEXT         NOT NULL,
    source_section  VARCHAR(100),
    catalog_skill_id UUID REFERENCES skills(id) ON DELETE SET NULL,
    validated       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_skills_run_id ON ai_extracted_skills(run_id);

-- Extracted work experience proposals
CREATE TABLE ai_extracted_experiences (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    run_id          UUID NOT NULL REFERENCES ai_analysis_runs(id) ON DELETE CASCADE,
    company         VARCHAR(300),
    job_title       VARCHAR(300),
    start_date      VARCHAR(50),
    end_date        VARCHAR(50),
    responsibilities TEXT,
    evidence_quote  TEXT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_exp_run_id ON ai_extracted_experiences(run_id);

-- Extracted project proposals
CREATE TABLE ai_extracted_projects (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    run_id          UUID NOT NULL REFERENCES ai_analysis_runs(id) ON DELETE CASCADE,
    project_name    VARCHAR(300) NOT NULL,
    description     TEXT,
    technologies    TEXT,
    evidence_quote  TEXT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_proj_run_id ON ai_extracted_projects(run_id);

-- Extracted education proposals
CREATE TABLE ai_extracted_education (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    run_id          UUID NOT NULL REFERENCES ai_analysis_runs(id) ON DELETE CASCADE,
    institution     VARCHAR(300),
    degree          VARCHAR(200),
    field_of_study  VARCHAR(200),
    start_date      VARCHAR(50),
    end_date        VARCHAR(50),
    grade           VARCHAR(50),
    evidence_quote  TEXT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_edu_run_id ON ai_extracted_education(run_id);

-- Extracted certification proposals
CREATE TABLE ai_extracted_certifications (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    run_id          UUID NOT NULL REFERENCES ai_analysis_runs(id) ON DELETE CASCADE,
    certification_name VARCHAR(300) NOT NULL,
    issuing_body    VARCHAR(300),
    issued_date     VARCHAR(50),
    evidence_quote  TEXT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_cert_run_id ON ai_extracted_certifications(run_id);
