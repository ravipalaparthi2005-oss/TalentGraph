package com.talentgraph.assessment.service;

import com.talentgraph.audit.AuditEventService;
import com.talentgraph.auth.User;
import com.talentgraph.common.exception.ResourceNotFoundException;
import com.talentgraph.job.Job;
import com.talentgraph.job.JobRepository;
import com.talentgraph.organization.Organization;
import com.talentgraph.organization.OrganizationRole;
import com.talentgraph.organization.OrganizationRepository;
import com.talentgraph.organization.service.OrganizationAuthorizationService;
import com.talentgraph.assessment.*;
import com.talentgraph.assessment.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service managing coding assessments, questions, test cases, and publish validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodingAssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentTestCaseRepository testCaseRepository;
    private final OrganizationRepository organizationRepository;
    private final JobRepository jobRepository;
    private final OrganizationAuthorizationService authorizationService;
    private final AuditEventService auditEventService;

    /**
     * Create a new assessment in DRAFT status.
     */
    @Transactional
    public AssessmentResponse createAssessment(UUID orgId, CreateAssessmentRequest request, User currentUser) {
        authorizationService.requireRole(currentUser, orgId, OrganizationRole.RECRUITER);

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId));

        Job job = null;
        if (request.getJobId() != null) {
            job = jobRepository.findById(request.getJobId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + request.getJobId()));
            if (!job.getOrganization().getId().equals(orgId)) {
                throw new IllegalArgumentException("Job belongs to another organization.");
            }
        }

        Assessment assessment = Assessment.builder()
                .organization(org)
                .job(job)
                .name(request.getName().strip())
                .description(request.getDescription())
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 60)
                .status(AssessmentStatus.DRAFT)
                .createdBy(currentUser)
                .build();

        assessment = assessmentRepository.save(assessment);

        auditEventService.logEvent(
                org, currentUser,
                "Assessment", assessment.getId(),
                "ASSESSMENT_CREATED",
                String.format("{\"name\":\"%s\"}", assessment.getName())
        );

        return toResponse(assessment, List.of());
    }

    /**
     * Get assessment by ID (organization isolated).
     */
    @Transactional(readOnly = true)
    public AssessmentResponse getAssessment(UUID orgId, UUID assessmentId, User currentUser) {
        authorizationService.requireRole(currentUser, orgId, OrganizationRole.RECRUITER);

        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found: " + assessmentId));

        if (!assessment.getOrganization().getId().equals(orgId)) {
            throw new IllegalArgumentException("Assessment belongs to another organization.");
        }

        List<AssessmentQuestion> questions = questionRepository.findByAssessmentIdOrderByDisplayOrderAsc(assessmentId);
        return toResponse(assessment, questions);
    }

    /**
     * List organization assessments.
     */
    @Transactional(readOnly = true)
    public List<AssessmentResponse> getOrganizationAssessments(UUID orgId, User currentUser) {
        authorizationService.requireRole(currentUser, orgId, OrganizationRole.RECRUITER);

        List<Assessment> assessments = assessmentRepository.findByOrganizationId(orgId);
        return assessments.stream().map(a -> {
            List<AssessmentQuestion> questions = questionRepository.findByAssessmentIdOrderByDisplayOrderAsc(a.getId());
            return toResponse(a, questions);
        }).toList();
    }

    /**
     * Update assessment status (DRAFT -> PUBLISHED -> ARCHIVED).
     *
     * <p>Enforces Publish Validation:
     * - Valid non-blank name
     * - At least 1 question
     * - Total points > 0
     * - Every coding question must have at least 1 HIDDEN test case.
     */
    @Transactional
    public AssessmentResponse updateStatus(UUID orgId, UUID assessmentId, AssessmentStatus targetStatus, User currentUser) {
        authorizationService.requireRole(currentUser, orgId, OrganizationRole.RECRUITER);

        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found: " + assessmentId));

        if (!assessment.getOrganization().getId().equals(orgId)) {
            throw new IllegalArgumentException("Assessment belongs to another organization.");
        }

        if (targetStatus == AssessmentStatus.PUBLISHED) {
            validateAssessmentForPublish(assessment);
        }

        assessment.setStatus(targetStatus);
        assessment = assessmentRepository.save(assessment);

        auditEventService.logEvent(
                assessment.getOrganization(), currentUser,
                "Assessment", assessment.getId(),
                "ASSESSMENT_" + targetStatus.name(),
                String.format("{\"name\":\"%s\",\"status\":\"%s\"}", assessment.getName(), targetStatus.name())
        );

        List<AssessmentQuestion> questions = questionRepository.findByAssessmentIdOrderByDisplayOrderAsc(assessmentId);
        return toResponse(assessment, questions);
    }

    private void validateAssessmentForPublish(Assessment assessment) {
        if (assessment.getName() == null || assessment.getName().isBlank()) {
            throw new IllegalStateException("Assessment cannot be published without a valid name.");
        }

        List<AssessmentQuestion> questions = questionRepository.findByAssessmentIdOrderByDisplayOrderAsc(assessment.getId());
        if (questions.isEmpty()) {
            throw new IllegalStateException("Assessment cannot be published without at least one question.");
        }

        int totalPoints = questions.stream().mapToInt(AssessmentQuestion::getPoints).sum();
        if (totalPoints <= 0) {
            throw new IllegalStateException("Assessment total points must be greater than zero.");
        }

        for (AssessmentQuestion q : questions) {
            long hiddenCount = testCaseRepository.countByQuestionIdAndTestCaseType(q.getId(), TestCaseType.HIDDEN);
            if (hiddenCount == 0) {
                throw new IllegalStateException(String.format(
                        "Coding question '%s' cannot be published without at least one HIDDEN test case.", q.getTitle()));
            }
        }
    }

    private AssessmentResponse toResponse(Assessment a, List<AssessmentQuestion> questions) {
        int totalPoints = questions.stream().mapToInt(AssessmentQuestion::getPoints).sum();
        return AssessmentResponse.builder()
                .id(a.getId())
                .organizationId(a.getOrganization().getId())
                .jobId(a.getJob() != null ? a.getJob().getId() : null)
                .jobTitle(a.getJob() != null ? a.getJob().getTitle() : null)
                .name(a.getName())
                .description(a.getDescription())
                .durationMinutes(a.getDurationMinutes())
                .status(a.getStatus().name())
                .questionCount(questions.size())
                .totalPoints(totalPoints)
                .createdByEmail(a.getCreatedBy() != null ? a.getCreatedBy().getEmail() : null)
                .createdAt(a.getCreatedAt() != null ? a.getCreatedAt().toString() : null)
                .updatedAt(a.getUpdatedAt() != null ? a.getUpdatedAt().toString() : null)
                .build();
    }
}
