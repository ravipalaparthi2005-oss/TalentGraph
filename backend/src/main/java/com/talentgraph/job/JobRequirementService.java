package com.talentgraph.job;

import com.talentgraph.auth.User;
import com.talentgraph.auth.service.CurrentUserService;
import com.talentgraph.common.exception.ResourceNotFoundException;
import com.talentgraph.evidence.Skill;
import com.talentgraph.evidence.SkillRepository;
import com.talentgraph.job.dto.CreateRequirementRequest;
import com.talentgraph.job.dto.RequirementResponse;
import com.talentgraph.organization.OrganizationRole;
import com.talentgraph.organization.service.OrganizationAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class JobRequirementService {

    private final JobRepository jobRepository;
    private final JobRequirementRepository requirementRepository;
    private final SkillRepository skillRepository;
    private final CurrentUserService currentUserService;
    private final OrganizationAuthorizationService authorizationService;

    public JobRequirementService(
            JobRepository jobRepository,
            JobRequirementRepository requirementRepository,
            SkillRepository skillRepository,
            CurrentUserService currentUserService,
            OrganizationAuthorizationService authorizationService
    ) {
        this.jobRepository = jobRepository;
        this.requirementRepository = requirementRepository;
        this.skillRepository = skillRepository;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public RequirementResponse addRequirement(UUID jobId, CreateRequirementRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID " + jobId + " not found"));

        authorizationService.requireRole(currentUser, job.getOrganization().getId(), OrganizationRole.RECRUITER);

        Skill skill = null;
        if (request.getSkillId() != null) {
            skill = skillRepository.findById(request.getSkillId())
                    .orElseThrow(() -> new ResourceNotFoundException("Skill with ID " + request.getSkillId() + " not found"));
        }

        JobRequirement requirement = JobRequirement.builder()
                .job(job)
                .skill(skill)
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .requirementType(request.getRequirementType())
                .importance(request.getImportance())
                .minimumLevel(request.getMinimumLevel() != null ? request.getMinimumLevel().trim() : null)
                .build();

        JobRequirement saved = requirementRepository.save(requirement);
        return RequirementResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<RequirementResponse> getRequirements(UUID jobId) {
        User currentUser = currentUserService.getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID " + jobId + " not found"));

        authorizationService.verifyResourceAccess(currentUser, job.getOrganization().getId());

        List<JobRequirement> requirements = requirementRepository.findByJobId(jobId);
        return requirements.stream()
                .map(RequirementResponse::fromEntity)
                .toList();
    }

    @Transactional
    public RequirementResponse updateRequirement(UUID jobId, UUID requirementId, CreateRequirementRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID " + jobId + " not found"));

        authorizationService.requireRole(currentUser, job.getOrganization().getId(), OrganizationRole.RECRUITER);

        JobRequirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement with ID " + requirementId + " not found"));

        if (!requirement.getJob().getId().equals(job.getId())) {
            throw new IllegalArgumentException("Requirement does not belong to specified job");
        }

        Skill skill = null;
        if (request.getSkillId() != null) {
            skill = skillRepository.findById(request.getSkillId())
                    .orElseThrow(() -> new ResourceNotFoundException("Skill with ID " + request.getSkillId() + " not found"));
        }

        requirement.setName(request.getName().trim());
        requirement.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        requirement.setRequirementType(request.getRequirementType());
        requirement.setImportance(request.getImportance());
        requirement.setMinimumLevel(request.getMinimumLevel() != null ? request.getMinimumLevel().trim() : null);
        requirement.setSkill(skill);

        JobRequirement updated = requirementRepository.save(requirement);
        return RequirementResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteRequirement(UUID jobId, UUID requirementId) {
        User currentUser = currentUserService.getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID " + jobId + " not found"));

        authorizationService.requireRole(currentUser, job.getOrganization().getId(), OrganizationRole.RECRUITER);

        JobRequirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement with ID " + requirementId + " not found"));

        if (!requirement.getJob().getId().equals(job.getId())) {
            throw new IllegalArgumentException("Requirement does not belong to specified job");
        }

        requirementRepository.delete(requirement);
    }
}
