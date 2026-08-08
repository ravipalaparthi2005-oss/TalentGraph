package com.talentgraph.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluationCriterionRepository extends JpaRepository<EvaluationCriterion, UUID> {
    List<EvaluationCriterion> findByJobId(UUID jobId);
}
