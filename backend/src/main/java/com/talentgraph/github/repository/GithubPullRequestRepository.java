package com.talentgraph.github.repository;

import com.talentgraph.github.GithubPullRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GithubPullRequestRepository extends JpaRepository<GithubPullRequest, UUID> {

    List<GithubPullRequest> findByRepositoryIdOrderByCreatedAtGithubDesc(UUID repositoryId);

    Optional<GithubPullRequest> findByRepositoryIdAndGithubPrId(UUID repositoryId, Long githubPrId);
}
