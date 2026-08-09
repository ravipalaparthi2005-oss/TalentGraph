package com.talentgraph.assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentQuestionSkillRepository extends JpaRepository<AssessmentQuestionSkill, AssessmentQuestionSkillId> {

    List<AssessmentQuestionSkill> findByQuestionId(java.util.UUID questionId);

    void deleteByQuestionId(java.util.UUID questionId);
}
