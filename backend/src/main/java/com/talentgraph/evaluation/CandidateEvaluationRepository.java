package com.talentgraph.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidateEvaluationRepository extends JpaRepository<CandidateEvaluation, UUID> {
    List<CandidateEvaluation> findByApplicationId(UUID applicationId);
}
