package com.talentgraph.ai.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiAnalysisRunRepository extends JpaRepository<AiAnalysisRun, UUID> {

    List<AiAnalysisRun> findByDocumentIdOrderByCreatedAtDesc(UUID documentId);

    Optional<AiAnalysisRun> findTopByDocumentIdOrderByCreatedAtDesc(UUID documentId);

    /**
     * Find an existing completed analysis run that used the same content hash,
     * analysis type, prompt version, and model — for idempotency checks.
     */
    @Query("""
        SELECT r FROM AiAnalysisRun r
        WHERE r.document.id = :documentId
          AND r.inputHash = :inputHash
          AND r.analysisType = :analysisType
          AND r.promptVersion = :promptVersion
          AND r.model = :model
          AND r.status = com.talentgraph.ai.analysis.AiAnalysisStatus.COMPLETED
        ORDER BY r.createdAt DESC
        """)
    Optional<AiAnalysisRun> findMatchingCompletedRun(
            @Param("documentId") UUID documentId,
            @Param("inputHash") String inputHash,
            @Param("analysisType") String analysisType,
            @Param("promptVersion") String promptVersion,
            @Param("model") String model
    );
}
