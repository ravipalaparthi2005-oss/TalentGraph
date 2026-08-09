package com.talentgraph.github;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "github_repositories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "github_identity_id", nullable = false)
    private GithubIdentity githubIdentity;

    @Column(name = "github_repository_id", nullable = false, unique = true)
    private Long githubRepositoryId;

    @Column(name = "owner_login", nullable = false, length = 150)
    private String ownerLogin;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "full_name", nullable = false, length = 350)
    private String fullName;

    @Column(name = "html_url", nullable = false, length = 500)
    private String htmlUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_private", nullable = false)
    @Builder.Default
    private Boolean isPrivate = false;

    @Column(name = "is_fork", nullable = false)
    @Builder.Default
    private Boolean isFork = false;

    @Column(name = "default_branch", length = 100)
    private String defaultBranch;

    @Column(length = 100)
    private String language;

    @Column(name = "stars_count", nullable = false)
    @Builder.Default
    private Integer starsCount = 0;

    @Column(name = "forks_count", nullable = false)
    @Builder.Default
    private Integer forksCount = 0;

    @Column(name = "created_at_github")
    private Instant createdAtGithub;

    @Column(name = "updated_at_github")
    private Instant updatedAtGithub;

    @Column(name = "pushed_at_github")
    private Instant pushedAtGithub;

    @Column(name = "last_synced_at", nullable = false)
    @Builder.Default
    private Instant lastSyncedAt = Instant.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
