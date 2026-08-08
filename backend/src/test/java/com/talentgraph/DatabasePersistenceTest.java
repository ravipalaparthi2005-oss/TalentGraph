package com.talentgraph;

import com.talentgraph.audit.AuditEvent;
import com.talentgraph.audit.AuditEventRepository;
import com.talentgraph.auth.User;
import com.talentgraph.auth.UserRepository;
import com.talentgraph.candidate.*;
import com.talentgraph.evidence.*;
import com.talentgraph.job.EmploymentType;
import com.talentgraph.job.Job;
import com.talentgraph.job.JobRepository;
import com.talentgraph.job.JobStatus;
import com.talentgraph.organization.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatabasePersistenceTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationMemberRepository organizationMemberRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EvidenceSourceRepository evidenceSourceRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private EvidenceSkillRepository evidenceSkillRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    @DisplayName("1. Organization can be persisted with UUID and slug")
    void testOrganizationPersistence() {
        Organization org = Organization.builder()
                .name("Acme Engineering")
                .slug("acme-eng-" + UUID.randomUUID())
                .build();
        Organization saved = organizationRepository.saveAndFlush(org);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(organizationRepository.findBySlug(saved.getSlug())).isPresent();
    }

    @Test
    @DisplayName("2. User can be persisted")
    void testUserPersistence() {
        User user = User.builder()
                .email("recruiter-" + UUID.randomUUID() + "@acme.com")
                .firstName("Ravi")
                .lastName("Kumar")
                .passwordHash("hashed_secret")
                .build();
        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findByEmail(saved.getEmail())).isPresent();
    }

    @Test
    @DisplayName("3. Organization membership connects User and Organization with Role")
    void testOrganizationMemberPersistence() {
        Organization org = organizationRepository.save(Organization.builder().name("TechCorp").slug("techcorp-" + UUID.randomUUID()).build());
        User user = userRepository.save(User.builder().email("admin-" + UUID.randomUUID() + "@techcorp.com").firstName("Jane").lastName("Doe").build());

        OrganizationMember member = OrganizationMember.builder()
                .organization(org)
                .user(user)
                .role(OrganizationRole.OWNER)
                .build();
        OrganizationMember saved = organizationMemberRepository.save(member);

        assertThat(saved.getId()).isNotNull();
        assertThat(organizationMemberRepository.existsByOrganizationIdAndUserId(org.getId(), user.getId())).isTrue();
    }

    @Test
    @DisplayName("4. Job belongs to Organization")
    void testJobBelongsToOrganization() {
        Organization org = organizationRepository.save(Organization.builder().name("FinTech Inc").slug("fintech-" + UUID.randomUUID()).build());
        Job job = Job.builder()
                .organization(org)
                .title("Senior Backend Engineer (Java)")
                .employmentType(EmploymentType.FULL_TIME)
                .status(JobStatus.OPEN)
                .build();
        Job saved = jobRepository.save(job);

        assertThat(saved.getId()).isNotNull();
        assertThat(jobRepository.findByOrganizationId(org.getId())).hasSize(1);
    }

    @Test
    @DisplayName("5. Candidate belongs to Organization")
    void testCandidateBelongsToOrganization() {
        Organization org = organizationRepository.save(Organization.builder().name("DevOps Corp").slug("devops-" + UUID.randomUUID()).build());
        Candidate candidate = Candidate.builder()
                .organization(org)
                .firstName("Ravi")
                .lastName("Kumar")
                .email("candidate-" + UUID.randomUUID() + "@talent.com")
                .githubUsername("ravikumar-dev")
                .build();
        Candidate saved = candidateRepository.save(candidate);

        assertThat(saved.getId()).isNotNull();
        assertThat(candidateRepository.findByOrganizationId(org.getId())).hasSize(1);
    }

    @Test
    @DisplayName("6. Application connects Candidate and Job")
    void testApplicationConnectsCandidateAndJob() {
        Organization org = organizationRepository.save(Organization.builder().name("Cloud Labs").slug("cloudlabs-" + UUID.randomUUID()).build());
        Job job = jobRepository.save(Job.builder().organization(org).title("Java Engineer").employmentType(EmploymentType.FULL_TIME).status(JobStatus.OPEN).build());
        Candidate candidate = candidateRepository.save(Candidate.builder().organization(org).firstName("Alex").lastName("Smith").email("alex-" + UUID.randomUUID() + "@example.com").build());

        Application app = Application.builder()
                .job(job)
                .candidate(candidate)
                .status(ApplicationStatus.NEW)
                .source(ApplicationSource.RECRUITER)
                .build();
        Application saved = applicationRepository.save(app);

        assertThat(saved.getId()).isNotNull();
        assertThat(applicationRepository.findByCandidateIdAndJobId(candidate.getId(), job.getId())).isPresent();
    }

    @Test
    @DisplayName("7. Evidence connects Candidate and EvidenceSource")
    void testEvidenceSourceAndEvidencePersistence() {
        Organization org = organizationRepository.save(Organization.builder().name("MetaTech").slug("metatech-" + UUID.randomUUID()).build());
        Candidate candidate = candidateRepository.save(Candidate.builder().organization(org).firstName("Sam").lastName("Wilson").email("sam-" + UUID.randomUUID() + "@metatech.com").build());

        EvidenceSource source = evidenceSourceRepository.save(EvidenceSource.builder()
                .candidate(candidate)
                .sourceType(EvidenceSourceType.GITHUB)
                .sourceUrl("https://github.com/sam/spring-boot-demo")
                .build());

        Evidence evidence = evidenceRepository.save(Evidence.builder()
                .candidate(candidate)
                .evidenceSource(source)
                .title("GitHub Repository Commit Activity")
                .evidenceType(EvidenceType.COMMIT_ACTIVITY)
                .observedValue("37 commits in Java repository")
                .confidence(new BigDecimal("0.95"))
                .build());

        assertThat(evidence.getId()).isNotNull();
        assertThat(evidenceRepository.findByEvidenceSourceId(source.getId())).hasSize(1);
    }

    @Test
    @DisplayName("8. Evidence connects to Skill via EvidenceSkill graph edge")
    void testEvidenceSkillRelationship() {
        Organization org = organizationRepository.save(Organization.builder().name("Data Systems").slug("datasys-" + UUID.randomUUID()).build());
        Candidate candidate = candidateRepository.save(Candidate.builder().organization(org).firstName("Maria").lastName("Garcia").email("maria-" + UUID.randomUUID() + "@datasys.com").build());
        EvidenceSource source = evidenceSourceRepository.save(EvidenceSource.builder().candidate(candidate).sourceType(EvidenceSourceType.RESUME).build());
        Evidence evidence = evidenceRepository.save(Evidence.builder().candidate(candidate).evidenceSource(source).title("Resume Parsed Skill").evidenceType(EvidenceType.SKILL_MENTION).build());
        Skill skill = skillRepository.save(Skill.builder().name("Spring Boot").normalizedName("spring-boot-" + UUID.randomUUID()).category(SkillCategory.FRAMEWORK).build());

        EvidenceSkillId edgeId = new EvidenceSkillId(evidence.getId(), skill.getId());
        EvidenceSkill edge = EvidenceSkill.builder()
                .id(edgeId)
                .evidence(evidence)
                .skill(skill)
                .relationshipType(EvidenceRelationshipType.DEMONSTRATES)
                .build();
        EvidenceSkill savedEdge = evidenceSkillRepository.save(edge);

        assertThat(savedEdge.getId()).isNotNull();
        assertThat(evidenceSkillRepository.findByIdSkillId(skill.getId())).hasSize(1);
    }

    @Test
    @DisplayName("9. Duplicate organization membership is rejected by database constraint")
    void testDuplicateOrganizationMemberRejection() {
        Organization org = organizationRepository.save(Organization.builder().name("UniqueOrg").slug("uniqueorg-" + UUID.randomUUID()).build());
        User user = userRepository.save(User.builder().email("dup-" + UUID.randomUUID() + "@unique.com").firstName("Dup").lastName("User").build());

        organizationMemberRepository.saveAndFlush(OrganizationMember.builder().organization(org).user(user).role(OrganizationRole.RECRUITER).build());

        assertThrows(DataIntegrityViolationException.class, () -> {
            organizationMemberRepository.saveAndFlush(OrganizationMember.builder().organization(org).user(user).role(OrganizationRole.ADMIN).build());
        });
    }

    @Test
    @DisplayName("10. Duplicate normalized skill name is rejected")
    void testDuplicateNormalizedSkillRejection() {
        String sharedNormalizedName = "java-21-" + UUID.randomUUID();
        skillRepository.saveAndFlush(Skill.builder().name("Java 21").normalizedName(sharedNormalizedName).category(SkillCategory.LANGUAGE).build());

        assertThrows(DataIntegrityViolationException.class, () -> {
            skillRepository.saveAndFlush(Skill.builder().name("Java 21 Edition").normalizedName(sharedNormalizedName).category(SkillCategory.LANGUAGE).build());
        });
    }

    @Test
    @DisplayName("11. Audit event can be persisted")
    void testAuditEventPersistence() {
        Organization org = organizationRepository.save(Organization.builder().name("AuditOrg").slug("auditorg-" + UUID.randomUUID()).build());
        User actor = userRepository.save(User.builder().email("actor-" + UUID.randomUUID() + "@audit.com").firstName("Actor").lastName("User").build());

        AuditEvent event = auditEventRepository.save(AuditEvent.builder()
                .organization(org)
                .actorUser(actor)
                .entityType("JOB")
                .entityId(UUID.randomUUID())
                .action("JOB_CREATED")
                .metadataJson("{\"title\": \"Senior Java Developer\"}")
                .build());

        assertThat(event.getId()).isNotNull();
        assertThat(auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(org.getId())).hasSize(1);
    }
}
