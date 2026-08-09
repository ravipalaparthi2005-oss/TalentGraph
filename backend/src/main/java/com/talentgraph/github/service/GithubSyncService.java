package com.talentgraph.github.service;

import com.talentgraph.audit.AuditEventService;
import com.talentgraph.auth.User;
import com.talentgraph.candidate.Candidate;
import com.talentgraph.candidate.CandidateRepository;
import com.talentgraph.common.exception.ResourceNotFoundException;
import com.talentgraph.github.*;
import com.talentgraph.github.client.GithubApiClient;
import com.talentgraph.github.client.GithubProperties;
import com.talentgraph.github.client.dto.*;
import com.talentgraph.github.repository.*;
import com.talentgraph.github.security.GithubTokenEncryptionService;
import com.talentgraph.organization.OrganizationRole;
import com.talentgraph.organization.service.OrganizationAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Service orchestrating GitHub evidence synchronization.
 *
 * <p>Idempotently fetches and upserts repository metadata, language distributions,
 * commit history, and pull requests without duplicating records.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GithubSyncService {

    private final GithubProperties properties;
    private final GithubApiClient githubApiClient;
    private final GithubTokenEncryptionService encryptionService;
    private final GithubIdentityRepository identityRepository;
    private final GithubSyncRunRepository syncRunRepository;
    private final GithubRepositoryRepository repoRepository;
    private final GithubRepositoryLanguageRepository languageRepository;
    private final GithubCommitRepository commitRepository;
    private final GithubPullRequestRepository prRepository;
    private final CandidateRepository candidateRepository;
    private final OrganizationAuthorizationService authorizationService;
    private final AuditEventService auditEventService;

    /**
     * Trigger GitHub synchronization for a candidate.
     */
    @Transactional
    public GithubSyncRun syncCandidateGithub(UUID candidateId, User currentUser) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        authorizationService.requireRole(currentUser, candidate.getOrganization().getId(), OrganizationRole.RECRUITER);

        GithubIdentity identity = identityRepository.findByCandidateIdAndIsActiveTrue(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("No active GitHub identity linked to candidate: " + candidateId));

        // Create PENDING sync run
        GithubSyncRun syncRun = GithubSyncRun.builder()
                .githubIdentity(identity)
                .status(GithubSyncStatus.RUNNING)
                .startedAt(Instant.now())
                .build();
        syncRun = syncRunRepository.save(syncRun);

        auditEventService.logEvent(
                candidate.getOrganization(), currentUser,
                "GithubSyncRun", syncRun.getId(),
                "GITHUB_SYNC_STARTED",
                String.format("{\"login\":\"%s\"}", identity.getLogin())
        );

        String accessToken = encryptionService.decrypt(identity.getAccessTokenEncrypted());
        int reposProcessed = 0;
        int observationsCreated = 0;

        try {
            // Fetch repositories (paginated up to maxRepositories)
            List<GithubRepoDto> remoteRepos = githubApiClient.listUserRepositories(
                    accessToken, 1, Math.min(properties.getMaxRepositories(), 100));

            for (GithubRepoDto repoDto : remoteRepos) {
                if (repoDto.getId() == null) continue;

                // Upsert repository
                GithubRepository repo = upsertRepository(identity, repoDto);
                reposProcessed++;
                observationsCreated++;

                // Sync language breakdown
                Map<String, Long> languages = githubApiClient.getRepositoryLanguages(accessToken, repo.getOwnerLogin(), repo.getName());
                for (Map.Entry<String, Long> entry : languages.entrySet()) {
                    upsertLanguage(repo, entry.getKey(), entry.getValue());
                    observationsCreated++;
                }

                // Sync recent commits
                List<GithubCommitDto> commitDtos = githubApiClient.listRepositoryCommits(
                        accessToken, repo.getOwnerLogin(), repo.getName(),
                        Math.min(properties.getMaxCommitsPerRepo(), 100));
                for (GithubCommitDto commitDto : commitDtos) {
                    if (commitDto.getSha() != null) {
                        upsertCommit(repo, commitDto);
                        observationsCreated++;
                    }
                }

                // Sync recent PRs
                List<GithubPullRequestDto> prDtos = githubApiClient.listRepositoryPullRequests(
                        accessToken, repo.getOwnerLogin(), repo.getName(), "all",
                        Math.min(properties.getMaxPullRequestsPerRepo(), 100));
                for (GithubPullRequestDto prDto : prDtos) {
                    if (prDto.getId() != null) {
                        upsertPullRequest(repo, prDto);
                        observationsCreated++;
                    }
                }
            }

            // Mark COMPLETED
            syncRun.setStatus(GithubSyncStatus.COMPLETED);
            syncRun.setCompletedAt(Instant.now());
            syncRun.setRepositoriesProcessed(reposProcessed);
            syncRun.setObservationsCreated(observationsCreated);
            syncRunRepository.save(syncRun);

            identity.setLastSyncedAt(Instant.now());
            identityRepository.save(identity);

            auditEventService.logEvent(
                    candidate.getOrganization(), currentUser,
                    "GithubSyncRun", syncRun.getId(),
                    "GITHUB_SYNC_COMPLETED",
                    String.format("{\"repos\":%d,\"observations\":%d}", reposProcessed, observationsCreated)
            );

            log.info("GitHub sync completed successfully: candidateId={} repos={} observations={}",
                    candidateId, reposProcessed, observationsCreated);
            return syncRun;

        } catch (Exception e) {
            log.error("GitHub sync failed for candidateId={}", candidateId, e);

            syncRun.setStatus(GithubSyncStatus.FAILED);
            syncRun.setCompletedAt(Instant.now());
            syncRun.setErrorCode("SYNC_ERROR");
            syncRun.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Synchronization failed.");
            syncRunRepository.save(syncRun);

            auditEventService.logEvent(
                    candidate.getOrganization(), currentUser,
                    "GithubSyncRun", syncRun.getId(),
                    "GITHUB_SYNC_FAILED",
                    String.format("{\"error\":\"%s\"}", e.getMessage())
            );

            return syncRun;
        }
    }

    // ---- Private Upsert Helpers ----

    private GithubRepository upsertRepository(GithubIdentity identity, GithubRepoDto dto) {
        Optional<GithubRepository> existing = repoRepository.findByGithubIdentityIdAndGithubRepositoryId(
                identity.getId(), dto.getId());

        GithubRepository repo;
        if (existing.isPresent()) {
            repo = existing.get();
            repo.setName(dto.getName());
            repo.setFullName(dto.getFullName());
            repo.setHtmlUrl(dto.getHtmlUrl());
            repo.setDescription(dto.getDescription());
            repo.setIsPrivate(Boolean.TRUE.equals(dto.getIsPrivate()));
            repo.setIsFork(Boolean.TRUE.equals(dto.getIsFork()));
            repo.setDefaultBranch(dto.getDefaultBranch());
            repo.setLanguage(dto.getLanguage());
            repo.setStarsCount(dto.getStargazersCount() != null ? dto.getStargazersCount() : 0);
            repo.setForksCount(dto.getForksCount() != null ? dto.getForksCount() : 0);
            repo.setUpdatedAtGithub(dto.getUpdatedAt());
            repo.setPushedAtGithub(dto.getPushedAt());
            repo.setLastSyncedAt(Instant.now());
        } else {
            repo = GithubRepository.builder()
                    .githubIdentity(identity)
                    .githubRepositoryId(dto.getId())
                    .ownerLogin(dto.getOwner() != null && dto.getOwner().getLogin() != null
                            ? dto.getOwner().getLogin() : identity.getLogin())
                    .name(dto.getName())
                    .fullName(dto.getFullName())
                    .htmlUrl(dto.getHtmlUrl())
                    .description(dto.getDescription())
                    .isPrivate(Boolean.TRUE.equals(dto.getIsPrivate()))
                    .isFork(Boolean.TRUE.equals(dto.getIsFork()))
                    .defaultBranch(dto.getDefaultBranch())
                    .language(dto.getLanguage())
                    .starsCount(dto.getStargazersCount() != null ? dto.getStargazersCount() : 0)
                    .forksCount(dto.getForksCount() != null ? dto.getForksCount() : 0)
                    .createdAtGithub(dto.getCreatedAt())
                    .updatedAtGithub(dto.getUpdatedAt())
                    .pushedAtGithub(dto.getPushedAt())
                    .lastSyncedAt(Instant.now())
                    .build();
        }
        return repoRepository.save(repo);
    }

    private void upsertLanguage(GithubRepository repo, String langName, Long bytes) {
        Optional<GithubRepositoryLanguage> existing = languageRepository.findByRepositoryIdAndLanguageName(
                repo.getId(), langName);

        GithubRepositoryLanguage lang;
        if (existing.isPresent()) {
            lang = existing.get();
            lang.setBytesCount(bytes);
            lang.setObservedAt(Instant.now());
        } else {
            lang = GithubRepositoryLanguage.builder()
                    .repository(repo)
                    .languageName(langName)
                    .bytesCount(bytes)
                    .observedAt(Instant.now())
                    .build();
        }
        languageRepository.save(lang);
    }

    private void upsertCommit(GithubRepository repo, GithubCommitDto dto) {
        Optional<GithubCommit> existing = commitRepository.findByRepositoryIdAndGithubCommitSha(
                repo.getId(), dto.getSha());

        if (existing.isPresent()) return; // Commits are immutable

        String authorLogin = dto.getAuthor() != null ? dto.getAuthor().getLogin() : null;
        String authorEmail = (dto.getCommit() != null && dto.getCommit().getAuthor() != null)
                ? dto.getCommit().getAuthor().getEmail() : null;
        String message = (dto.getCommit() != null && dto.getCommit().getMessage() != null)
                ? dto.getCommit().getMessage() : "Commit " + dto.getSha();
        Instant committedAt = (dto.getCommit() != null && dto.getCommit().getAuthor() != null)
                ? dto.getCommit().getAuthor().getDate() : null;

        GithubCommit commit = GithubCommit.builder()
                .repository(repo)
                .githubCommitSha(dto.getSha())
                .authorLogin(authorLogin)
                .authorEmail(authorEmail)
                .message(message)
                .commitUrl(dto.getHtmlUrl() != null ? dto.getHtmlUrl() : repo.getHtmlUrl() + "/commit/" + dto.getSha())
                .committedAt(committedAt)
                .observedAt(Instant.now())
                .build();

        commitRepository.save(commit);
    }

    private void upsertPullRequest(GithubRepository repo, GithubPullRequestDto dto) {
        Optional<GithubPullRequest> existing = prRepository.findByRepositoryIdAndGithubPrId(
                repo.getId(), dto.getId());

        GithubPullRequest pr;
        if (existing.isPresent()) {
            pr = existing.get();
            pr.setTitle(dto.getTitle());
            pr.setState(dto.getState());
            pr.setMergedAt(dto.getMergedAt());
            pr.setObservedAt(Instant.now());
        } else {
            String authorLogin = dto.getUser() != null ? dto.getUser().getLogin() : null;
            pr = GithubPullRequest.builder()
                    .repository(repo)
                    .githubPrId(dto.getId())
                    .number(dto.getNumber())
                    .title(dto.getTitle())
                    .state(dto.getState())
                    .authorLogin(authorLogin)
                    .htmlUrl(dto.getHtmlUrl())
                    .createdAtGithub(dto.getCreatedAt())
                    .mergedAt(dto.getMergedAt())
                    .observedAt(Instant.now())
                    .build();
        }
        prRepository.save(pr);
    }
}
