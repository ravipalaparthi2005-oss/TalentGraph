package com.talentgraph.job;

import com.talentgraph.audit.AuditEventService;
import com.talentgraph.auth.User;
import com.talentgraph.auth.service.CurrentUserService;
import com.talentgraph.common.exception.ConflictException;
import com.talentgraph.common.exception.ResourceNotFoundException;
import com.talentgraph.evaluation.EvaluationCriterion;
import com.talentgraph.evaluation.EvaluationCriterionRepository;
import com.talentgraph.job.dto.*;
import com.talentgraph.organization.Organization;
import com.talentgraph.organization.OrganizationMemberRepository;
import com.talentgraph.organization.OrganizationRepository;
import com.talentgraph.organization.OrganizationRole;
import com.talentgraph.organization.service.OrganizationAuthorizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobRequirementRepository requirementRepository;
    private final EvaluationCriterionRepository criterionRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final CurrentUserService currentUserService;
    private final OrganizationAuthorizationService authorizationService;
    private final AuditEventService auditEventService;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("createdAt", "updatedAt", "title");

    public JobService(
            JobRepository jobRepository,
            JobRequirementRepository requirementRepository,
            EvaluationCriterionRepository criterionRepository,
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            CurrentUserService currentUserService,
            OrganizationAuthorizationService authorizationService,
            AuditEventService auditEventService
    ) {
        this.jobRepository = jobRepository;
        this.requirementRepository = requirementRepository;
        this.criterionRepository = criterionRepository;
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public JobResponse createJob(CreateJobRequest request, UUID explicitOrgId) {
        User currentUser = currentUserService.getCurrentUser();
        UUID resolvedOrgId = request.getOrganizationId() != null ? request.getOrganizationId() : explicitOrgId;

        if (resolvedOrgId == null) {
            // Resolve primary organization if not supplied
            resolvedOrgId = organizationMemberRepository.findByUserId(currentUser.getId()).stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("User does not belong to any organization"))
                    .getOrganization().getId();
        }

        final UUID targetOrgId = resolvedOrgId;

        // Verify RECRUITER or higher permission
        authorizationService.requireRole(currentUser, targetOrgId, OrganizationRole.RECRUITER);

        Organization organization = organizationRepository.findById(targetOrgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization with ID " + targetOrgId + " not found"));

        Job job = Job.builder()
                .organization(organization)
                .title(request.getTitle().trim())
                .department(request.getDepartment() != null ? request.getDepartment().trim() : null)
                .location(request.getLocation() != null ? request.getLocation().trim() : null)
                .employmentType(request.getEmploymentType())
                .description(request.getDescription().trim())
                .status(JobStatus.DRAFT)
                .createdBy(currentUser)
                .build();

        Job savedJob = jobRepository.save(job);

        auditEventService.logEvent(
                organization,
                currentUser,
                "JOB",
                savedJob.getId(),
                "JOB_CREATED",
                "{\"title\":\"" + savedJob.getTitle() + "\"}"
        );

        return JobResponse.fromEntity(savedJob);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> getJobs(
            UUID organizationId,
            JobStatus status,
            EmploymentType employmentType,
            String search,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        User currentUser = currentUserService.getCurrentUser();
        authorizationService.requireOrganizationMember(currentUser, organizationId);

        int maxPageSize = Math.min(size, 100);
        String safeSortBy = ALLOWED_SORT_PROPERTIES.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, maxPageSize, Sort.by(direction, safeSortBy));

        Page<Job> jobsPage = jobRepository.findJobsFiltered(
                organizationId,
                status,
                employmentType,
                (search != null && !search.isBlank()) ? search.trim() : null,
                pageable
        );

        return jobsPage.map(JobResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(UUID jobId) {
        User currentUser = currentUserService.getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID " + jobId + " not found"));

        authorizationService.verifyResourceAccess(currentUser, job.getOrganization().getId());

        return JobResponse.fromEntity(job);
    }

    @Transactional
    public JobResponse updateJob(UUID jobId, UpdateJobRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID " + jobId + " not found"));

        authorizationService.requireRole(currentUser, job.getOrganization().getId(), OrganizationRole.RECRUITER);

        if (job.getStatus() == JobStatus.CLOSED) {
            throw new ConflictException("CLOSED jobs are read-only and cannot be updated");
        }

        job.setTitle(request.getTitle().trim());
        job.setDepartment(request.getDepartment() != null ? request.getDepartment().trim() : null);
        job.setLocation(request.getLocation() != null ? request.getLocation().trim() : null);
        job.setEmploymentType(request.getEmploymentType());
        job.setDescription(request.getDescription().trim());

        Job updatedJob = jobRepository.save(job);

        auditEventService.logEvent(
                job.getOrganization(),
                currentUser,
                "JOB",
                job.getId(),
                "JOB_UPDATED",
                "{\"title\":\"" + updatedJob.getTitle() + "\"}"
        );

        return JobResponse.fromEntity(updatedJob);
    }

    @Transactional
    public JobResponse updateJobStatus(UUID jobId, JobStatus newStatus) {
        User currentUser = currentUserService.getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID " + jobId + " not found"));

        authorizationService.requireRole(currentUser, job.getOrganization().getId(), OrganizationRole.RECRUITER);

        JobStatus currentStatus = job.getStatus();

        if (currentStatus == newStatus) {
            return JobResponse.fromEntity(job);
        }

        // Validate allowed transitions
        if (currentStatus == JobStatus.CLOSED && newStatus != JobStatus.CLOSED) {
            throw new ConflictException("Invalid job status transition from CLOSED to " + newStatus);
        }

        // Mandatory Publish Validation if moving DRAFT -> OPEN
        if (currentStatus == JobStatus.DRAFT && newStatus == JobStatus.OPEN) {
            validateJobForPublishing(job);
        }

        job.setStatus(newStatus);
        Job updatedJob = jobRepository.save(job);

        String auditAction = switch (newStatus) {
            case OPEN -> "JOB_PUBLISHED";
            case PAUSED -> "JOB_PAUSED";
            case CLOSED -> "JOB_CLOSED";
            default -> "JOB_STATUS_UPDATED";
        };

        auditEventService.logEvent(
                job.getOrganization(),
                currentUser,
                "JOB",
                job.getId(),
                auditAction,
                "{\"previousStatus\":\"" + currentStatus + "\",\"newStatus\":\"" + newStatus + "\"}"
        );

        return JobResponse.fromEntity(updatedJob);
    }

    @Transactional
    public void deleteJob(UUID jobId) {
        User currentUser = currentUserService.getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID " + jobId + " not found"));

        authorizationService.requireRole(currentUser, job.getOrganization().getId(), OrganizationRole.RECRUITER);

        // Soft archive Strategy
        job.setStatus(JobStatus.CLOSED);
        jobRepository.save(job);

        auditEventService.logEvent(
                job.getOrganization(),
                currentUser,
                "JOB",
                job.getId(),
                "JOB_ARCHIVED",
                "{\"action\":\"Soft archive job set status to CLOSED\"}"
        );
    }

    public void validateJobForPublishing(Job job) {
        if (job.getTitle() == null || job.getTitle().isBlank()) {
            throw new IllegalArgumentException("Job title is required before publishing");
        }
        if (job.getDescription() == null || job.getDescription().isBlank()) {
            throw new IllegalArgumentException("Job description is required before publishing");
        }
        if (job.getEmploymentType() == null) {
            throw new IllegalArgumentException("Employment type is required before publishing");
        }

        List<JobRequirement> requirements = requirementRepository.findByJobId(job.getId());
        if (requirements.isEmpty()) {
            throw new IllegalArgumentException("Job must have at least one requirement before publishing");
        }

        boolean hasRequiredType = requirements.stream()
                .anyMatch(r -> r.getRequirementType() == RequirementType.REQUIRED);
        if (!hasRequiredType) {
            throw new IllegalArgumentException("Job must have at least one REQUIRED requirement before publishing");
        }

        List<EvaluationCriterion> criteria = criterionRepository.findByJobId(job.getId());
        if (!criteria.isEmpty()) {
            BigDecimal totalWeight = criteria.stream()
                    .map(EvaluationCriterion::getWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalWeight.subtract(BigDecimal.ONE).abs().doubleValue() > 0.001) {
                throw new IllegalArgumentException("Total weight of evaluation criteria must equal 1.00 before publishing (current total: " + totalWeight + ")");
            }
        }
    }
}
