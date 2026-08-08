package com.talentgraph;

import com.talentgraph.audit.AuditEventRepository;
import com.talentgraph.auth.User;
import com.talentgraph.auth.UserRepository;
import com.talentgraph.auth.service.AuthenticationService;
import com.talentgraph.auth.service.JwtService;
import com.talentgraph.evaluation.EvaluationCriterionRepository;
import com.talentgraph.evaluation.EvaluationCriterionService;
import com.talentgraph.evaluation.dto.CreateCriterionRequest;
import com.talentgraph.evidence.Skill;
import com.talentgraph.evidence.SkillCategory;
import com.talentgraph.evidence.SkillRepository;
import com.talentgraph.evidence.SkillService;
import com.talentgraph.evidence.dto.CreateSkillRequest;
import com.talentgraph.job.*;
import com.talentgraph.job.dto.*;
import com.talentgraph.organization.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JobManagementTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository organizationMemberRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobRequirementRepository requirementRepository;

    @Autowired
    private EvaluationCriterionRepository criterionRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobRequirementService requirementService;

    @Autowired
    private EvaluationCriterionService criterionService;

    @Autowired
    private SkillService skillService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User recruiterUser;
    private User interviewerUser;
    private Organization orgA;
    private Organization orgB;
    private String recruiterToken;
    private String interviewerToken;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        criterionRepository.deleteAll();
        requirementRepository.deleteAll();
        jobRepository.deleteAll();
        skillRepository.deleteAll();
        organizationMemberRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();

        orgA = organizationRepository.save(Organization.builder().name("Acme Corp").slug("acme-corp-" + UUID.randomUUID()).build());
        orgB = organizationRepository.save(Organization.builder().name("Stark Tech").slug("stark-tech-" + UUID.randomUUID()).build());

        recruiterUser = userRepository.save(User.builder().email("recruiter@acme.com").passwordHash(passwordEncoder.encode("Pass12345")).firstName("Recruiter").lastName("User").build());
        interviewerUser = userRepository.save(User.builder().email("interviewer@acme.com").passwordHash(passwordEncoder.encode("Pass12345")).firstName("Interviewer").lastName("User").build());

        organizationMemberRepository.save(OrganizationMember.builder().organization(orgA).user(recruiterUser).role(OrganizationRole.RECRUITER).build());
        organizationMemberRepository.save(OrganizationMember.builder().organization(orgA).user(interviewerUser).role(OrganizationRole.INTERVIEWER).build());

        recruiterToken = jwtService.generateAccessToken(recruiterUser.getId(), recruiterUser.getEmail());
        interviewerToken = jwtService.generateAccessToken(interviewerUser.getId(), interviewerUser.getEmail());

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    private void setAuth(User user) {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user, null, java.util.List.of())
        );
    }

    @Test
    @DisplayName("1. Authenticated Recruiter can create a DRAFT job")
    void testAuthenticatedRecruiterCanCreateJob() throws Exception {
        String payload = """
            {
              "title": "Senior Backend Engineer",
              "department": "Engineering",
              "location": "Remote",
              "employmentType": "FULL_TIME",
              "description": "Build high-throughput Spring Boot microservices.",
              "organizationId": "%s"
            }
            """.formatted(orgA.getId());

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Senior Backend Engineer"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.organizationId").value(orgA.getId().toString()));
    }

    @Test
    @DisplayName("2. Unauthenticated user cannot create a job (401 Unauthorized)")
    void testUnauthenticatedUserCannotCreateJob() throws Exception {
        String payload = """
            {
              "title": "Senior Backend Engineer",
              "employmentType": "FULL_TIME",
              "description": "Job description",
              "organizationId": "%s"
            }
            """.formatted(orgA.getId());

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("3. Interviewer role cannot create a job (403 Forbidden)")
    void testInterviewerCannotCreateJob() throws Exception {
        String payload = """
            {
              "title": "Senior Backend Engineer",
              "employmentType": "FULL_TIME",
              "description": "Job description",
              "organizationId": "%s"
            }
            """.formatted(orgA.getId());

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + interviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Recruiter in Org A cannot access Org B's job (403 Forbidden IDOR Guard)")
    void testRecruiterCannotAccessAnotherOrgJob() throws Exception {
        Job jobB = jobRepository.save(Job.builder()
                .organization(orgB)
                .title("Stark AI Architect")
                .employmentType(EmploymentType.FULL_TIME)
                .description("Secret AI work")
                .status(JobStatus.DRAFT)
                .build());

        mockMvc.perform(get("/api/v1/jobs/" + jobB.getId())
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5. Recruiter can list only own organization's jobs")
    void testRecruiterCanListOnlyOwnOrgJobs() {
        setAuth(recruiterUser);
        jobRepository.save(Job.builder().organization(orgA).title("Job Org A").employmentType(EmploymentType.FULL_TIME).description("Desc A").status(JobStatus.OPEN).build());
        jobRepository.save(Job.builder().organization(orgB).title("Job Org B").employmentType(EmploymentType.FULL_TIME).description("Desc B").status(JobStatus.OPEN).build());

        Page<JobResponse> result = jobService.getJobs(orgA.getId(), null, null, null, 0, 20, "createdAt", "DESC");
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Job Org A");
    }

    @Test
    @DisplayName("6. Pagination enforces maximum page size limit (100)")
    void testPaginationEnforcesMaxSizeLimit() {
        setAuth(recruiterUser);
        Page<JobResponse> result = jobService.getJobs(orgA.getId(), null, null, null, 0, 500, "createdAt", "DESC");
        assertThat(result.getPageable().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("7. Status and employment type filtering works correctly")
    void testStatusAndEmploymentTypeFiltering() {
        setAuth(recruiterUser);
        jobRepository.save(Job.builder().organization(orgA).title("Full Time Open").employmentType(EmploymentType.FULL_TIME).description("D").status(JobStatus.OPEN).build());
        jobRepository.save(Job.builder().organization(orgA).title("Contract Open").employmentType(EmploymentType.CONTRACT).description("D").status(JobStatus.OPEN).build());
        jobRepository.save(Job.builder().organization(orgA).title("Full Time Draft").employmentType(EmploymentType.FULL_TIME).description("D").status(JobStatus.DRAFT).build());

        Page<JobResponse> result = jobService.getJobs(orgA.getId(), JobStatus.OPEN, EmploymentType.FULL_TIME, null, 0, 20, "createdAt", "DESC");
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Full Time Open");
    }

    @Test
    @DisplayName("8. Job search filtering operates against title, department, description")
    void testJobSearchFiltering() {
        setAuth(recruiterUser);
        jobRepository.save(Job.builder().organization(orgA).title("Java Dev").department("Core").employmentType(EmploymentType.FULL_TIME).description("Spring Boot").status(JobStatus.OPEN).build());
        jobRepository.save(Job.builder().organization(orgA).title("Frontend").department("UI").employmentType(EmploymentType.FULL_TIME).description("React Tailwind").status(JobStatus.OPEN).build());

        Page<JobResponse> searchJava = jobService.getJobs(orgA.getId(), null, null, "spring", 0, 20, "createdAt", "DESC");
        assertThat(searchJava.getContent()).hasSize(1);
        assertThat(searchJava.getContent().get(0).getTitle()).isEqualTo("Java Dev");
    }

    @Test
    @DisplayName("9. Invalid status transition (CLOSED -> OPEN) returns 409 Conflict")
    void testInvalidStatusTransitionReturns409() throws Exception {
        Job job = jobRepository.save(Job.builder().organization(orgA).title("Closed Job").employmentType(EmploymentType.FULL_TIME).description("D").status(JobStatus.CLOSED).build());

        String payload = """
            { "status": "OPEN" }
            """;

        mockMvc.perform(patch("/api/v1/jobs/" + job.getId() + "/status")
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Invalid job status transition from CLOSED to OPEN"));
    }

    @Test
    @DisplayName("10. Requirement belongs to specified job")
    void testRequirementBelongsToSpecifiedJob() {
        setAuth(recruiterUser);
        Job job = jobRepository.save(Job.builder().organization(orgA).title("Backend Job").employmentType(EmploymentType.FULL_TIME).description("D").status(JobStatus.DRAFT).build());
        CreateRequirementRequest req = CreateRequirementRequest.builder().name("Java 21").requirementType(RequirementType.REQUIRED).importance(Importance.HIGH).build();

        RequirementResponse created = requirementService.addRequirement(job.getId(), req);
        assertThat(created.getJobId()).isEqualTo(job.getId());
    }

    @Test
    @DisplayName("11. Cross-organization requirement access is rejected")
    void testCrossOrgRequirementAccessRejected() {
        setAuth(recruiterUser);
        Job jobB = jobRepository.save(Job.builder().organization(orgB).title("Org B Job").employmentType(EmploymentType.FULL_TIME).description("D").status(JobStatus.DRAFT).build());
        CreateRequirementRequest req = CreateRequirementRequest.builder().name("Java 21").requirementType(RequirementType.REQUIRED).importance(Importance.HIGH).build();

        assertThrows(Exception.class, () -> requirementService.addRequirement(jobB.getId(), req));
    }

    @Test
    @DisplayName("12. Duplicate skill normalization is rejected")
    void testDuplicateSkillNormalizationRejected() {
        setAuth(recruiterUser);
        skillService.createSkill(CreateSkillRequest.builder().name("Spring Boot").category(SkillCategory.FRAMEWORK).build());

        assertThrows(Exception.class, () ->
                skillService.createSkill(CreateSkillRequest.builder().name("  spring   boot  ").category(SkillCategory.FRAMEWORK).build())
        );
    }

    @Test
    @DisplayName("13. Evaluation criteria weight validation requires weight > 0 and <= 1.0")
    void testCriteriaWeightValidation() throws Exception {
        Job job = jobRepository.save(Job.builder().organization(orgA).title("Job").employmentType(EmploymentType.FULL_TIME).description("D").status(JobStatus.DRAFT).build());

        String invalidWeightPayload = """
            {
              "name": "System Design",
              "weight": 1.50
            }
            """;

        mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/criteria")
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidWeightPayload))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("14. Job cannot publish (DRAFT -> OPEN) without valid requirements")
    void testJobCannotPublishWithoutRequirements() {
        setAuth(recruiterUser);
        Job job = jobRepository.save(Job.builder().organization(orgA).title("Empty Requirements Job").employmentType(EmploymentType.FULL_TIME).description("Desc").status(JobStatus.DRAFT).build());

        assertThrows(IllegalArgumentException.class, () -> jobService.updateJobStatus(job.getId(), JobStatus.OPEN));
    }

    @Test
    @DisplayName("15. Job cannot publish if evaluation criteria weights do not sum to 1.00")
    void testJobCannotPublishWithInvalidCriteriaWeights() {
        setAuth(recruiterUser);
        Job job = jobRepository.save(Job.builder().organization(orgA).title("Invalid Criteria Job").employmentType(EmploymentType.FULL_TIME).description("Desc").status(JobStatus.DRAFT).build());
        requirementService.addRequirement(job.getId(), CreateRequirementRequest.builder().name("Java").requirementType(RequirementType.REQUIRED).importance(Importance.HIGH).build());

        criterionService.addCriterion(job.getId(), CreateCriterionRequest.builder().name("Criteria 1").weight(new BigDecimal("0.50")).build());

        assertThrows(IllegalArgumentException.class, () -> jobService.updateJobStatus(job.getId(), JobStatus.OPEN));
    }

    @Test
    @DisplayName("16. Audit event is created when a job is created")
    void testAuditEventCreatedOnJobCreation() {
        setAuth(recruiterUser);
        CreateJobRequest req = CreateJobRequest.builder()
                .title("Audited Job")
                .employmentType(EmploymentType.FULL_TIME)
                .description("Desc")
                .organizationId(orgA.getId())
                .build();

        JobResponse res = jobService.createJob(req, orgA.getId());

        assertThat(auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(orgA.getId()))
                .anyMatch(e -> "JOB_CREATED".equals(e.getAction()) && res.getId().equals(e.getEntityId()));
    }

    @Test
    @DisplayName("17. Audit event is created when a job is published")
    void testAuditEventCreatedOnJobPublish() {
        setAuth(recruiterUser);
        Job job = jobRepository.save(Job.builder().organization(orgA).title("Publishable Job").employmentType(EmploymentType.FULL_TIME).description("Desc").status(JobStatus.DRAFT).build());
        requirementService.addRequirement(job.getId(), CreateRequirementRequest.builder().name("Java").requirementType(RequirementType.REQUIRED).importance(Importance.HIGH).build());
        criterionService.addCriterion(job.getId(), CreateCriterionRequest.builder().name("Criteria 1").weight(new BigDecimal("1.00")).build());

        jobService.updateJobStatus(job.getId(), JobStatus.OPEN);

        assertThat(auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(orgA.getId()))
                .anyMatch(e -> "JOB_PUBLISHED".equals(e.getAction()) && job.getId().equals(e.getEntityId()));
    }
}
