package com.talentgraph.ai.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AiExtractedCertificationRepository extends JpaRepository<AiExtractedCertification, UUID> {
    List<AiExtractedCertification> findByRunId(UUID runId);
}
