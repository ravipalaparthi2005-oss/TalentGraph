package com.talentgraph.github.repository;

import com.talentgraph.github.GithubCommit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GithubCommitRepository extends JpaRepository<GithubCommit, UUID> {

    List<GithubCommit> findByRepositoryIdOrderByCommittedAtDesc(UUID repositoryId);

    Optional<GithubCommit> findByRepositoryIdAndGithubCommitSha(UUID repositoryId, String githubCommitSha);
}
