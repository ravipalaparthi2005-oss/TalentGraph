package com.talentgraph.github.controller;

import com.talentgraph.auth.User;
import com.talentgraph.auth.service.CurrentUserService;
import com.talentgraph.github.GithubIdentity;
import com.talentgraph.github.client.GithubProperties;
import com.talentgraph.github.dto.GithubIdentityResponse;
import com.talentgraph.github.service.GithubOAuthService;
import com.talentgraph.github.service.GithubSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for GitHub OAuth authorization initiation and callback handling.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class GithubOAuthController {

    private final GithubOAuthService oauthService;
    private final GithubSyncService syncService;
    private final com.talentgraph.auth.UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final GithubProperties properties;
    private final com.talentgraph.github.security.GithubOAuthStateService stateService;

    /**
     * Initiate GitHub OAuth connection for a candidate.
     */
    @GetMapping("/candidates/{candidateId}/github/connect")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> initiateConnect(@PathVariable UUID candidateId) {
        User currentUser = currentUserService.getCurrentUser();
        String authUrl = oauthService.getAuthorizationUrl(candidateId, currentUser);
        return ResponseEntity.ok(Map.of("authorizationUrl", authUrl));
    }

    /**
     * GitHub OAuth callback endpoint.
     */
    @GetMapping("/github/oauth/callback")
    public RedirectView handleOAuthCallback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        try {
            var payload = stateService.validateAndParseState(state);
            GithubIdentity identity = oauthService.handleCallback(code, state);

            // Trigger initial background sync asynchronously
            try {
                User actor = userRepository.findById(payload.getActorUserId()).orElse(null);
                if (actor != null) {
                    syncService.syncCandidateGithub(identity.getCandidate().getId(), actor);
                }
            } catch (Exception e) {
                log.warn("Initial post-link sync warning: {}", e.getMessage());
            }

            String targetUrl = properties.getRedirectUri().contains("localhost")
                    ? "http://localhost:5173/candidates/" + identity.getCandidate().getId() + "?github_connected=true"
                    : "/candidates/" + identity.getCandidate().getId() + "?github_connected=true";

            return new RedirectView(targetUrl);
        } catch (Exception e) {
            log.error("GitHub OAuth callback error", e);
            return new RedirectView("http://localhost:5173/candidates?error=" + e.getMessage());
        }
    }
}
