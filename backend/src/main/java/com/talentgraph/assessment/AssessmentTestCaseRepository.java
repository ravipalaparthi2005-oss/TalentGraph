package com.talentgraph.assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssessmentTestCaseRepository extends JpaRepository<AssessmentTestCase, UUID> {

    List<AssessmentTestCase> findByQuestionIdOrderByDisplayOrderAsc(UUID questionId);

    List<AssessmentTestCase> findByQuestionIdAndTestCaseTypeOrderByDisplayOrderAsc(UUID questionId, TestCaseType testCaseType);

    long countByQuestionIdAndTestCaseType(UUID questionId, TestCaseType testCaseType);
}
