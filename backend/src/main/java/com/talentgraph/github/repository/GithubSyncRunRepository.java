package com.talentgraph.github.repository;

import com.talentgraph.github.GithubSyncRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GithubSyncRunRepository extends JpaRepository<GithubSyncRun, UUID> {

    List<GithubSyncRun> findByGithubIdentityIdOrderByStartedAtDesc(UUID githubIdentityId);

    Optional<GithubSyncRun> findTopByGithubIdentityIdOrderByStartedAtDesc(UUID githubIdentityId);
}
