package com.talentgraph.github.repository;

import com.talentgraph.github.GithubRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GithubRepositoryRepository extends JpaRepository<GithubRepository, UUID> {

    List<GithubRepository> findByGithubIdentityIdOrderByStarsCountDesc(UUID githubIdentityId);

    Optional<GithubRepository> findByGithubIdentityIdAndGithubRepositoryId(UUID githubIdentityId, Long githubRepositoryId);

    Optional<GithubRepository> findByGithubRepositoryId(Long githubRepositoryId);
}
