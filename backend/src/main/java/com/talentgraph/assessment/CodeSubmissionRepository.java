package com.talentgraph.assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission, UUID> {

    List<CodeSubmission> findByAttemptId(UUID attemptId);

    List<CodeSubmission> findByAttemptIdAndQuestionIdOrderBySubmittedAtDesc(UUID attemptId, UUID questionId);

    Optional<CodeSubmission> findTopByAttemptIdAndQuestionIdOrderBySubmittedAtDesc(UUID attemptId, UUID questionId);

    Optional<CodeSubmission> findByJudge0SubmissionToken(String judge0SubmissionToken);
}
