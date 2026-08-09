package com.talentgraph.github.repository;

import com.talentgraph.github.GithubIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GithubIdentityRepository extends JpaRepository<GithubIdentity, UUID> {

    Optional<GithubIdentity> findByCandidateIdAndIsActiveTrue(UUID candidateId);

    Optional<GithubIdentity> findByCandidateId(UUID candidateId);

    Optional<GithubIdentity> findByGithubUserId(Long githubUserId);

    boolean existsByGithubUserIdAndCandidateIdNotAndIsActiveTrue(Long githubUserId, UUID candidateId);
}
