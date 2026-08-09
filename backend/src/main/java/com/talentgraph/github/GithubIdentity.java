package com.talentgraph.github;

import com.talentgraph.candidate.Candidate;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Authenticated GitHub identity linked to a Candidate profile.
 *
 * <p>Security rule: Access token is stored ENCRYPTED at rest and NEVER returned via API.
 */
@Entity
@Table(name = "github_identities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(name = "github_user_id", nullable = false, unique = true)
    private Long githubUserId;

    @Column(nullable = false, length = 150)
    private String login;

    @Column(name = "profile_url", nullable = false, length = 500)
    private String profileUrl;

    @Column(name = "access_token_encrypted", nullable = false, columnDefinition = "TEXT")
    private String accessTokenEncrypted;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(length = 255)
    private String scope;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "connected_at", nullable = false)
    @Builder.Default
    private Instant connectedAt = Instant.now();

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
