package com.talentgraph.github;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "github_commits", uniqueConstraints = {
        @UniqueConstraint(name = "uk_gh_repo_commit_sha", columnNames = {"repository_id", "github_commit_sha"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubCommit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private GithubRepository repository;

    @Column(name = "github_commit_sha", nullable = false, length = 64)
    private String githubCommitSha;

    @Column(name = "author_login", length = 150)
    private String authorLogin;

    @Column(name = "author_email", length = 255)
    private String authorEmail;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "commit_url", nullable = false, length = 500)
    private String commitUrl;

    @Column(name = "committed_at")
    private Instant committedAt;

    @Column(name = "observed_at", nullable = false)
    @Builder.Default
    private Instant observedAt = Instant.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
