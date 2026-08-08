package com.talentgraph.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateDocumentRepository extends JpaRepository<CandidateDocument, UUID> {

    List<CandidateDocument> findByCandidateIdOrderByUploadedAtDesc(UUID candidateId);

    List<CandidateDocument> findByApplicationId(UUID applicationId);

    Optional<CandidateDocument> findByCandidateIdAndSha256Hash(UUID candidateId, String sha256Hash);

    Optional<CandidateDocument> findByStorageKey(String storageKey);
}
