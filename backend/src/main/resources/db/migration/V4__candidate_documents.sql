-- ==============================================================================
-- TalentGraph Phase 05 Migration: Candidate Soft-Delete & Resume Ingestion
-- Version: 4.0.0
-- Migration: V4__candidate_documents.sql
-- Description: Adds is_active to candidates for soft archiving, candidate_documents
--              table for uploaded file tracking, and resume_parsed_contents for
--              persisted extracted raw text.
-- ==============================================================================

-- 1. ADD SOFT DELETE / ACTIVE FLAG TO CANDIDATES
ALTER TABLE candidates ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true;

-- 2. CANDIDATE DOCUMENTS TABLE
CREATE TABLE candidate_documents (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    candidate_id UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    application_id UUID REFERENCES applications(id) ON DELETE SET NULL,
    document_type VARCHAR(50) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    mime_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes > 0),
    sha256_hash VARCHAR(64) NOT NULL,
    processing_status VARCHAR(50) NOT NULL,
    processing_error TEXT,
    uploaded_by UUID REFERENCES users(id) ON DELETE SET NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_candidate_docs_candidate ON candidate_documents(candidate_id);
CREATE INDEX idx_candidate_docs_app ON candidate_documents(application_id);
CREATE INDEX idx_candidate_docs_status ON candidate_documents(processing_status);
CREATE INDEX idx_candidate_docs_hash ON candidate_documents(sha256_hash);

-- 3. RESUME PARSED CONTENTS TABLE (ONE-TO-ONE WITH CANDIDATE DOCUMENTS)
CREATE TABLE resume_parsed_contents (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    document_id UUID NOT NULL UNIQUE REFERENCES candidate_documents(id) ON DELETE CASCADE,
    raw_text TEXT NOT NULL,
    parser_version VARCHAR(50) NOT NULL,
    character_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_parsed_content_doc ON resume_parsed_contents(document_id);
