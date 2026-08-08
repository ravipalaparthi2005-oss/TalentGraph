package com.talentgraph.evidence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID> {
    Optional<Skill> findByNormalizedName(String normalizedName);
    boolean existsByNormalizedName(String normalizedName);
    java.util.List<Skill> findByNameContainingIgnoreCaseOrNormalizedNameContainingIgnoreCase(String nameQuery, String normalizedQuery);
}
