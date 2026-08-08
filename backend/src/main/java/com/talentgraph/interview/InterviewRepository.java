package com.talentgraph.interview;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, UUID> {
    List<Interview> findByCandidateId(UUID candidateId);
    List<Interview> findByOrganizationId(UUID organizationId);
    List<Interview> findByApplicationId(UUID applicationId);
}
