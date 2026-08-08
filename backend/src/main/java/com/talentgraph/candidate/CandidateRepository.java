package com.talentgraph.candidate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, UUID> {
    List<Candidate> findByOrganizationId(UUID organizationId);
    Optional<Candidate> findByOrganizationIdAndEmail(UUID organizationId, String email);
    Optional<Candidate> findByOrganizationIdAndGithubUsername(UUID organizationId, String githubUsername);
}
