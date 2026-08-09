package com.talentgraph.assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, UUID> {

    List<AssessmentAttempt> findByApplicationAssessmentId(UUID applicationAssessmentId);

    Optional<AssessmentAttempt> findByApplicationAssessmentIdAndCandidateId(UUID applicationAssessmentId, UUID candidateId);

    List<AssessmentAttempt> findByCandidateId(UUID candidateId);
}
