package com.talentgraph.ai.analysis;

import com.talentgraph.ai.AiExtractionResult;
import com.talentgraph.ai.AiProvider;
import com.talentgraph.ai.AiProviderException;
import com.talentgraph.ai.openrouter.OpenRouterProperties;
import com.talentgraph.audit.AuditEventService;
import com.talentgraph.auth.User;
import com.talentgraph.auth.service.CurrentUserService;
import com.talentgraph.candidate.Candidate;
import com.talentgraph.candidate.CandidateRepository;
import com.talentgraph.common.exception.ResourceNotFoundException;
import com.talentgraph.document.CandidateDocument;
import com.talentgraph.document.CandidateDocumentRepository;
import com.talentgraph.document.ProcessingStatus;
import com.talentgraph.document.ResumeParsedContent;
import com.talentgraph.document.ResumeParsedContentRepository;
import com.talentgraph.evidence.*;
import com.talentgraph.organization.OrganizationRole;
import com.talentgraph.organization.service.OrganizationAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service for AI-powered resume intelligence.
 *
 * <p>Architecture invariants enforced here:
 * <ol>
 *   <li>AI provider is an extraction assistant — never an authority.</li>
 *   <li>Evidence quotes are verified against the actual source text before storage.</li>
 *   <li>Skills are mapped to the existing catalog — unknown skills are stored as unvalidated proposals.</li>
 *   <li>No ATS score, ranking, fit score, or hiring recommendation is generated.</li>
 *   <li>Idempotency: same content + same prompt version + same model → skip re-analysis.</li>
 *   <li>The OpenRouter HTTP call occurs outside long-running DB transactions.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeAiAnalysisService {

    static final String ANALYSIS_TYPE_RESUME_EXTRACTION = "RESUME_EXTRACTION";
    static final String PROMPT_VERSION = "resume-extraction-v1";

    private final AiProvider aiProvider;
    private final OpenRouterProperties openRouterProperties;
    private final AiAnalysisRunRepository runRepository;
    private final AiExtractedSkillRepository skillProposalRepository;
    private final AiExtractedExperienceRepository experienceRepository;
    private final AiExtractedProjectRepository projectRepository;
    private final AiExtractedEducationRepository educationRepository;
    private final AiExtractedCertificationRepository certificationRepository;
    private final CandidateDocumentRepository documentRepository;
    private final ResumeParsedContentRepository parsedContentRepository;
    private final CandidateRepository candidateRepository;
    private final SkillRepository skillRepository;
    private final EvidenceSourceRepository evidenceSourceRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceSkillRepository evidenceSkillRepository;
    private final OrganizationAuthorizationService authorizationService;
    private final CurrentUserService currentUserService;
    private final AuditEventService auditEventService;

    /**
     * Trigger AI analysis for a candidate document.
     *
     * @param candidateId  the candidate owning the document
     * @param documentId   the document to analyze
     * @param reanalyze    when true, always create a new run even if a matching one exists
     * @return analysis response including run status and extracted proposals
     */
    @Transactional
    public AiAnalysisResponse analyze(UUID candidateId, UUID documentId, boolean reanalyze) {

        if (!openRouterProperties.isAnalysisEnabled()) {
            return AiAnalysisResponse.builder()
                    .analysisEnabled(false)
                    .status("UNAVAILABLE")
                    .errorMessage("AI analysis is disabled in server configuration.")
                    .build();
        }

        // 1. Auth: verify recruiter access to this candidate's organization
        User currentUser = currentUserService.getCurrentUser();
        CandidateDocument document = loadAndAuthorizeDocument(candidateId, documentId, currentUser);

        // 2. Document must be fully processed (text extracted) before AI analysis
        if (document.getProcessingStatus() != ProcessingStatus.PROCESSED) {
            throw new IllegalStateException(
                    "Document is not yet processed. Current status: " + document.getProcessingStatus());
        }

        // 3. Load parsed resume text
        ResumeParsedContent parsedContent = parsedContentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Parsed content not found for document: " + documentId));

        String resumeText = parsedContent.getRawText();
        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalStateException("Resume text is empty — cannot analyze.");
        }

        // 4. Calculate input hash for idempotency
        String inputHash = sha256(resumeText);
        String model = aiProvider.modelName();

        // 5. Check idempotency: skip if same content has already been successfully analyzed
        if (!reanalyze) {
            Optional<AiAnalysisRun> existing = runRepository.findMatchingCompletedRun(
                    documentId, inputHash, ANALYSIS_TYPE_RESUME_EXTRACTION, PROMPT_VERSION, model);
            if (existing.isPresent()) {
                log.info("AI analysis idempotency hit: runId={} documentId={}", existing.get().getId(), documentId);
                return buildResponse(existing.get());
            }
        }

        // 6. Create run record (PENDING)
        AiAnalysisRun run = AiAnalysisRun.builder()
                .document(document)
                .provider(aiProvider.providerName())
                .model(model)
                .analysisType(ANALYSIS_TYPE_RESUME_EXTRACTION)
                .status(AiAnalysisStatus.PENDING)
                .promptVersion(PROMPT_VERSION)
                .inputHash(inputHash)
                .build();
        run = runRepository.save(run);

        Candidate candidate = document.getCandidate();
        auditEventService.logEvent(
                candidate.getOrganization(), currentUser,
                "AiAnalysisRun", run.getId(),
                "AI_ANALYSIS_STARTED",
                String.format("{\"provider\":\"%s\",\"model\":\"%s\",\"promptVersion\":\"%s\"}",
                        aiProvider.providerName(), model, PROMPT_VERSION)
        );

        // 7. Mark RUNNING, update startedAt — flush before the external call
        run.setStatus(AiAnalysisStatus.RUNNING);
        run.setStartedAt(Instant.now());
        run = runRepository.save(run);

        // 8. Call AI provider (outside long-running transaction — this can take seconds)
        AiExtractionResult extractionResult;
        try {
            extractionResult = aiProvider.extractResumeInformation(resumeText, PROMPT_VERSION);
        } catch (AiProviderException e) {
            run.setStatus(AiAnalysisStatus.FAILED);
            run.setCompletedAt(Instant.now());
            run.setErrorCode(e.getErrorCode());
            run.setErrorMessage(e.getMessage());
            runRepository.save(run);

            auditEventService.logEvent(
                    candidate.getOrganization(), currentUser,
                    "AiAnalysisRun", run.getId(),
                    "AI_ANALYSIS_FAILED",
                    String.format("{\"errorCode\":\"%s\"}", e.getErrorCode())
            );
            log.warn("AI analysis failed: runId={} errorCode={}", run.getId(), e.getErrorCode());
            return buildResponse(run);
        }

        // 9. Validate and persist extraction results
        try {
            persistExtractionResults(run, candidate, document, resumeText, extractionResult);
        } catch (Exception e) {
            run.setStatus(AiAnalysisStatus.FAILED);
            run.setCompletedAt(Instant.now());
            run.setErrorCode("PERSISTENCE_ERROR");
            run.setErrorMessage("Failed to persist AI extraction results.");
            runRepository.save(run);
            log.error("AI analysis persistence error: runId={}", run.getId(), e);
            return buildResponse(run);
        }

        // 10. Mark COMPLETED
        run.setStatus(AiAnalysisStatus.COMPLETED);
        run.setCompletedAt(Instant.now());
        run = runRepository.save(run);

        auditEventService.logEvent(
                candidate.getOrganization(), currentUser,
                "AiAnalysisRun", run.getId(),
                "AI_ANALYSIS_COMPLETED",
                String.format("{\"provider\":\"%s\",\"model\":\"%s\"}", aiProvider.providerName(), model)
        );

        log.info("AI analysis completed: runId={} documentId={}", run.getId(), documentId);
        return buildResponse(run);
    }

    /**
     * Get the latest analysis run status for a document.
     */
    @Transactional(readOnly = true)
    public AiAnalysisResponse getLatestAnalysis(UUID candidateId, UUID documentId) {
        User currentUser = currentUserService.getCurrentUser();
        loadAndAuthorizeDocument(candidateId, documentId, currentUser);

        if (!openRouterProperties.isAnalysisEnabled()) {
            return AiAnalysisResponse.builder()
                    .analysisEnabled(false)
                    .status("UNAVAILABLE")
                    .build();
        }

        return runRepository.findTopByDocumentIdOrderByCreatedAtDesc(documentId)
                .map(this::buildResponse)
                .orElse(AiAnalysisResponse.builder()
                        .analysisEnabled(true)
                        .status("NOT_ANALYZED")
                        .build());
    }

    // ---- Validation + persistence ----

    private void persistExtractionResults(
            AiAnalysisRun run, Candidate candidate, CandidateDocument document,
            String resumeText, AiExtractionResult result) {

        // Create a dedicated EvidenceSource for AI-derived evidence
        EvidenceSource aiEvidenceSource = EvidenceSource.builder()
                .candidate(candidate)
                .sourceType(EvidenceSourceType.RESUME)
                .externalReference("AI:" + PROMPT_VERSION + ":" + document.getOriginalFilename())
                .sourceUrl("/api/v1/candidates/" + candidate.getId() + "/documents/" + document.getId() + "/ai-analysis")
                .collectedAt(Instant.now())
                .metadataJson(String.format("{\"runId\":\"%s\",\"provider\":\"%s\",\"model\":\"%s\",\"promptVersion\":\"%s\"}",
                        run.getId(), run.getProvider(), run.getModel(), run.getPromptVersion()))
                .build();
        aiEvidenceSource = evidenceSourceRepository.save(aiEvidenceSource);

        // Persist and validate skill proposals
        if (result.getSkills() != null) {
            for (AiExtractionResult.ExtractedSkill s : result.getSkills()) {
                if (s.getName() == null || s.getEvidenceQuote() == null) continue;

                boolean quoteValid = isQuotePresent(resumeText, s.getEvidenceQuote());
                if (!quoteValid) {
                    log.warn("AI quote validation failed for skill '{}' — quote not found in source text", s.getName());
                    // Still store the proposal as unvalidated; don't create Evidence
                    AiExtractedSkill proposal = AiExtractedSkill.builder()
                            .run(run).skillName(s.getName()).evidenceQuote(s.getEvidenceQuote())
                            .sourceSection(s.getSourceSection()).validated(false).build();
                    skillProposalRepository.save(proposal);
                    continue;
                }

                // Map to catalog
                String normalized = s.getName().toLowerCase().replaceAll("[^a-z0-9 .#+]", "").strip();
                Optional<Skill> catalogSkill = skillRepository.findByNormalizedName(normalized);

                AiExtractedSkill proposal = AiExtractedSkill.builder()
                        .run(run).skillName(s.getName()).evidenceQuote(s.getEvidenceQuote())
                        .sourceSection(s.getSourceSection())
                        .catalogSkillId(catalogSkill.map(Skill::getId).orElse(null))
                        .validated(catalogSkill.isPresent())
                        .build();
                skillProposalRepository.save(proposal);

                // Only create Evidence if catalog match exists
                if (catalogSkill.isPresent()) {
                    Evidence evidence = Evidence.builder()
                            .candidate(candidate)
                            .evidenceSource(aiEvidenceSource)
                            .title("AI Skill: " + catalogSkill.get().getName())
                            .description("Resume states: " + s.getEvidenceQuote())
                            .evidenceType(EvidenceType.SKILL_MENTION)
                            .observedValue(s.getName())
                            .normalizedValue(normalized)
                            .confidence(new BigDecimal("0.90"))
                            .occurredAt(Instant.now())
                            .sourceReference("AI:" + PROMPT_VERSION)
                            .build();
                    evidence = evidenceRepository.save(evidence);

                    EvidenceSkill edge = EvidenceSkill.builder()
                            .id(new EvidenceSkillId(evidence.getId(), catalogSkill.get().getId()))
                            .evidence(evidence).skill(catalogSkill.get())
                            .relationshipType(EvidenceRelationshipType.DEMONSTRATES)
                            .build();
                    evidenceSkillRepository.save(edge);
                }
            }
        }

        // Persist experience proposals (stored as-is; no catalog mapping needed)
        if (result.getExperiences() != null) {
            for (AiExtractionResult.ExtractedExperience e : result.getExperiences()) {
                if (e.getEvidenceQuote() == null) continue;
                experienceRepository.save(AiExtractedExperience.builder()
                        .run(run).company(e.getCompany()).jobTitle(e.getJobTitle())
                        .startDate(e.getStartDate()).endDate(e.getEndDate())
                        .responsibilities(e.getResponsibilities()).evidenceQuote(e.getEvidenceQuote()).build());
            }
        }

        // Persist project proposals
        if (result.getProjects() != null) {
            for (AiExtractionResult.ExtractedProject p : result.getProjects()) {
                if (p.getProjectName() == null || p.getEvidenceQuote() == null) continue;
                projectRepository.save(AiExtractedProject.builder()
                        .run(run).projectName(p.getProjectName()).description(p.getDescription())
                        .technologies(p.getTechnologies()).evidenceQuote(p.getEvidenceQuote()).build());
            }
        }

        // Persist education proposals
        if (result.getEducation() != null) {
            for (AiExtractionResult.ExtractedEducation ed : result.getEducation()) {
                if (ed.getEvidenceQuote() == null) continue;
                educationRepository.save(AiExtractedEducation.builder()
                        .run(run).institution(ed.getInstitution()).degree(ed.getDegree())
                        .fieldOfStudy(ed.getFieldOfStudy()).startDate(ed.getStartDate())
                        .endDate(ed.getEndDate()).grade(ed.getGrade()).evidenceQuote(ed.getEvidenceQuote()).build());
            }
        }

        // Persist certification proposals
        if (result.getCertifications() != null) {
            for (AiExtractionResult.ExtractedCertification c : result.getCertifications()) {
                if (c.getCertificationName() == null || c.getEvidenceQuote() == null) continue;
                certificationRepository.save(AiExtractedCertification.builder()
                        .run(run).certificationName(c.getCertificationName())
                        .issuingBody(c.getIssuingBody()).issuedDate(c.getIssuedDate())
                        .evidenceQuote(c.getEvidenceQuote()).build());
            }
        }
    }

    /**
     * Validate that the AI-provided evidence quote is actually present in the source text.
     * Normalizes whitespace before comparison to handle line breaks and extra spaces.
     * Public for unit testing.
     */
    public boolean isQuotePresent(String sourceText, String quote) {
        if (sourceText == null || quote == null || quote.isBlank()) return false;
        String normalizedSource = sourceText.replaceAll("\\s+", " ").toLowerCase();
        String normalizedQuote = quote.replaceAll("\\s+", " ").toLowerCase().strip();
        return normalizedQuote.length() >= 5 && normalizedSource.contains(normalizedQuote);
    }

    private CandidateDocument loadAndAuthorizeDocument(UUID candidateId, UUID documentId, User currentUser) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));
        authorizationService.requireRole(currentUser, candidate.getOrganization().getId(), OrganizationRole.RECRUITER);

        CandidateDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        if (!document.getCandidate().getId().equals(candidateId)) {
            throw new IllegalArgumentException("Document does not belong to this candidate.");
        }
        return document;
    }

    private AiAnalysisResponse buildResponse(AiAnalysisRun run) {
        List<AiExtractedSkill> skills = skillProposalRepository.findByRunId(run.getId());
        List<AiExtractedExperience> experiences = experienceRepository.findByRunId(run.getId());
        List<AiExtractedProject> projects = projectRepository.findByRunId(run.getId());
        List<AiExtractedEducation> education = educationRepository.findByRunId(run.getId());
        List<AiExtractedCertification> certs = certificationRepository.findByRunId(run.getId());

        return AiAnalysisResponse.builder()
                .runId(run.getId())
                .provider(run.getProvider())
                .model(run.getModel())
                .analysisType(run.getAnalysisType())
                .status(run.getStatus().name())
                .promptVersion(run.getPromptVersion())
                .analysisEnabled(openRouterProperties.isAnalysisEnabled())
                .startedAt(run.getStartedAt() != null ? run.getStartedAt().toString() : null)
                .completedAt(run.getCompletedAt() != null ? run.getCompletedAt().toString() : null)
                .errorCode(run.getErrorCode())
                .errorMessage(run.getErrorCode() != null ? "AI analysis could not be completed." : null)
                .skills(skills.stream().map(s -> AiAnalysisResponse.SkillResult.builder()
                        .id(s.getId()).skillName(s.getSkillName()).evidenceQuote(s.getEvidenceQuote())
                        .sourceSection(s.getSourceSection()).catalogSkillId(s.getCatalogSkillId())
                        .validated(s.isValidated()).build()).collect(Collectors.toList()))
                .experiences(experiences.stream().map(e -> AiAnalysisResponse.ExperienceResult.builder()
                        .id(e.getId()).company(e.getCompany()).jobTitle(e.getJobTitle())
                        .startDate(e.getStartDate()).endDate(e.getEndDate())
                        .responsibilities(e.getResponsibilities()).evidenceQuote(e.getEvidenceQuote())
                        .build()).collect(Collectors.toList()))
                .projects(projects.stream().map(p -> AiAnalysisResponse.ProjectResult.builder()
                        .id(p.getId()).projectName(p.getProjectName()).description(p.getDescription())
                        .technologies(p.getTechnologies()).evidenceQuote(p.getEvidenceQuote())
                        .build()).collect(Collectors.toList()))
                .education(education.stream().map(e -> AiAnalysisResponse.EducationResult.builder()
                        .id(e.getId()).institution(e.getInstitution()).degree(e.getDegree())
                        .fieldOfStudy(e.getFieldOfStudy()).startDate(e.getStartDate())
                        .endDate(e.getEndDate()).grade(e.getGrade()).evidenceQuote(e.getEvidenceQuote())
                        .build()).collect(Collectors.toList()))
                .certifications(certs.stream().map(c -> AiAnalysisResponse.CertificationResult.builder()
                        .id(c.getId()).certificationName(c.getCertificationName())
                        .issuingBody(c.getIssuingBody()).issuedDate(c.getIssuedDate())
                        .evidenceQuote(c.getEvidenceQuote()).build()).collect(Collectors.toList()))
                .build();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute input hash.", e);
        }
    }
}
