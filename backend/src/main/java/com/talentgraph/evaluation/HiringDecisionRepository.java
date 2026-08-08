package com.talentgraph.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HiringDecisionRepository extends JpaRepository<HiringDecision, UUID> {
    Optional<HiringDecision> findByApplicationId(UUID applicationId);
}
