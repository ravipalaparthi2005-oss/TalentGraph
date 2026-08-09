package com.talentgraph;

import com.talentgraph.ai.AiExtractionResult;
import com.talentgraph.ai.TestAiProvider;
import com.talentgraph.ai.analysis.*;
import com.talentgraph.ai.openrouter.OpenRouterProperties;
import com.talentgraph.candidate.Candidate;
import com.talentgraph.candidate.CandidateRepository;
import com.talentgraph.document.*;
import com.talentgraph.evidence.*;
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
 * Integration tests for Phase 06 — AI Resume Intelligence.
 *
 * <p>Uses a test-profile {@link TestAiProvider} that never makes real HTTP calls.
 * All test classes share a single Spring context via the "test" profile.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Phase 06 — AI Resume Intelligence Integration Tests")
public class AiResumeIntelligenceTest {

    @Autowired MockMvc mockMvc;
    @Autowired CandidateRepository candidateRepository;
    @Autowired CandidateDocumentRepository documentRepository;
    @Autowired ResumeParsedContentRepository parsedContentRepository;
    @Autowired AiAnalysisRunRepository runRepository;
    @Autowired AiExtractedSkillRepository skillProposalRepository;
    @Autowired AiExtractedExperienceRepository experienceRepository;
    @Autowired EvidenceRepository evidenceRepository;
    @Autowired SkillRepository skillRepository;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired OrganizationMemberRepository memberRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired OpenRouterProperties openRouterProperties;
    @Autowired ResumeAiAnalysisService analysisService;
    @Autowired TestAiProvider testAiProvider;

    private User recruiter;
    private Organization organization;
    private Candidate candidate;
    private CandidateDocument processedDocument;
    private String recruiterToken;

    /**
     * Resume text — AI mock quotes MUST be substrings of this for validation to pass.
     */
    static final String RESUME_TEXT = """
            Software Engineer at Acme Corp 2022-2024
            Led backend services using Spring Boot and PostgreSQL.
            Built REST APIs using Spring Boot for microservices architecture.
            B.Tech in Computer Science from State University 2018-2022.
            Used SomeFakeSkillXYZ123 in production but skill not in catalog.
            """;

    /** Set the authenticated user in the Spring Security context (for direct service calls). */
    private void setAuth(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of())
        );
    }

    /** Configure the TestAiProvider with a rich extraction result for the current test. */
    private void configureRichMockResult() {
        testAiProvider.setNextResult(AiExtractionResult.builder()
                .summary("Engineer with Spring Boot experience.")
                .skills(List.of(
                        AiExtractionResult.ExtractedSkill.builder()
                                .name("Spring Boot")
                                .evidenceQuote("Built REST APIs using Spring Boot")
                                .sourceSection("Projects").build(),
                        AiExtractionResult.ExtractedSkill.builder()
                                .name("SomeFakeSkillXYZ123")
                                .evidenceQuote("Used SomeFakeSkillXYZ123 in production but skill not in catalog")
                                .sourceSection("Skills").build()
                ))
                .experiences(List.of(
                        AiExtractionResult.ExtractedExperience.builder()
                                .company("Acme Corp").jobTitle("Software Engineer")
                                .startDate("2022").endDate("2024")
                                .responsibilities("Led backend services")
                                .evidenceQuote("Software Engineer at Acme Corp").build()
                ))
                .projects(List.of(
                        AiExtractionResult.ExtractedProject.builder()
                                .projectName("TalentAPI").description("REST API")
                                .technologies("Spring Boot, PostgreSQL")
                                .evidenceQuote("Built REST APIs using Spring Boot").build()
                ))
                .education(List.of(
                        AiExtractionResult.ExtractedEducation.builder()
                                .institution("State University").degree("B.Tech")
                                .fieldOfStudy("Computer Science")
                                .startDate("2018").endDate("2022")
                                .evidenceQuote("B.Tech in Computer Science from State University").build()
                ))
                .certifications(List.of())
                .build());
    }

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        testAiProvider.setNextResult(null);

        organization = organizationRepository.save(Organization.builder()
                .name("AI Test Org " + UUID.randomUUID())
                .slug("ai-test-" + UUID.randomUUID().toString().substring(0, 8))
                .build());

        recruiter = userRepository.save(User.builder()
                .email("ai-recruiter-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .firstName("AI").lastName("Recruiter")
                .build());

        memberRepository.save(OrganizationMember.builder()
                .organization(organization).user(recruiter)
                .role(OrganizationRole.RECRUITER)
                .build());

        recruiterToken = jwtService.generateAccessToken(recruiter.getId(), recruiter.getEmail());

        candidate = candidateRepository.save(Candidate.builder()
                .organization(organization)
                .firstName("Test").lastName("Candidate")
                .email("ai-candidate-" + UUID.randomUUID() + "@test.com")
                .isActive(true)
                .build());

        processedDocument = documentRepository.save(CandidateDocument.builder()
                .candidate(candidate)
                .documentType(DocumentType.RESUME)
                .originalFilename("test-resume.pdf")
                .storedFilename("stored-key-" + UUID.randomUUID())
                .storageKey("stored-key-" + UUID.randomUUID())
                .mimeType("application/pdf").fileSizeBytes(1024L)
                .sha256Hash("abc123-" + UUID.randomUUID())
                .processingStatus(ProcessingStatus.PROCESSED)
                .uploadedBy(recruiter).uploadedAt(Instant.now()).processedAt(Instant.now())
                .build());

        parsedContentRepository.save(ResumeParsedContent.builder()
                .document(processedDocument).rawText(RESUME_TEXT)
                .parserVersion("1.0.0-test").characterCount(RESUME_TEXT.length())
                .build());

        // Use findOrCreate to avoid unique constraint violations with shared H2 context
        skillRepository.findByNormalizedName("spring boot").orElseGet(() ->
                skillRepository.save(Skill.builder()
                        .name("Spring Boot").normalizedName("spring boot")
                        .category(SkillCategory.FRAMEWORK).build()));

        openRouterProperties.setAnalysisEnabled(true);
        openRouterProperties.setApiKey("test-key");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        openRouterProperties.setAnalysisEnabled(true);
    }

    // ---- 1. Unauthenticated returns 401 ----

    @Test
    @DisplayName("AI analysis requires authentication — unauthenticated gets 401")
    void aiAnalysisRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/candidates/{cid}/documents/{did}/ai-analysis",
                        candidate.getId(), processedDocument.getId()))
                .andExpect(status().isUnauthorized()); // 401
    }

    // ---- 2. Cross-org access returns 403 ----

    @Test
    @DisplayName("Cross-organization analysis returns 403 Forbidden")
    void crossOrgAnalysisBlocked() throws Exception {
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

        mockMvc.perform(post("/api/v1/candidates/{cid}/documents/{did}/ai-analysis",
                        candidate.getId(), processedDocument.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    // ---- 3. Unprocessed document rejected ----

    @Test
    @DisplayName("Unprocessed document raises IllegalStateException from service")
    void unprocessedDocumentRejected() {
        setAuth(recruiter);
        CandidateDocument unprocessed = documentRepository.save(CandidateDocument.builder()
                .candidate(candidate).documentType(DocumentType.RESUME)
                .originalFilename("unprocessed.pdf")
                .storedFilename("key2-" + UUID.randomUUID())
                .storageKey("key2-" + UUID.randomUUID())
                .mimeType("application/pdf").fileSizeBytes(512L)
                .sha256Hash("xyz-" + UUID.randomUUID())
                .processingStatus(ProcessingStatus.PROCESSING)
                .uploadedBy(recruiter).uploadedAt(Instant.now())
                .build());

        assertThrows(IllegalStateException.class, () ->
                analysisService.analyze(candidate.getId(), unprocessed.getId(), false));
    }

    // ---- 4. Successful analysis creates COMPLETED run ----

    @Test
    @DisplayName("Successful AI analysis creates COMPLETED run")
    void successfulAnalysisCreatesCompletedRun() {
        setAuth(recruiter);
        configureRichMockResult();
        AiAnalysisResponse response = analysisService.analyze(
                candidate.getId(), processedDocument.getId(), false);

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getRunId()).isNotNull();
        assertThat(response.getProvider()).isEqualTo("test-no-op");
        assertThat(response.getPromptVersion()).isEqualTo("resume-extraction-v1");
    }

    // ---- 5. Validated skill ----

    @Test
    @DisplayName("Validated skill with catalog match stored as validated proposal")
    void validatedSkillStored() {
        setAuth(recruiter);
        configureRichMockResult();
        analysisService.analyze(candidate.getId(), processedDocument.getId(), false);

        List<AiExtractedSkill> proposals = skillProposalRepository.findAll().stream()
                .filter(s -> s.getSkillName().equals("Spring Boot")).toList();
        assertThat(proposals).isNotEmpty();
        assertThat(proposals.get(0).isValidated()).isTrue();
        assertThat(proposals.get(0).getCatalogSkillId()).isNotNull();
    }

    // ---- 6. Unknown skill NOT added to catalog ----

    @Test
    @DisplayName("Unknown skill stored as unvalidated — not added to catalog")
    void unknownSkillNotAddedToCatalog() {
        setAuth(recruiter);
        configureRichMockResult();
        analysisService.analyze(candidate.getId(), processedDocument.getId(), false);

        assertThat(skillRepository.existsByNormalizedName("somefakeskillxyz123")).isFalse();

        List<AiExtractedSkill> fakeProposals = skillProposalRepository.findAll().stream()
                .filter(s -> s.getSkillName().equals("SomeFakeSkillXYZ123")).toList();
        assertThat(fakeProposals).isNotEmpty();
        assertThat(fakeProposals.get(0).isValidated()).isFalse();
    }

    // ---- 7. Idempotency ----

    @Test
    @DisplayName("Idempotency: second analyze call returns same cached run")
    void idempotencyReturnsCachedResult() {
        setAuth(recruiter);
        configureRichMockResult();
        AiAnalysisResponse first = analysisService.analyze(candidate.getId(), processedDocument.getId(), false);

        setAuth(recruiter);
        configureRichMockResult();
        AiAnalysisResponse second = analysisService.analyze(candidate.getId(), processedDocument.getId(), false);

        assertThat(first.getRunId()).isEqualTo(second.getRunId());
        long runCount = runRepository.findAll().stream()
                .filter(r -> r.getDocument().getId().equals(processedDocument.getId())).count();
        assertThat(runCount).isEqualTo(1);
    }

    // ---- 8. Reanalyze creates new run ----

    @Test
    @DisplayName("reanalyze=true creates a new analysis run")
    void reanalyzeForcesNewRun() {
        setAuth(recruiter);
        configureRichMockResult();
        AiAnalysisResponse first = analysisService.analyze(candidate.getId(), processedDocument.getId(), false);

        setAuth(recruiter);
        configureRichMockResult();
        AiAnalysisResponse second = analysisService.analyze(candidate.getId(), processedDocument.getId(), true);

        assertThat(first.getRunId()).isNotEqualTo(second.getRunId());
    }

    // ---- 9. GET returns NOT_ANALYZED ----

    @Test
    @DisplayName("GET /ai-analysis returns NOT_ANALYZED when no run exists")
    void getAnalysisReturnsNotAnalyzed() throws Exception {
        mockMvc.perform(get("/api/v1/candidates/{cid}/documents/{did}/ai-analysis",
                        candidate.getId(), processedDocument.getId())
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_ANALYZED"));
    }

    // ---- 10. Quote validation: fake quote ----

    @Test
    @DisplayName("Quote NOT present in resume text is invalid")
    void quoteNotInResumeIsRejected() {
        assertThat(analysisService.isQuotePresent(
                "I know Java and Spring Boot",
                "Expert in Kubernetes orchestration")).isFalse();
    }

    // ---- 11. Quote validation: valid quote ----

    @Test
    @DisplayName("Quote present in resume text is valid")
    void validQuotePassesValidation() {
        assertThat(analysisService.isQuotePresent(
                "Built REST APIs using Spring Boot for microservices",
                "Built REST APIs using Spring Boot")).isTrue();
    }

    // ---- 12. Analysis disabled ----

    @Test
    @DisplayName("Analysis disabled returns UNAVAILABLE")
    void analysisDisabledReturnsUnavailable() {
        setAuth(recruiter);
        openRouterProperties.setAnalysisEnabled(false);
        AiAnalysisResponse response = analysisService.analyze(
                candidate.getId(), processedDocument.getId(), false);
        assertThat(response.getStatus()).isEqualTo("UNAVAILABLE");
        assertThat(response.isAnalysisEnabled()).isFalse();
    }

    // ---- 13. Run metadata ----

    @Test
    @DisplayName("Analysis run persisted with correct provider, model, promptVersion")
    void runPersistedWithCorrectMetadata() {
        setAuth(recruiter);
        configureRichMockResult();
        analysisService.analyze(candidate.getId(), processedDocument.getId(), false);

        AiAnalysisRun run = runRepository.findAll().stream()
                .filter(r -> r.getDocument().getId().equals(processedDocument.getId()))
                .findFirst().orElseThrow();

        assertThat(run.getProvider()).isEqualTo("test-no-op");
        assertThat(run.getPromptVersion()).isEqualTo("resume-extraction-v1");
        assertThat(run.getStatus()).isEqualTo(AiAnalysisStatus.COMPLETED);
        assertThat(run.getCompletedAt()).isNotNull();
    }

    // ---- 14. Experience proposals ----

    @Test
    @DisplayName("Experience proposals from AI are persisted")
    void experienceProposalsPersisted() {
        setAuth(recruiter);
        configureRichMockResult();
        analysisService.analyze(candidate.getId(), processedDocument.getId(), false);

        List<AiExtractedExperience> experiences = experienceRepository.findAll().stream()
                .filter(e -> e.getRun().getDocument().getId().equals(processedDocument.getId()))
                .toList();
        assertThat(experiences).isNotEmpty();
        assertThat(experiences.get(0).getCompany()).isEqualTo("Acme Corp");
    }

    // ---- 15. AI does not modify candidate ----

    @Test
    @DisplayName("AI analysis does not modify candidate identity fields")
    void aiDoesNotModifyCandidateIdentity() {
        setAuth(recruiter);
        String originalEmail = candidate.getEmail();
        configureRichMockResult();
        analysisService.analyze(candidate.getId(), processedDocument.getId(), false);

        Candidate reloaded = candidateRepository.findById(candidate.getId()).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo(originalEmail);
        assertThat(reloaded.getFirstName()).isEqualTo("Test");
    }

    // ---- 16. POST returns 200 ----

    @Test
    @DisplayName("POST /ai-analysis returns 200 for authenticated recruiter")
    void postAnalysisReturns200() throws Exception {
        configureRichMockResult();
        mockMvc.perform(post("/api/v1/candidates/{cid}/documents/{did}/ai-analysis",
                        candidate.getId(), processedDocument.getId())
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.promptVersion").value("resume-extraction-v1"));
    }

    // ---- 17. No score in response ----

    @Test
    @DisplayName("AI analysis response contains no score or ranking field")
    void noScoreOrRankingInResponse() {
        setAuth(recruiter);
        configureRichMockResult();
        AiAnalysisResponse response = analysisService.analyze(
                candidate.getId(), processedDocument.getId(), false);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        // AiAnalysisResponse has no score/rank fields — compile-time guarantee
    }

    // ---- 18. Short quote is invalid ----

    @Test
    @DisplayName("Quote shorter than 5 characters is treated as invalid")
    void shortQuoteIsInvalid() {
        assertThat(analysisService.isQuotePresent("Java Spring Boot developer", "Jav")).isFalse();
    }
}
