package com.talentgraph.github.controller;

import com.talentgraph.auth.User;
import com.talentgraph.auth.service.CurrentUserService;
import com.talentgraph.candidate.Candidate;
import com.talentgraph.candidate.CandidateRepository;
import com.talentgraph.common.exception.ResourceNotFoundException;
import com.talentgraph.github.*;
import com.talentgraph.github.dto.GithubIdentityResponse;
import com.talentgraph.github.dto.GithubRepositoryResponse;
import com.talentgraph.github.dto.GithubSyncResponse;
import com.talentgraph.github.repository.*;
import com.talentgraph.github.service.GithubOAuthService;
import com.talentgraph.github.service.GithubSyncService;
import com.talentgraph.organization.OrganizationRole;
import com.talentgraph.organization.service.OrganizationAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for managing candidate GitHub integration and evidence data.
 */
@RestController
@RequestMapping("/api/v1/candidates/{candidateId}/github")
@RequiredArgsConstructor
public class CandidateGithubController {

    private final GithubOAuthService oauthService;
    private final GithubSyncService syncService;
    private final GithubIdentityRepository identityRepository;
    private final GithubSyncRunRepository syncRunRepository;
    private final GithubRepositoryRepository repoRepository;
    private final GithubRepositoryLanguageRepository languageRepository;
    private final GithubCommitRepository commitRepository;
    private final GithubPullRequestRepository prRepository;
    private final CandidateRepository candidateRepository;
    private final CurrentUserService currentUserService;
    private final OrganizationAuthorizationService authorizationService;

    /**
     * Get candidate's GitHub identity connection status.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GithubIdentityResponse> getGithubStatus(@PathVariable UUID candidateId) {
        User currentUser = currentUserService.getCurrentUser();
        authorizeAccess(candidateId, currentUser);

        Optional<GithubIdentity> identityOpt = identityRepository.findByCandidateIdAndIsActiveTrue(candidateId);
        if (identityOpt.isEmpty()) {
            return ResponseEntity.ok(GithubIdentityResponse.builder()
                    .candidateId(candidateId)
                    .connected(false)
                    .build());
        }

        GithubIdentity identity = identityOpt.get();
        Optional<GithubSyncRun> latestRun = syncRunRepository.findTopByGithubIdentityIdOrderByStartedAtDesc(identity.getId());

        return ResponseEntity.ok(GithubIdentityResponse.builder()
                .id(identity.getId())
                .candidateId(candidateId)
                .githubUserId(identity.getGithubUserId())
                .login(identity.getLogin())
                .profileUrl(identity.getProfileUrl())
                .connected(true)
                .connectedAt(identity.getConnectedAt() != null ? identity.getConnectedAt().toString() : null)
                .lastSyncedAt(identity.getLastSyncedAt() != null ? identity.getLastSyncedAt().toString() : null)
                .syncStatus(latestRun.map(r -> r.getStatus().name()).orElse("NOT_SYNCED"))
                .repositoriesProcessed(latestRun.map(GithubSyncRun::getRepositoriesProcessed).orElse(0))
                .observationsCreated(latestRun.map(GithubSyncRun::getObservationsCreated).orElse(0))
                .build());
    }

    /**
     * Trigger GitHub evidence synchronization.
     */
    @PostMapping("/sync")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GithubSyncResponse> syncGithub(@PathVariable UUID candidateId) {
        User currentUser = currentUserService.getCurrentUser();
        authorizeAccess(candidateId, currentUser);

        GithubSyncRun run = syncService.syncCandidateGithub(candidateId, currentUser);

        return ResponseEntity.ok(GithubSyncResponse.builder()
                .syncRunId(run.getId())
                .status(run.getStatus().name())
                .startedAt(run.getStartedAt() != null ? run.getStartedAt().toString() : null)
                .completedAt(run.getCompletedAt() != null ? run.getCompletedAt().toString() : null)
                .repositoriesProcessed(run.getRepositoriesProcessed())
                .observationsCreated(run.getObservationsCreated())
                .errorCode(run.getErrorCode())
                .errorMessage(run.getErrorMessage())
                .build());
    }

    /**
     * List candidate's synchronized GitHub repositories.
     */
    @GetMapping("/repositories")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GithubRepositoryResponse>> getRepositories(@PathVariable UUID candidateId) {
        User currentUser = currentUserService.getCurrentUser();
        authorizeAccess(candidateId, currentUser);

        GithubIdentity identity = identityRepository.findByCandidateIdAndIsActiveTrue(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("No active GitHub identity found for candidate: " + candidateId));

        List<GithubRepository> repos = repoRepository.findByGithubIdentityIdOrderByStarsCountDesc(identity.getId());

        List<GithubRepositoryResponse> response = repos.stream().map(repo -> {
            Map<String, Long> languages = languageRepository.findByRepositoryIdOrderByBytesCountDesc(repo.getId())
                    .stream().collect(Collectors.toMap(GithubRepositoryLanguage::getLanguageName, GithubRepositoryLanguage::getBytesCount));

            List<GithubRepositoryResponse.CommitSummary> commits = commitRepository.findByRepositoryIdOrderByCommittedAtDesc(repo.getId())
                    .stream().limit(5)
                    .map(c -> GithubRepositoryResponse.CommitSummary.builder()
                            .sha(c.getGithubCommitSha())
                            .authorLogin(c.getAuthorLogin())
                            .message(c.getMessage())
                            .commitUrl(c.getCommitUrl())
                            .committedAt(c.getCommittedAt() != null ? c.getCommittedAt().toString() : null)
                            .build()).toList();

            List<GithubRepositoryResponse.PullRequestSummary> prs = prRepository.findByRepositoryIdOrderByCreatedAtGithubDesc(repo.getId())
                    .stream().limit(5)
                    .map(pr -> GithubRepositoryResponse.PullRequestSummary.builder()
                            .prId(pr.getGithubPrId())
                            .number(pr.getNumber())
                            .title(pr.getTitle())
                            .state(pr.getState())
                            .authorLogin(pr.getAuthorLogin())
                            .htmlUrl(pr.getHtmlUrl())
                            .createdAt(pr.getCreatedAtGithub() != null ? pr.getCreatedAtGithub().toString() : null)
                            .mergedAt(pr.getMergedAt() != null ? pr.getMergedAt().toString() : null)
                            .build()).toList();

            return GithubRepositoryResponse.builder()
                    .id(repo.getId())
                    .githubRepositoryId(repo.getGithubRepositoryId())
                    .ownerLogin(repo.getOwnerLogin())
                    .name(repo.getName())
                    .fullName(repo.getFullName())
                    .htmlUrl(repo.getHtmlUrl())
                    .description(repo.getDescription())
                    .isPrivate(repo.getIsPrivate())
                    .isFork(repo.getIsFork())
                    .defaultBranch(repo.getDefaultBranch())
                    .primaryLanguage(repo.getLanguage())
                    .starsCount(repo.getStarsCount())
                    .forksCount(repo.getForksCount())
                    .createdAtGithub(repo.getCreatedAtGithub() != null ? repo.getCreatedAtGithub().toString() : null)
                    .updatedAtGithub(repo.getUpdatedAtGithub() != null ? repo.getUpdatedAtGithub().toString() : null)
                    .pushedAtGithub(repo.getPushedAtGithub() != null ? repo.getPushedAtGithub().toString() : null)
                    .lastSyncedAt(repo.getLastSyncedAt() != null ? repo.getLastSyncedAt().toString() : null)
                    .languages(languages)
                    .recentCommits(commits)
                    .recentPullRequests(prs)
                    .build();
        }).toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Disconnect candidate's GitHub identity.
     */
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> disconnectGithub(@PathVariable UUID candidateId) {
        User currentUser = currentUserService.getCurrentUser();
        authorizeAccess(candidateId, currentUser);

        oauthService.disconnect(candidateId, currentUser);
        return ResponseEntity.ok(Map.of("message", "GitHub identity successfully disconnected."));
    }

    private void authorizeAccess(UUID candidateId, User currentUser) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));
        authorizationService.requireRole(currentUser, candidate.getOrganization().getId(), OrganizationRole.RECRUITER);
    }
}
