package com.talentgraph.evidence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, UUID> {
    List<Evidence> findByCandidateId(UUID candidateId);
    List<Evidence> findByEvidenceSourceId(UUID evidenceSourceId);
    List<Evidence> findByCandidateIdAndEvidenceType(UUID candidateId, EvidenceType evidenceType);
    java.util.Optional<Evidence> findByCandidateIdAndSourceReference(UUID candidateId, String sourceReference);
}
