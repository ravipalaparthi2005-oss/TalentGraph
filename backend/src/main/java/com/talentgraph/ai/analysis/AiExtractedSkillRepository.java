package com.talentgraph.ai.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AiExtractedSkillRepository extends JpaRepository<AiExtractedSkill, UUID> {
    List<AiExtractedSkill> findByRunId(UUID runId);
}
