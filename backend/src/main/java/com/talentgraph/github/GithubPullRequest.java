package com.talentgraph.github;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "github_pull_requests", uniqueConstraints = {
        @UniqueConstraint(name = "uk_gh_repo_pr", columnNames = {"repository_id", "github_pr_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubPullRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private GithubRepository repository;

    @Column(name = "github_pr_id", nullable = false)
    private Long githubPrId;

    @Column(nullable = false)
    private Integer number;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(nullable = false, length = 50)
    private String state;

    @Column(name = "author_login", length = 150)
    private String authorLogin;

    @Column(name = "html_url", nullable = false, length = 500)
    private String htmlUrl;

    @Column(name = "created_at_github")
    private Instant createdAtGithub;

    @Column(name = "merged_at")
    private Instant mergedAt;

    @Column(name = "observed_at", nullable = false)
    @Builder.Default
    private Instant observedAt = Instant.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
