package com.talentgraph.github.service;

import com.talentgraph.audit.AuditEventService;
import com.talentgraph.auth.User;
import com.talentgraph.candidate.Candidate;
import com.talentgraph.candidate.CandidateRepository;
import com.talentgraph.common.exception.ResourceNotFoundException;
import com.talentgraph.github.GithubIdentity;
import com.talentgraph.github.client.GithubApiClient;
import com.talentgraph.github.client.GithubProperties;
import com.talentgraph.github.client.dto.GithubTokenResponseDto;
import com.talentgraph.github.client.dto.GithubUserDto;
import com.talentgraph.github.repository.GithubIdentityRepository;
import com.talentgraph.github.security.GithubOAuthStateService;
import com.talentgraph.github.security.GithubTokenEncryptionService;
import com.talentgraph.organization.OrganizationRole;
import com.talentgraph.organization.service.OrganizationAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for GitHub OAuth initiation, callback processing, and account linking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GithubOAuthService {

    private final GithubProperties properties;
    private final GithubApiClient githubApiClient;
    private final GithubOAuthStateService stateService;
    private final GithubTokenEncryptionService encryptionService;
    private final GithubIdentityRepository identityRepository;
    private final CandidateRepository candidateRepository;
    private final OrganizationAuthorizationService authorizationService;
    private final AuditEventService auditEventService;

    /**
     * Generate GitHub OAuth authorization URL for a candidate.
     */
    @Transactional(readOnly = true)
    public String getAuthorizationUrl(UUID candidateId, User currentUser) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        authorizationService.requireRole(currentUser, candidate.getOrganization().getId(), OrganizationRole.RECRUITER);

        String state = stateService.generateState(candidateId, currentUser.getId());
        String scope = "read:user,repo";

        return String.format("https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=%s&state=%s",
                URLEncoder.encode(properties.getClientId(), StandardCharsets.UTF_8),
                URLEncoder.encode(properties.getRedirectUri(), StandardCharsets.UTF_8),
                URLEncoder.encode(scope, StandardCharsets.UTF_8),
                URLEncoder.encode(state, StandardCharsets.UTF_8)
        );
    }

    /**
     * Process GitHub OAuth callback, exchange code, verify user, and link to Candidate.
     */
    @Transactional
    public GithubIdentity handleCallback(String code, String stateToken) {
        // 1. Validate state
        GithubOAuthStateService.OAuthStatePayload payload = stateService.validateAndParseState(stateToken);
        UUID candidateId = payload.getCandidateId();

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        // 2. Exchange code for access token
        GithubTokenResponseDto tokenResponse = githubApiClient.exchangeCodeForToken(code, stateToken);
        if (tokenResponse.getAccessToken() == null || tokenResponse.getAccessToken().isBlank()) {
            String errorMsg = tokenResponse.getErrorDescription() != null
                    ? tokenResponse.getErrorDescription()
                    : "Failed to obtain GitHub access token.";
            throw new IllegalArgumentException(errorMsg);
        }

        String rawToken = tokenResponse.getAccessToken();

        // 3. Retrieve authenticated GitHub user
        GithubUserDto githubUser = githubApiClient.getAuthenticatedUser(rawToken);
        if (githubUser == null || githubUser.getId() == null) {
            throw new IllegalStateException("Failed to retrieve GitHub user profile.");
        }

        // 4. Check uniqueness: account must not be linked to another candidate
        if (identityRepository.existsByGithubUserIdAndCandidateIdNotAndIsActiveTrue(githubUser.getId(), candidateId)) {
            throw new IllegalStateException(String.format(
                    "GitHub account '%s' is already linked to another candidate.", githubUser.getLogin()));
        }

        // 5. Encrypt token at rest
        String encryptedToken = encryptionService.encrypt(rawToken);

        // 6. Upsert GithubIdentity
        Optional<GithubIdentity> existingOpt = identityRepository.findByCandidateId(candidateId);
        GithubIdentity identity;
        if (existingOpt.isPresent()) {
            identity = existingOpt.get();
            identity.setGithubUserId(githubUser.getId());
            identity.setLogin(githubUser.getLogin());
            identity.setProfileUrl(githubUser.getHtmlUrl());
            identity.setAccessTokenEncrypted(encryptedToken);
            identity.setScope(tokenResponse.getScope());
            identity.setIsActive(true);
            identity.setConnectedAt(Instant.now());
        } else {
            identity = GithubIdentity.builder()
                    .candidate(candidate)
                    .githubUserId(githubUser.getId())
                    .login(githubUser.getLogin())
                    .profileUrl(githubUser.getHtmlUrl())
                    .accessTokenEncrypted(encryptedToken)
                    .scope(tokenResponse.getScope())
                    .isActive(true)
                    .connectedAt(Instant.now())
                    .build();
        }

        identity = identityRepository.save(identity);

        // Update candidate's githubUsername field with verified username
        candidate.setGithubUsername(githubUser.getLogin());
        candidateRepository.save(candidate);

        // Audit log
        auditEventService.logEvent(
                candidate.getOrganization(), null,
                "GithubIdentity", identity.getId(),
                "GITHUB_CONNECTED",
                String.format("{\"login\":\"%s\",\"githubUserId\":%d}", githubUser.getLogin(), githubUser.getId())
        );

        log.info("GitHub account successfully linked: login={} candidateId={}", githubUser.getLogin(), candidateId);
        return identity;
    }

    /**
     * Disconnect GitHub identity from candidate.
     */
    @Transactional
    public void disconnect(UUID candidateId, User currentUser) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        authorizationService.requireRole(currentUser, candidate.getOrganization().getId(), OrganizationRole.RECRUITER);

        GithubIdentity identity = identityRepository.findByCandidateIdAndIsActiveTrue(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Active GitHub identity not found for candidate."));

        identity.setIsActive(false);
        identityRepository.save(identity);

        auditEventService.logEvent(
                candidate.getOrganization(), currentUser,
                "GithubIdentity", identity.getId(),
                "GITHUB_DISCONNECTED",
                String.format("{\"login\":\"%s\"}", identity.getLogin())
        );

        log.info("GitHub identity disconnected for candidateId={}", candidateId);
    }
}
