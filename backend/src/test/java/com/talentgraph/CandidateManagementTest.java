package com.talentgraph;

import com.talentgraph.audit.AuditEventRepository;
import com.talentgraph.candidate.*;
import com.talentgraph.candidate.dto.CandidateResponse;
import com.talentgraph.candidate.dto.CreateApplicationRequest;
import com.talentgraph.candidate.dto.CreateCandidateRequest;
import com.talentgraph.document.*;
import com.talentgraph.document.dto.CandidateDocumentResponse;
import com.talentgraph.evidence.*;
import com.talentgraph.job.EmploymentType;
import com.talentgraph.job.Job;
import com.talentgraph.job.JobRepository;
import com.talentgraph.job.JobStatus;
import com.talentgraph.organization.Organization;
import com.talentgraph.organization.OrganizationMember;
import com.talentgraph.organization.OrganizationMemberRepository;
import com.talentgraph.organization.OrganizationRepository;
import com.talentgraph.organization.OrganizationRole;
import com.talentgraph.auth.User;
import com.talentgraph.auth.UserRepository;
import com.talentgraph.auth.service.JwtService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CandidateManagementTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private CandidateDocumentRepository documentRepository;

    @Autowired
    private ResumeParsedContentRepository parsedContentRepository;

    @Autowired
    private EvidenceSourceRepository evidenceSourceRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository organizationMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private SkillService skillService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private JwtService jwtService;

    private Organization orgA;
    private Organization orgB;
    private User recruiterA;
    private User recruiterB;
    private User interviewerA;
    private String recruiterAToken;
    private String recruiterBToken;
    private String interviewerAToken;
    private Skill javaSkill;
    private Skill springBootSkill;

    @BeforeEach
    void setUp() {
        orgA = organizationRepository.save(Organization.builder().name("Org Alpha").slug("org-alpha-" + UUID.randomUUID()).build());
        orgB = organizationRepository.save(Organization.builder().name("Org Beta").slug("org-beta-" + UUID.randomUUID()).build());

        recruiterA = userRepository.save(User.builder().firstName("Recruiter").lastName("Alpha").email("recruiter.a@" + UUID.randomUUID() + ".com").isActive(true).build());
        recruiterB = userRepository.save(User.builder().firstName("Recruiter").lastName("Beta").email("recruiter.b@" + UUID.randomUUID() + ".com").isActive(true).build());
        interviewerA = userRepository.save(User.builder().firstName("Interviewer").lastName("Alpha").email("interviewer.a@" + UUID.randomUUID() + ".com").isActive(true).build());

        organizationMemberRepository.save(OrganizationMember.builder().organization(orgA).user(recruiterA).role(OrganizationRole.RECRUITER).build());
        organizationMemberRepository.save(OrganizationMember.builder().organization(orgB).user(recruiterB).role(OrganizationRole.RECRUITER).build());
        organizationMemberRepository.save(OrganizationMember.builder().organization(orgA).user(interviewerA).role(OrganizationRole.INTERVIEWER).build());

        recruiterAToken = jwtService.generateAccessToken(recruiterA.getId(), recruiterA.getEmail());
        recruiterBToken = jwtService.generateAccessToken(recruiterB.getId(), recruiterB.getEmail());
        interviewerAToken = jwtService.generateAccessToken(interviewerA.getId(), interviewerA.getEmail());

        javaSkill = skillRepository.findByNormalizedName("java").orElseGet(() ->
                skillRepository.save(Skill.builder().name("Java").normalizedName("java").category(SkillCategory.LANGUAGE).build())
        );
        springBootSkill = skillRepository.findByNormalizedName("spring boot").orElseGet(() ->
                skillRepository.save(Skill.builder().name("Spring Boot").normalizedName("spring boot").category(SkillCategory.FRAMEWORK).build())
        );

        SecurityContextHolder.clearContext();
    }

    private void setAuth(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of())
        );
    }

    @Test
    @DisplayName("1. Recruiter creates a candidate")
    void testRecruiterCreatesCandidate() {
        setAuth(recruiterA);
        CreateCandidateRequest req = CreateCandidateRequest.builder()
                .firstName("Alice")
                .lastName("Smith")
                .email("alice.smith@example.com")
                .organizationId(orgA.getId())
                .build();

        CandidateResponse res = candidateService.createCandidate(req, orgA.getId());
        assertThat(res.getId()).isNotNull();
        assertThat(res.getFirstName()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("2. Candidate belongs to correct organization")
    void testCandidateBelongsToCorrectOrg() {
        setAuth(recruiterA);
        CreateCandidateRequest req = CreateCandidateRequest.builder()
                .firstName("Bob")
                .lastName("Jones")
                .email("bob.jones@example.com")
                .organizationId(orgA.getId())
                .build();

        CandidateResponse res = candidateService.createCandidate(req, orgA.getId());
        assertThat(res.getOrganizationId()).isEqualTo(orgA.getId());
    }

    @Test
    @DisplayName("3. Recruiter lists only own organization candidates")
    void testRecruiterListsOnlyOwnOrgCandidates() {
        setAuth(recruiterA);
        candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Cand").lastName("A").email("cand.a@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        setAuth(recruiterB);
        candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Cand").lastName("B").email("cand.b@example.com").organizationId(orgB.getId()).build(), orgB.getId());

        setAuth(recruiterA);
        Page<CandidateResponse> listA = candidateService.getCandidates(orgA.getId(), null, 0, 20, "createdAt", "DESC");
        assertThat(listA.getContent()).hasSize(1);
        assertThat(listA.getContent().get(0).getEmail()).isEqualTo("cand.a@example.com");
    }

    @Test
    @DisplayName("4. Cross-organization candidate access is rejected")
    void testCrossOrgCandidateAccessRejected() throws Exception {
        setAuth(recruiterA);
        CandidateResponse candA = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Cand").lastName("A").email("cand.a@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/candidates/" + candA.getId())
                        .header("Authorization", "Bearer " + recruiterBToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5. Candidate can be connected to job via Application")
    void testCandidateConnectedToJobViaApplication() {
        setAuth(recruiterA);
        Job job = jobRepository.save(Job.builder().organization(orgA).title("Backend Dev").employmentType(EmploymentType.FULL_TIME).status(JobStatus.OPEN).build());
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        applicationService.createApplication(job.getId(), CreateApplicationRequest.builder().candidateId(cand.getId()).build());

        List<com.talentgraph.candidate.dto.ApplicationResponse> apps = applicationService.getJobApplications(job.getId());
        assertThat(apps).hasSize(1);
        assertThat(apps.get(0).getCandidateId()).isEqualTo(cand.getId());
    }

    @Test
    @DisplayName("6. Cross-organization application creation is rejected")
    void testCrossOrgApplicationRejected() {
        setAuth(recruiterA);
        Job jobA = jobRepository.save(Job.builder().organization(orgA).title("Job A").employmentType(EmploymentType.FULL_TIME).status(JobStatus.OPEN).build());

        setAuth(recruiterB);
        CandidateResponse candB = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Bob").lastName("Beta").email("bob.beta@example.com").organizationId(orgB.getId()).build(), orgB.getId());

        setAuth(recruiterA);
        assertThrows(Exception.class, () ->
                applicationService.createApplication(jobA.getId(), CreateApplicationRequest.builder().candidateId(candB.getId()).build())
        );
    }

    @Test
    @DisplayName("7. Duplicate application to same job is rejected")
    void testDuplicateApplicationRejected() {
        setAuth(recruiterA);
        Job job = jobRepository.save(Job.builder().organization(orgA).title("Backend Dev").employmentType(EmploymentType.FULL_TIME).status(JobStatus.OPEN).build());
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        applicationService.createApplication(job.getId(), CreateApplicationRequest.builder().candidateId(cand.getId()).build());

        assertThrows(Exception.class, () ->
                applicationService.createApplication(job.getId(), CreateApplicationRequest.builder().candidateId(cand.getId()).build())
        );
    }

    @Test
    @DisplayName("8. Valid PDF resume upload is accepted")
    void testValidPdfResumeUploadAccepted() throws Exception {
        setAuth(recruiterA);
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        byte[] pdfContent = createValidPdfBytes("Alice Smith - Senior Java Developer with Spring Boot experience.");
        MockMultipartFile pdfFile = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdfContent);

        mockMvc.perform(multipart("/api/v1/candidates/" + cand.getId() + "/documents")
                        .file(pdfFile)
                        .header("Authorization", "Bearer " + recruiterAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processingStatus").value("PROCESSED"));
    }

    @Test
    @DisplayName("9. Valid DOCX resume upload is accepted")
    void testValidDocxResumeUploadAccepted() throws Exception {
        setAuth(recruiterA);
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        byte[] docxContent = createValidDocxBytes("Alice Smith - Senior Java Developer with Spring Boot experience.");
        MockMultipartFile docxFile = new MockMultipartFile("file", "resume.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxContent);

        mockMvc.perform(multipart("/api/v1/candidates/" + cand.getId() + "/documents")
                        .file(docxFile)
                        .header("Authorization", "Bearer " + recruiterAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processingStatus").value("PROCESSED"));
    }

    @Test
    @DisplayName("10. Unsupported file type (.exe) is rejected")
    void testUnsupportedFileTypeRejected() throws Exception {
        setAuth(recruiterA);
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        MockMultipartFile exeFile = new MockMultipartFile("file", "malware.exe", "application/x-msdownload", "MZ...exe".getBytes());

        mockMvc.perform(multipart("/api/v1/candidates/" + cand.getId() + "/documents")
                        .file(exeFile)
                        .header("Authorization", "Bearer " + recruiterAToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("11. Oversized file (>10MB) is rejected")
    void testOversizedFileRejected() throws Exception {
        setAuth(recruiterA);
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        byte[] hugeBytes = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile hugeFile = new MockMultipartFile("file", "huge.pdf", "application/pdf", hugeBytes);

        mockMvc.perform(multipart("/api/v1/candidates/" + cand.getId() + "/documents")
                        .file(hugeFile)
                        .header("Authorization", "Bearer " + recruiterAToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("12. Path traversal filename is sanitized")
    void testPathTraversalFilenameSanitized() throws Exception {
        setAuth(recruiterA);
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        byte[] pdfContent = createValidPdfBytes("Alice Smith - Senior Java Developer.");
        MockMultipartFile pdfFile = new MockMultipartFile("file", "../../../etc/passwd.pdf", "application/pdf", pdfContent);

        mockMvc.perform(multipart("/api/v1/candidates/" + cand.getId() + "/documents")
                        .file(pdfFile)
                        .header("Authorization", "Bearer " + recruiterAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.originalFilename").value(".._.._.._etc_passwd.pdf"));
    }

    @Test
    @DisplayName("13. SHA-256 hash is generated from raw bytes")
    void testSha256HashGeneratedFromRawBytes() throws Exception {
        setAuth(recruiterA);
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        byte[] pdfContent = createValidPdfBytes("Alice Smith - Senior Java Developer.");
        MockMultipartFile pdfFile = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdfContent);

        CandidateDocumentResponse docRes = documentService.uploadDocument(cand.getId(), null, pdfFile, DocumentType.RESUME);
        assertThat(docRes.getSha256Hash()).hasSize(64);
    }

    @Test
    @DisplayName("14. PDF text extraction extracts real plain text")
    void testPdfTextExtractionExtractsPlainText() throws Exception {
        setAuth(recruiterA);
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        byte[] pdfContent = createValidPdfBytes("Alice Smith - Senior Java Developer with Spring Boot experience.");
        MockMultipartFile pdfFile = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdfContent);

        CandidateDocumentResponse docRes = documentService.uploadDocument(cand.getId(), null, pdfFile, DocumentType.RESUME);
        assertThat(docRes.getRawText()).contains("Java").contains("Spring Boot");
    }

    @Test
    @DisplayName("15. DOCX text extraction extracts real plain text")
    void testDocxTextExtractionExtractsPlainText() throws Exception {
        setAuth(recruiterA);
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        byte[] docxContent = createValidDocxBytes("Alice Smith - Senior Java Developer with Spring Boot experience.");
        MockMultipartFile docxFile = new MockMultipartFile("file", "resume.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxContent);

        CandidateDocumentResponse docRes = documentService.uploadDocument(cand.getId(), null, docxFile, DocumentType.RESUME);
        assertThat(docRes.getRawText()).contains("Java").contains("Spring Boot");
    }

    @Test
    @DisplayName("16. Parsed resume text is persisted in database")
    void testParsedResumeTextPersistedInDatabase() throws Exception {
        setAuth(recruiterA);
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        byte[] pdfContent = createValidPdfBytes("Alice Smith - Senior Java Developer.");
        MockMultipartFile pdfFile = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdfContent);

        CandidateDocumentResponse docRes = documentService.uploadDocument(cand.getId(), null, pdfFile, DocumentType.RESUME);

        ResumeParsedContent content = parsedContentRepository.findByDocumentId(docRes.getId()).orElse(null);
        assertThat(content).isNotNull();
        assertThat(content.getRawText()).contains("Alice Smith");
    }

    @Test
    @DisplayName("17. Resume EvidenceSource is created")
    void testResumeEvidenceSourceCreated() throws Exception {
        setAuth(recruiterA);
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        byte[] pdfContent = createValidPdfBytes("Alice Smith - Senior Java Developer.");
        MockMultipartFile pdfFile = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdfContent);

        documentService.uploadDocument(cand.getId(), null, pdfFile, DocumentType.RESUME);

        List<EvidenceSource> sources = evidenceSourceRepository.findByCandidateIdAndSourceType(cand.getId(), EvidenceSourceType.RESUME);
        assertThat(sources).hasSize(1);
        assertThat(sources.get(0).getExternalReference()).isEqualTo("resume.pdf");
    }

    @Test
    @DisplayName("18. Evidence nodes reference the resume source")
    void testEvidenceNodesReferenceResumeSource() throws Exception {
        setAuth(recruiterA);
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        byte[] pdfContent = createValidPdfBytes("Alice Smith - Experienced Java engineer.");
        MockMultipartFile pdfFile = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdfContent);

        documentService.uploadDocument(cand.getId(), null, pdfFile, DocumentType.RESUME);

        List<Evidence> evidenceList = evidenceRepository.findByCandidateIdAndEvidenceType(cand.getId(), EvidenceType.SKILL_MENTION);
        assertThat(evidenceList).isNotEmpty();
        assertThat(evidenceList.get(0).getSourceReference()).isEqualTo("resume.pdf");
    }

    @Test
    @DisplayName("19. Detected skill maps to existing catalog skill")
    void testDetectedSkillMapsToCatalogSkill() throws Exception {
        setAuth(recruiterA);
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        byte[] pdfContent = createValidPdfBytes("Skills: Java, Spring Boot");
        MockMultipartFile pdfFile = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdfContent);

        documentService.uploadDocument(cand.getId(), null, pdfFile, DocumentType.RESUME);

        List<Evidence> evidenceList = evidenceRepository.findByCandidateIdAndEvidenceType(cand.getId(), EvidenceType.SKILL_MENTION);
        assertThat(evidenceList).anyMatch(e -> e.getObservedValue().equalsIgnoreCase("Java"));
        assertThat(evidenceList).anyMatch(e -> e.getObservedValue().equalsIgnoreCase("Spring Boot"));
    }

    @Test
    @DisplayName("20. Unsupported/absent skill is NOT invented")
    void testUnsupportedSkillNotInvented() throws Exception {
        setAuth(recruiterA);
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        byte[] pdfContent = createValidPdfBytes("Skills: Java");
        MockMultipartFile pdfFile = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdfContent);

        documentService.uploadDocument(cand.getId(), null, pdfFile, DocumentType.RESUME);

        List<Evidence> evidenceList = evidenceRepository.findByCandidateIdAndEvidenceType(cand.getId(), EvidenceType.SKILL_MENTION);
        assertThat(evidenceList).noneMatch(e -> e.getObservedValue().equalsIgnoreCase("Spring Boot"));
    }

    @Test
    @DisplayName("21. Processing failure is persisted when unparseable")
    void testProcessingFailurePersistedWhenUnparseable() throws Exception {
        setAuth(recruiterA);
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        // Corrupt PDF bytes (has valid PDF magic header %PDF- but corrupt internal xref stream)
        byte[] corruptPdf = "%PDF-1.4\nCorrupt PDF content without valid xref or trailer\n%%EOF".getBytes();
        MockMultipartFile corruptFile = new MockMultipartFile("file", "corrupt.pdf", "application/pdf", corruptPdf);

        CandidateDocumentResponse docRes = documentService.uploadDocument(cand.getId(), null, corruptFile, DocumentType.RESUME);

        assertThat(docRes.getProcessingStatus()).isEqualTo(ProcessingStatus.FAILED);
        assertThat(docRes.getProcessingError()).isNotNull();
    }

    @Test
    @DisplayName("22. Resume cannot be downloaded across organizations (403 Forbidden)")
    void testResumeCannotBeDownloadedCrossOrg() throws Exception {
        setAuth(recruiterA);
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        byte[] pdfContent = createValidPdfBytes("Alice Smith - Senior Java Developer.");
        MockMultipartFile pdfFile = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdfContent);
        CandidateDocumentResponse docRes = documentService.uploadDocument(cand.getId(), null, pdfFile, DocumentType.RESUME);

        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/candidates/" + cand.getId() + "/documents/" + docRes.getId() + "/download")
                        .header("Authorization", "Bearer " + recruiterBToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("23. Audit event is created for resume upload")
    void testAuditEventCreatedForResumeUpload() throws Exception {
        setAuth(recruiterA);
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        byte[] pdfContent = createValidPdfBytes("Alice Smith - Senior Java Developer.");
        MockMultipartFile pdfFile = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdfContent);
        documentService.uploadDocument(cand.getId(), null, pdfFile, DocumentType.RESUME);

        assertThat(auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(orgA.getId()))
                .anyMatch(e -> "RESUME_UPLOADED".equals(e.getAction()));
    }

    @Test
    @DisplayName("24. Audit event is created for successful processing")
    void testAuditEventCreatedForSuccessfulProcessing() throws Exception {
        setAuth(recruiterA);
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        byte[] pdfContent = createValidPdfBytes("Alice Smith - Senior Java Developer.");
        MockMultipartFile pdfFile = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdfContent);
        documentService.uploadDocument(cand.getId(), null, pdfFile, DocumentType.RESUME);

        assertThat(auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(orgA.getId()))
                .anyMatch(e -> "RESUME_PROCESSED".equals(e.getAction()));
    }

    @Test
    @DisplayName("25. Audit event is created for processing failure")
    void testAuditEventCreatedForProcessingFailure() throws Exception {
        setAuth(recruiterA);
        CandidateResponse cand = candidateService.createCandidate(CreateCandidateRequest.builder().firstName("Alice").lastName("Smith").email("alice@example.com").organizationId(orgA.getId()).build(), orgA.getId());

        byte[] corruptPdf = "%PDF-1.4\nCorrupt PDF content\n%%EOF".getBytes();
        MockMultipartFile corruptFile = new MockMultipartFile("file", "corrupt.pdf", "application/pdf", corruptPdf);
        documentService.uploadDocument(cand.getId(), null, corruptFile, DocumentType.RESUME);

        assertThat(auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(orgA.getId()))
                .anyMatch(e -> "RESUME_PROCESSING_FAILED".equals(e.getAction()));
    }

    private byte[] createValidPdfBytes(String textContent) {
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
            doc.addPage(page);
            try (org.apache.pdfbox.pdmodel.PDPageContentStream contents = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                contents.beginText();
                contents.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 12);
                contents.newLineAtOffset(100, 700);
                contents.showText(textContent);
                contents.endText();
            }
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Could not create mock PDF file", e);
        }
    }

    private byte[] createValidDocxBytes(String textContent) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             XWPFDocument doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText(textContent);
            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Could not create mock DOCX file", e);
        }
    }
}
