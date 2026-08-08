package com.talentgraph.evidence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvidenceSkillRepository extends JpaRepository<EvidenceSkill, EvidenceSkillId> {
    List<EvidenceSkill> findByIdEvidenceId(UUID evidenceId);
    List<EvidenceSkill> findByIdSkillId(UUID skillId);
}
