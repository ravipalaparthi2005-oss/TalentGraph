package com.talentgraph.evaluation;

import com.talentgraph.auth.User;
import com.talentgraph.auth.service.CurrentUserService;
import com.talentgraph.common.exception.ResourceNotFoundException;
import com.talentgraph.evaluation.dto.CreateCriterionRequest;
import com.talentgraph.evaluation.dto.CriterionResponse;
import com.talentgraph.evidence.Skill;
import com.talentgraph.evidence.SkillRepository;
import com.talentgraph.job.Job;
import com.talentgraph.job.JobRepository;
import com.talentgraph.organization.OrganizationRole;
import com.talentgraph.organization.service.OrganizationAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EvaluationCriterionService {

    private final JobRepository jobRepository;
    private final EvaluationCriterionRepository criterionRepository;
    private final SkillRepository skillRepository;
    private final CurrentUserService currentUserService;
    private final OrganizationAuthorizationService authorizationService;

    public EvaluationCriterionService(
            JobRepository jobRepository,
            EvaluationCriterionRepository criterionRepository,
            SkillRepository skillRepository,
            CurrentUserService currentUserService,
            OrganizationAuthorizationService authorizationService
    ) {
        this.jobRepository = jobRepository;
        this.criterionRepository = criterionRepository;
        this.skillRepository = skillRepository;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public CriterionResponse addCriterion(UUID jobId, CreateCriterionRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID " + jobId + " not found"));

        authorizationService.requireRole(currentUser, job.getOrganization().getId(), OrganizationRole.RECRUITER);

        Skill skill = null;
        if (request.getSkillId() != null) {
            skill = skillRepository.findById(request.getSkillId())
                    .orElseThrow(() -> new ResourceNotFoundException("Skill with ID " + request.getSkillId() + " not found"));
        }

        EvaluationCriterion criterion = EvaluationCriterion.builder()
                .job(job)
                .skill(skill)
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .weight(request.getWeight())
                .build();

        EvaluationCriterion saved = criterionRepository.save(criterion);
        return CriterionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<CriterionResponse> getCriteria(UUID jobId) {
        User currentUser = currentUserService.getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID " + jobId + " not found"));

        authorizationService.verifyResourceAccess(currentUser, job.getOrganization().getId());

        List<EvaluationCriterion> criteria = criterionRepository.findByJobId(jobId);
        return criteria.stream()
                .map(CriterionResponse::fromEntity)
                .toList();
    }

    @Transactional
    public CriterionResponse updateCriterion(UUID jobId, UUID criterionId, CreateCriterionRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID " + jobId + " not found"));

        authorizationService.requireRole(currentUser, job.getOrganization().getId(), OrganizationRole.RECRUITER);

        EvaluationCriterion criterion = criterionRepository.findById(criterionId)
                .orElseThrow(() -> new ResourceNotFoundException("Criterion with ID " + criterionId + " not found"));

        if (!criterion.getJob().getId().equals(job.getId())) {
            throw new IllegalArgumentException("Criterion does not belong to specified job");
        }

        Skill skill = null;
        if (request.getSkillId() != null) {
            skill = skillRepository.findById(request.getSkillId())
                    .orElseThrow(() -> new ResourceNotFoundException("Skill with ID " + request.getSkillId() + " not found"));
        }

        criterion.setName(request.getName().trim());
        criterion.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        criterion.setWeight(request.getWeight());
        criterion.setSkill(skill);

        EvaluationCriterion updated = criterionRepository.save(criterion);
        return CriterionResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteCriterion(UUID jobId, UUID criterionId) {
        User currentUser = currentUserService.getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID " + jobId + " not found"));

        authorizationService.requireRole(currentUser, job.getOrganization().getId(), OrganizationRole.RECRUITER);

        EvaluationCriterion criterion = criterionRepository.findById(criterionId)
                .orElseThrow(() -> new ResourceNotFoundException("Criterion with ID " + criterionId + " not found"));

        if (!criterion.getJob().getId().equals(job.getId())) {
            throw new IllegalArgumentException("Criterion does not belong to specified job");
        }

        criterionRepository.delete(criterion);
    }
}
