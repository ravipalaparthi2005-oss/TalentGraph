package com.talentgraph.github;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "github_repository_languages", uniqueConstraints = {
        @UniqueConstraint(name = "uk_gh_repo_language", columnNames = {"repository_id", "language_name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubRepositoryLanguage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private GithubRepository repository;

    @Column(name = "language_name", nullable = false, length = 100)
    private String languageName;

    @Column(name = "bytes_count", nullable = false)
    @Builder.Default
    private Long bytesCount = 0L;

    @Column(name = "observed_at", nullable = false)
    @Builder.Default
    private Instant observedAt = Instant.now();
}
