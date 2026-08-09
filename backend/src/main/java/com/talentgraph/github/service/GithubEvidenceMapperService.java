package com.talentgraph.github.service;

import com.talentgraph.candidate.Candidate;
import com.talentgraph.evidence.*;
import com.talentgraph.github.GithubCommit;
import com.talentgraph.github.GithubIdentity;
import com.talentgraph.github.GithubRepository;
import com.talentgraph.github.GithubRepositoryLanguage;
import com.talentgraph.github.repository.GithubCommitRepository;
import com.talentgraph.github.repository.GithubIdentityRepository;
import com.talentgraph.github.repository.GithubRepositoryLanguageRepository;
import com.talentgraph.github.repository.GithubRepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Maps raw GitHub observations to the core Evidence Graph.
 *
 * <p>Strict architectural rules:
 * <ul>
 *   <li>Maps observations ONLY to existing catalog skills — never creates new global skills.</li>
 *   <li>Phrases evidence factually: "GitHub reports Java as a repository language."</li>
 *   <li>Never generates synthetic scores or skill percentages.</li>
 *   <li>Enforces idempotency by checking external references before saving.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GithubEvidenceMapperService {

    private final GithubIdentityRepository identityRepository;
    private final GithubRepositoryRepository repoRepository;
    private final GithubRepositoryLanguageRepository languageRepository;
    private final GithubCommitRepository commitRepository;
    private final EvidenceSourceRepository evidenceSourceRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceSkillRepository evidenceSkillRepository;
    private final SkillRepository skillRepository;

    /**
     * Map all verified GitHub observations for a candidate to Evidence Graph nodes.
     */
    @Transactional
    public void mapGithubObservationsToEvidence(GithubIdentity identity) {
        Candidate candidate = identity.getCandidate();

        // 1. Get or create EvidenceSource for GitHub
        String externalRef = "GITHUB:account:" + identity.getGithubUserId();
        EvidenceSource source = evidenceSourceRepository.findByCandidateIdAndSourceTypeAndExternalReference(
                candidate.getId(), EvidenceSourceType.GITHUB, externalRef)
                .orElseGet(() -> evidenceSourceRepository.save(EvidenceSource.builder()
                        .candidate(candidate)
                        .sourceType(EvidenceSourceType.GITHUB)
                        .externalReference(externalRef)
                        .sourceUrl(identity.getProfileUrl())
                        .collectedAt(Instant.now())
                        .metadataJson(String.format("{\"login\":\"%s\",\"githubUserId\":%d}",
                                identity.getLogin(), identity.getGithubUserId()))
                        .build()));

        List<GithubRepository> repos = repoRepository.findByGithubIdentityIdOrderByStarsCountDesc(identity.getId());

        for (GithubRepository repo : repos) {
            mapRepositoryLanguages(candidate, source, repo);
            mapRepositoryCommits(candidate, source, identity, repo);
        }

        log.info("Mapped GitHub observations to Evidence Graph for candidateId={} login={}",
                candidate.getId(), identity.getLogin());
    }

    private void mapRepositoryLanguages(Candidate candidate, EvidenceSource source, GithubRepository repo) {
        List<GithubRepositoryLanguage> languages = languageRepository.findByRepositoryIdOrderByBytesCountDesc(repo.getId());

        for (GithubRepositoryLanguage lang : languages) {
            String normalized = lang.getLanguageName().toLowerCase().replaceAll("[^a-z0-9 .#+]", "").strip();
            Optional<Skill> catalogSkillOpt = skillRepository.findByNormalizedName(normalized);

            if (catalogSkillOpt.isEmpty()) {
                // Do NOT pollute global skill catalog with raw external language strings
                continue;
            }

            Skill skill = catalogSkillOpt.get();
            String evidenceRef = "GITHUB:lang:" + repo.getGithubRepositoryId() + ":" + normalized;

            // Check idempotency
            Optional<Evidence> existingEvidence = evidenceRepository.findByCandidateIdAndSourceReference(
                    candidate.getId(), evidenceRef);

            if (existingEvidence.isPresent()) {
                continue;
            }

            String description = String.format("GitHub reports %s as a repository language in %s (%d bytes observed).",
                    lang.getLanguageName(), repo.getFullName(), lang.getBytesCount());

            Evidence evidence = Evidence.builder()
                    .candidate(candidate)
                    .evidenceSource(source)
                    .title("GitHub Language: " + skill.getName())
                    .description(description)
                    .evidenceType(EvidenceType.REPOSITORY)
                    .observedValue(lang.getLanguageName())
                    .normalizedValue(normalized)
                    .confidence(new BigDecimal("0.95"))
                    .occurredAt(repo.getPushedAtGithub() != null ? repo.getPushedAtGithub() : Instant.now())
                    .sourceReference(evidenceRef)
                    .build();

            evidence = evidenceRepository.save(evidence);

            EvidenceSkill edge = EvidenceSkill.builder()
                    .id(new EvidenceSkillId(evidence.getId(), skill.getId()))
                    .evidence(evidence)
                    .skill(skill)
                    .relationshipType(EvidenceRelationshipType.DEMONSTRATES)
                    .build();

            evidenceSkillRepository.save(edge);
        }
    }

    private void mapRepositoryCommits(Candidate candidate, EvidenceSource source, GithubIdentity identity, GithubRepository repo) {
        List<GithubCommit> commits = commitRepository.findByRepositoryIdOrderByCommittedAtDesc(repo.getId());
        if (commits.isEmpty()) return;

        // Filter commits where author matches the candidate's GitHub login
        long candidateCommitCount = commits.stream()
                .filter(c -> c.getAuthorLogin() != null && c.getAuthorLogin().equalsIgnoreCase(identity.getLogin()))
                .count();

        if (candidateCommitCount == 0) return;

        String evidenceRef = "GITHUB:commits:" + repo.getGithubRepositoryId() + ":" + identity.getLogin();

        Optional<Evidence> existingEvidence = evidenceRepository.findByCandidateIdAndSourceReference(
                candidate.getId(), evidenceRef);

        if (existingEvidence.isPresent()) {
            Evidence evidence = existingEvidence.get();
            evidence.setDescription(String.format(
                    "Connected GitHub identity '%s' authored %d observed commits in repository %s.",
                    identity.getLogin(), candidateCommitCount, repo.getFullName()));
            evidenceRepository.save(evidence);
            return;
        }

        String description = String.format(
                "Connected GitHub identity '%s' authored %d observed commits in repository %s.",
                identity.getLogin(), candidateCommitCount, repo.getFullName());

        Evidence evidence = Evidence.builder()
                .candidate(candidate)
                .evidenceSource(source)
                .title("GitHub Commits: " + repo.getName())
                .description(description)
                .evidenceType(EvidenceType.COMMIT_ACTIVITY)
                .observedValue(String.valueOf(candidateCommitCount))
                .confidence(new BigDecimal("0.90"))
                .occurredAt(repo.getPushedAtGithub() != null ? repo.getPushedAtGithub() : Instant.now())
                .sourceReference(evidenceRef)
                .build();

        evidenceRepository.save(evidence);
    }
}
