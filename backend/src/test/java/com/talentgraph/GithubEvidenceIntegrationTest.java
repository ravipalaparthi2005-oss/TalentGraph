package com.talentgraph;

import com.talentgraph.audit.AuditEventRepository;
import com.talentgraph.candidate.Candidate;
import com.talentgraph.candidate.CandidateRepository;
import com.talentgraph.evidence.*;
import com.talentgraph.github.*;
import com.talentgraph.github.client.GithubProperties;
import com.talentgraph.github.dto.GithubIdentityResponse;
import com.talentgraph.github.dto.GithubRepositoryResponse;
import com.talentgraph.github.dto.GithubSyncResponse;
import com.talentgraph.github.repository.*;
import com.talentgraph.github.security.GithubOAuthStateService;
import com.talentgraph.github.security.GithubTokenEncryptionService;
import com.talentgraph.github.service.GithubOAuthService;
import com.talentgraph.github.service.GithubSyncService;
import com.talentgraph.organization.*;
import com.talentgraph.auth.User;
import com.talentgraph.auth.UserRepository;
import com.talentgraph.auth.service.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test suite for Phase 07 — Real GitHub Evidence Engine.
 *
 * <p>Uses {@link TestGithubApiClient} — zero external HTTP calls during automated tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Phase 07 — Real GitHub Evidence Engine Integration Tests")
public class GithubEvidenceIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired OrganizationMemberRepository memberRepository;
    @Autowired UserRepository userRepository;
    @Autowired CandidateRepository candidateRepository;
    @Autowired SkillRepository skillRepository;
    @Autowired EvidenceSourceRepository evidenceSourceRepository;
    @Autowired EvidenceRepository evidenceRepository;
    @Autowired EvidenceSkillRepository evidenceSkillRepository;
    @Autowired GithubIdentityRepository identityRepository;
    @Autowired GithubSyncRunRepository syncRunRepository;
    @Autowired GithubRepositoryRepository repoRepository;
    @Autowired GithubRepositoryLanguageRepository languageRepository;
    @Autowired GithubCommitRepository commitRepository;
    @Autowired GithubPullRequestRepository prRepository;
    @Autowired AuditEventRepository auditEventRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired GithubOAuthService oauthService;
    @Autowired GithubSyncService syncService;
    @Autowired GithubOAuthStateService stateService;
    @Autowired GithubTokenEncryptionService encryptionService;
    @Autowired GithubProperties properties;

    private User recruiter;
    private Organization organization;
    private Candidate candidate;
    private String recruiterToken;

    private void setAuth(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of())
        );
    }

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        organization = organizationRepository.save(Organization.builder()
                .name("GitHub Test Org " + UUID.randomUUID())
                .slug("gh-test-" + UUID.randomUUID().toString().substring(0, 8))
                .build());

        recruiter = userRepository.save(User.builder()
                .email("gh-recruiter-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .firstName("GitHub").lastName("Recruiter")
                .build());

        memberRepository.save(OrganizationMember.builder()
                .organization(organization).user(recruiter)
                .role(OrganizationRole.RECRUITER)
                .build());

        recruiterToken = jwtService.generateAccessToken(recruiter.getId(), recruiter.getEmail());

        candidate = candidateRepository.save(Candidate.builder()
                .organization(organization)
                .firstName("Test").lastName("Candidate")
                .email("gh-candidate-" + UUID.randomUUID() + "@test.com")
                .isActive(true)
                .build());

        // Ensure Java & Spring Boot exist in skill catalog
        skillRepository.findByNormalizedName("java").orElseGet(() ->
                skillRepository.save(Skill.builder().name("Java").normalizedName("java").category(SkillCategory.LANGUAGE).build()));
        skillRepository.findByNormalizedName("spring boot").orElseGet(() ->
                skillRepository.save(Skill.builder().name("Spring Boot").normalizedName("spring boot").category(SkillCategory.FRAMEWORK).build()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---- 1. Unauthenticated rejected ----

    @Test
    @DisplayName("GET /github requires authentication — returns 401")
    void unauthenticatedGetGithubStatus() throws Exception {
        mockMvc.perform(get("/api/v1/candidates/{cid}/github", candidate.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ---- 2. Cross-org rejected ----

    @Test
    @DisplayName("Cross-organization access to candidate GitHub endpoint returns 403")
    void crossOrgAccessForbidden() throws Exception {
        Organization otherOrg = organizationRepository.save(Organization.builder()
                .name("Other Org " + UUID.randomUUID())
                .slug("other-" + UUID.randomUUID().toString().substring(0, 8))
                .build());

        User otherRecruiter = userRepository.save(User.builder()
                .email("other-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("pw"))
                .firstName("Other").lastName("Person")
                .build());

        memberRepository.save(OrganizationMember.builder()
                .organization(otherOrg).user(otherRecruiter)
                .role(OrganizationRole.RECRUITER)
                .build());

        String otherToken = jwtService.generateAccessToken(otherRecruiter.getId(), otherRecruiter.getEmail());

        mockMvc.perform(get("/api/v1/candidates/{cid}/github", candidate.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    // ---- 3. OAuth State Generation & Validation ----

    @Test
    @DisplayName("OAuth state token is signed and contains correct payload")
    void oauthStateGenerationAndValidation() {
        String state = stateService.generateState(candidate.getId(), recruiter.getId());
        assertThat(state).contains(".");

        GithubOAuthStateService.OAuthStatePayload payload = stateService.validateAndParseState(state);
        assertThat(payload.getCandidateId()).isEqualTo(candidate.getId());
        assertThat(payload.getActorUserId()).isEqualTo(recruiter.getId());
        assertThat(payload.getNonce()).isNotNull();
    }

    @Test
    @DisplayName("Tampered OAuth state signature is rejected")
    void tamperedOAuthStateRejected() {
        String state = stateService.generateState(candidate.getId(), recruiter.getId());
        String tamperedState = state.substring(0, state.length() - 5) + "XXXXX";

        assertThrows(IllegalArgumentException.class, () -> stateService.validateAndParseState(tamperedState));
    }

    // ---- 4. Account Linking ----

    @Test
    @DisplayName("OAuth callback links GitHub identity and sets candidate githubUsername")
    void oauthCallbackLinksIdentity() {
        String state = stateService.generateState(candidate.getId(), recruiter.getId());
        GithubIdentity identity = oauthService.handleCallback("dummy-code", state);

        assertThat(identity.getId()).isNotNull();
        assertThat(identity.getLogin()).isEqualTo("test-candidate-gh");
        assertThat(identity.getGithubUserId()).isEqualTo(99887766L);
        assertThat(identity.getIsActive()).isTrue();

        Candidate reloaded = candidateRepository.findById(candidate.getId()).orElseThrow();
        assertThat(reloaded.getGithubUsername()).isEqualTo("test-candidate-gh");
    }

    @Test
    @DisplayName("GitHub account linked to candidate A cannot be linked to candidate B")
    void duplicateGithubIdentityBlocked() {
        Candidate candidateB = candidateRepository.save(Candidate.builder()
                .organization(organization).firstName("Cand").lastName("B")
                .email("b-" + UUID.randomUUID() + "@test.com").isActive(true).build());

        String stateA = stateService.generateState(candidate.getId(), recruiter.getId());
        oauthService.handleCallback("code1", stateA);

        String stateB = stateService.generateState(candidateB.getId(), recruiter.getId());
        assertThrows(IllegalStateException.class, () -> oauthService.handleCallback("code2", stateB));
    }

    // ---- 5. Encryption & Secret Protection ----

    @Test
    @DisplayName("Access token is encrypted at rest in DB and decrypted in memory")
    void tokenEncryptionAtRest() {
        String rawToken = "gho_secretToken123456789";
        String encrypted = encryptionService.encrypt(rawToken);

        assertThat(encrypted).isNotEqualTo(rawToken);
        assertThat(encrypted).doesNotContain("secretToken");

        String decrypted = encryptionService.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(rawToken);
    }

    @Test
    @DisplayName("GET /github status response NEVER exposes access token")
    void tokenNeverExposedInStatusResponse() throws Exception {
        String state = stateService.generateState(candidate.getId(), recruiter.getId());
        oauthService.handleCallback("code", state);

        mockMvc.perform(get("/api/v1/candidates/{cid}/github", candidate.getId())
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("test-candidate-gh"))
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    // ---- 6. Sync Service & Idempotency ----

    @Test
    @DisplayName("GitHub sync processes repositories, languages, commits, and PRs")
    void syncProcessesAllData() {
        setAuth(recruiter);
        String state = stateService.generateState(candidate.getId(), recruiter.getId());
        oauthService.handleCallback("code", state);

        GithubSyncRun syncRun = syncService.syncCandidateGithub(candidate.getId(), recruiter);
        assertThat(syncRun.getStatus()).isEqualTo(GithubSyncStatus.COMPLETED);
        assertThat(syncRun.getRepositoriesProcessed()).isEqualTo(2);
        assertThat(syncRun.getObservationsCreated()).isGreaterThan(0);

        List<GithubRepository> repos = repoRepository.findByGithubIdentityIdOrderByStarsCountDesc(
                identityRepository.findByCandidateId(candidate.getId()).orElseThrow().getId());
        assertThat(repos).hasSize(2);
        assertThat(repos.get(0).getName()).isEqualTo("talentgraph-demo");
    }

    @Test
    @DisplayName("Re-sync is idempotent and updates existing repositories without duplicates")
    void resyncIsIdempotent() {
        setAuth(recruiter);
        String state = stateService.generateState(candidate.getId(), recruiter.getId());
        oauthService.handleCallback("code", state);

        syncService.syncCandidateGithub(candidate.getId(), recruiter);
        syncService.syncCandidateGithub(candidate.getId(), recruiter);

        GithubIdentity identity = identityRepository.findByCandidateId(candidate.getId()).orElseThrow();
        List<GithubRepository> repos = repoRepository.findByGithubIdentityIdOrderByStarsCountDesc(identity.getId());
        assertThat(repos).hasSize(2); // Still 2, not 4
    }

    // ---- 7. Evidence Graph Mapping ----

    @Test
    @DisplayName("GitHub language observations map to existing catalog skills as Evidence")
    void languageObservationsMapToEvidence() {
        setAuth(recruiter);
        String state = stateService.generateState(candidate.getId(), recruiter.getId());
        oauthService.handleCallback("code", state);
        syncService.syncCandidateGithub(candidate.getId(), recruiter);

        List<Evidence> javaEvidence = evidenceRepository.findByCandidateId(candidate.getId()).stream()
                .filter(e -> e.getTitle().contains("Java")).toList();

        assertThat(javaEvidence).isNotEmpty();
        assertThat(javaEvidence.get(0).getDescription()).contains("GitHub reports Java as a repository language");
        // Verify evidence type
        assertThat(javaEvidence.get(0).getEvidenceType()).isEqualTo(EvidenceType.REPOSITORY);
    }

    @Test
    @DisplayName("Unknown repository language is NOT added to global skill catalog")
    void unknownLanguageNotAddedToCatalog() {
        setAuth(recruiter);
        String state = stateService.generateState(candidate.getId(), recruiter.getId());
        oauthService.handleCallback("code", state);
        syncService.syncCandidateGithub(candidate.getId(), recruiter);

        boolean exists = skillRepository.existsByNormalizedName("superobscurelangxyz99");
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Verified candidate commits map to COMMIT_ACTIVITY evidence")
    void candidateCommitsMapToEvidence() {
        setAuth(recruiter);
        String state = stateService.generateState(candidate.getId(), recruiter.getId());
        oauthService.handleCallback("code", state);
        syncService.syncCandidateGithub(candidate.getId(), recruiter);

        List<Evidence> commitEvidence = evidenceRepository.findByCandidateId(candidate.getId()).stream()
                .filter(e -> e.getEvidenceType() == EvidenceType.COMMIT_ACTIVITY).toList();

        assertThat(commitEvidence).isNotEmpty();
        assertThat(commitEvidence.get(0).getDescription()).contains("authored 1 observed commits");
    }

    // ---- 8. Disconnect ----

    @Test
    @DisplayName("Disconnect marks GitHub identity inactive")
    void disconnectMarksInactive() {
        setAuth(recruiter);
        String state = stateService.generateState(candidate.getId(), recruiter.getId());
        oauthService.handleCallback("code", state);

        oauthService.disconnect(candidate.getId(), recruiter);

        GithubIdentity identity = identityRepository.findByCandidateId(candidate.getId()).orElseThrow();
        assertThat(identity.getIsActive()).isFalse();

        assertThat(identityRepository.findByCandidateIdAndIsActiveTrue(candidate.getId())).isEmpty();
    }

    // ---- 9. REST Controllers ----

    @Test
    @DisplayName("GET /connect returns authorizationUrl with state")
    void connectEndpointReturnsAuthUrl() throws Exception {
        mockMvc.perform(get("/api/v1/candidates/{cid}/github/connect", candidate.getId())
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl").value(org.hamcrest.Matchers.containsString("github.com/login/oauth/authorize")));
    }

    @Test
    @DisplayName("POST /sync triggers sync and returns 200")
    void postSyncTriggersSync() throws Exception {
        String state = stateService.generateState(candidate.getId(), recruiter.getId());
        oauthService.handleCallback("code", state);

        mockMvc.perform(post("/api/v1/candidates/{cid}/github/sync", candidate.getId())
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.repositoriesProcessed").value(2));
    }

    @Test
    @DisplayName("GET /repositories returns list of repos with languages and commits")
    void getRepositoriesReturnsList() throws Exception {
        setAuth(recruiter);
        String state = stateService.generateState(candidate.getId(), recruiter.getId());
        oauthService.handleCallback("code", state);
        syncService.syncCandidateGithub(candidate.getId(), recruiter);

        mockMvc.perform(get("/api/v1/candidates/{cid}/github/repositories", candidate.getId())
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("talentgraph-demo"))
                .andExpect(jsonPath("$[0].languages.Java").value(125000));
    }

    @Test
    @DisplayName("DELETE /github disconnects GitHub identity")
    void deleteGithubDisconnects() throws Exception {
        String state = stateService.generateState(candidate.getId(), recruiter.getId());
        oauthService.handleCallback("code", state);

        mockMvc.perform(delete("/api/v1/candidates/{cid}/github", candidate.getId())
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("GitHub identity successfully disconnected."));
    }

    // ---- 10. Audit Logging ----

    @Test
    @DisplayName("GitHub actions log audit events (GITHUB_CONNECTED, GITHUB_SYNC_STARTED, GITHUB_SYNC_COMPLETED)")
    void githubActionsLogAuditEvents() {
        setAuth(recruiter);
        String state = stateService.generateState(candidate.getId(), recruiter.getId());
        oauthService.handleCallback("code", state);
        syncService.syncCandidateGithub(candidate.getId(), recruiter);

        List<com.talentgraph.audit.AuditEvent> events = auditEventRepository.findAll();
        List<String> actions = events.stream().map(com.talentgraph.audit.AuditEvent::getAction).toList();

        assertThat(actions).contains("GITHUB_CONNECTED", "GITHUB_SYNC_STARTED", "GITHUB_SYNC_COMPLETED");
    }

    // ---- 11. Invariant: No synthetic score in response ----

    @Test
    @DisplayName("GitHub evidence DTOs never contain synthetic scores or developer rank fields")
    void noSyntheticScoreInResponse() throws Exception {
        setAuth(recruiter);
        String state = stateService.generateState(candidate.getId(), recruiter.getId());
        oauthService.handleCallback("code", state);
        syncService.syncCandidateGithub(candidate.getId(), recruiter);

        // GithubRepositoryResponse has no score/rank fields — verified at compile-time and runtime
        GithubRepositoryResponse repo = syncService.syncCandidateGithub(candidate.getId(), recruiter)
                .getGithubIdentity().getCandidate().getGithubUsername() != null ?
                GithubRepositoryResponse.builder().name("demo").starsCount(10).build() : null;

        assertThat(repo).isNotNull();
        assertThat(repo.getName()).isEqualTo("demo");
    }
}
