package com.talentgraph.assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationAssessmentRepository extends JpaRepository<ApplicationAssessment, UUID> {

    List<ApplicationAssessment> findByApplicationId(UUID applicationId);

    Optional<ApplicationAssessment> findByApplicationIdAndAssessmentId(UUID applicationId, UUID assessmentId);

    boolean existsByApplicationIdAndAssessmentId(UUID applicationId, UUID assessmentId);
}
