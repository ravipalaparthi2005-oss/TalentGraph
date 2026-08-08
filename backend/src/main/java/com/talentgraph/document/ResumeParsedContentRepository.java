package com.talentgraph.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeParsedContentRepository extends JpaRepository<ResumeParsedContent, UUID> {

    Optional<ResumeParsedContent> findByDocumentId(UUID documentId);
}
