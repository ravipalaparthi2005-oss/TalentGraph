-- V7: Real Coding Assessment Engine & Judge0 Schema
-- Manages controlled coding languages, coding questions, public/hidden test cases, application assessment assignments, attempts, code submissions, and question skill mappings.

-- 1. CONTROLLED CODING LANGUAGES
CREATE TABLE coding_languages (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name                    VARCHAR(100) NOT NULL UNIQUE,
    slug                    VARCHAR(100) NOT NULL UNIQUE,
    judge0_language_id      INT NOT NULL UNIQUE,
    is_enabled              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_coding_lang_enabled ON coding_languages(is_enabled);
CREATE INDEX idx_coding_lang_judge0_id ON coding_languages(judge0_language_id);

-- Seed default controlled languages (Judge0 standard IDs: Java=62, Python=71, JavaScript=63, C++=54, C=50)
INSERT INTO coding_languages (id, name, slug, judge0_language_id, is_enabled) VALUES
    (gen_random_uuid(), 'Java (OpenJDK 17)', 'java', 62, true),
    (gen_random_uuid(), 'Python (3.8.1)', 'python', 71, true),
    (gen_random_uuid(), 'JavaScript (Node.js 18.15.0)', 'javascript', 63, true),
    (gen_random_uuid(), 'C++ (GCC 9.2.0)', 'cpp', 54, true),
    (gen_random_uuid(), 'C (GCC 9.2.0)', 'c', 50, true);

-- 2. ASSESSMENT QUESTIONS
CREATE TABLE assessment_questions (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    assessment_id           UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    title                   VARCHAR(300) NOT NULL,
    description             TEXT NOT NULL,
    constraints             TEXT,
    input_format            TEXT,
    output_format           TEXT,
    examples_json           TEXT,
    difficulty              VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    points                  INT NOT NULL DEFAULT 10,
    display_order           INT NOT NULL DEFAULT 1,
    time_limit_seconds      DOUBLE PRECISION NOT NULL DEFAULT 2.0,
    memory_limit_mb         INT NOT NULL DEFAULT 256,
    allowed_languages_json  TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_questions_assessment_id ON assessment_questions(assessment_id);
CREATE INDEX idx_questions_display_order ON assessment_questions(display_order);

-- 3. ASSESSMENT TEST CASES (PUBLIC & HIDDEN)
CREATE TABLE assessment_test_cases (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    question_id             UUID NOT NULL REFERENCES assessment_questions(id) ON DELETE CASCADE,
    input                   TEXT NOT NULL,
    expected_output         TEXT NOT NULL,
    test_case_type          VARCHAR(20) NOT NULL DEFAULT 'HIDDEN',
    display_order           INT NOT NULL DEFAULT 1,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_test_cases_question_id ON assessment_test_cases(question_id);
CREATE INDEX idx_test_cases_type ON assessment_test_cases(test_case_type);

-- 4. APPLICATION ASSESSMENTS
CREATE TABLE application_assessments (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    application_id          UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    assessment_id           UUID NOT NULL REFERENCES assessments(id) ON DELETE RESTRICT,
    assigned_by_id          UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    assigned_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_at                  TIMESTAMP WITH TIME ZONE,
    status                  VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED',
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_app_assessment UNIQUE (application_id, assessment_id)
);

CREATE INDEX idx_app_assessments_app_id ON application_assessments(application_id);
CREATE INDEX idx_app_assessments_assessment_id ON application_assessments(assessment_id);
CREATE INDEX idx_app_assessments_status ON application_assessments(status);

-- 5. ASSESSMENT ATTEMPTS
CREATE TABLE assessment_attempts (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    application_assessment_id UUID NOT NULL REFERENCES application_assessments(id) ON DELETE CASCADE,
    candidate_id            UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    status                  VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',
    started_at              TIMESTAMP WITH TIME ZONE,
    submitted_at            TIMESTAMP WITH TIME ZONE,
    evaluated_at            TIMESTAMP WITH TIME ZONE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_attempts_app_assessment_id ON assessment_attempts(application_assessment_id);
CREATE INDEX idx_attempts_candidate_id ON assessment_attempts(candidate_id);
CREATE INDEX idx_attempts_status ON assessment_attempts(status);

-- 6. CODE SUBMISSIONS (VERIFIED JUDGE0 RESULTS)
CREATE TABLE code_submissions (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    attempt_id              UUID NOT NULL REFERENCES assessment_attempts(id) ON DELETE CASCADE,
    question_id             UUID NOT NULL REFERENCES assessment_questions(id) ON DELETE RESTRICT,
    language                VARCHAR(50) NOT NULL,
    source_code             TEXT NOT NULL,
    status                  VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    submitted_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    evaluated_at            TIMESTAMP WITH TIME ZONE,
    judge0_submission_token VARCHAR(100),
    execution_time_ms       DOUBLE PRECISION,
    memory_kb               LONG,
    test_cases_passed       INT NOT NULL DEFAULT 0,
    test_cases_total        INT NOT NULL DEFAULT 0,
    judge0_status_id        INT,
    compiler_output         TEXT,
    stderr                  TEXT,
    stdout                  TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_submissions_attempt_id ON code_submissions(attempt_id);
CREATE INDEX idx_submissions_question_id ON code_submissions(question_id);
CREATE INDEX idx_submissions_status ON code_submissions(status);
CREATE INDEX idx_submissions_judge0_token ON code_submissions(judge0_submission_token);

-- 7. QUESTION SKILL MAPPINGS
CREATE TABLE assessment_question_skills (
    question_id             UUID NOT NULL REFERENCES assessment_questions(id) ON DELETE CASCADE,
    skill_id                UUID NOT NULL REFERENCES skills(id) ON DELETE RESTRICT,
    PRIMARY KEY (question_id, skill_id)
);

CREATE INDEX idx_q_skills_skill_id ON assessment_question_skills(skill_id);
