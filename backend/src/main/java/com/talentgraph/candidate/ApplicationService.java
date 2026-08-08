package com.talentgraph.candidate;

import com.talentgraph.audit.AuditEventService;
import com.talentgraph.candidate.dto.ApplicationResponse;
import com.talentgraph.candidate.dto.CreateApplicationRequest;
import com.talentgraph.common.exception.ConflictException;
import com.talentgraph.common.exception.DuplicateResourceException;
import com.talentgraph.common.exception.ResourceNotFoundException;
import com.talentgraph.job.Job;
import com.talentgraph.job.JobRepository;
import com.talentgraph.organization.OrganizationRole;
import com.talentgraph.organization.service.OrganizationAuthorizationService;
import com.talentgraph.auth.User;
import com.talentgraph.auth.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CandidateRepository candidateRepository;
    private final JobRepository jobRepository;
    private final OrganizationAuthorizationService authorizationService;
    private final CurrentUserService currentUserService;
    private final AuditEventService auditEventService;

    @Transactional
    public ApplicationResponse createApplication(UUID jobId, CreateApplicationRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        UUID orgId = job.getOrganization().getId();
        authorizationService.requireRole(currentUser, orgId, OrganizationRole.RECRUITER);

        Candidate candidate = candidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + request.getCandidateId()));

        // Cross-organization application validation check
        if (!candidate.getOrganization().getId().equals(orgId)) {
            throw new ConflictException(String.format(
                    "Cross-organization application rejected: Candidate organization (%s) does not match Job organization (%s).",
                    candidate.getOrganization().getId(), orgId
            ));
        }

        if (applicationRepository.existsByCandidateIdAndJobId(candidate.getId(), job.getId())) {
            throw new DuplicateResourceException("Candidate already has an active application for this job.");
        }

        Application application = Application.builder()
                .candidate(candidate)
                .job(job)
                .status(ApplicationStatus.NEW)
                .source(request.getSource() != null ? request.getSource() : ApplicationSource.RECRUITER)
                .appliedAt(Instant.now())
                .build();

        application = applicationRepository.save(application);

        auditEventService.logEvent(
                job.getOrganization(),
                currentUser,
                "Application",
                application.getId(),
                "APPLICATION_CREATED",
                String.format("{\"candidateId\":\"%s\",\"jobId\":\"%s\"}", candidate.getId(), job.getId())
        );

        return mapToResponse(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getJobApplications(UUID jobId) {
        User currentUser = currentUserService.getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        authorizationService.requireRole(currentUser, job.getOrganization().getId(), OrganizationRole.INTERVIEWER);

        return applicationRepository.findByJobIdOrderByCreatedAtDesc(jobId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getCandidateApplications(UUID candidateId) {
        User currentUser = currentUserService.getCurrentUser();
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        authorizationService.requireRole(currentUser, candidate.getOrganization().getId(), OrganizationRole.INTERVIEWER);

        return applicationRepository.findByCandidateIdOrderByCreatedAtDesc(candidateId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(UUID applicationId) {
        User currentUser = currentUserService.getCurrentUser();
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        authorizationService.requireRole(currentUser, application.getJob().getOrganization().getId(), OrganizationRole.INTERVIEWER);

        return mapToResponse(application);
    }

    @Transactional
    public ApplicationResponse updateApplicationStatus(UUID applicationId, ApplicationStatus newStatus) {
        User currentUser = currentUserService.getCurrentUser();
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        UUID orgId = application.getJob().getOrganization().getId();
        authorizationService.requireRole(currentUser, orgId, OrganizationRole.RECRUITER);

        ApplicationStatus previousStatus = application.getStatus();
        if (previousStatus == newStatus) {
            return mapToResponse(application);
        }

        application.setStatus(newStatus);
        application = applicationRepository.save(application);

        auditEventService.logEvent(
                application.getJob().getOrganization(),
                currentUser,
                "Application",
                application.getId(),
                "APPLICATION_STATUS_CHANGED",
                String.format("{\"previousStatus\":\"%s\",\"newStatus\":\"%s\"}", previousStatus.name(), newStatus.name())
        );

        return mapToResponse(application);
    }

    public ApplicationResponse mapToResponse(Application application) {
        return ApplicationResponse.builder()
                .id(application.getId())
                .candidateId(application.getCandidate().getId())
                .candidateName(application.getCandidate().getFirstName() + " " + application.getCandidate().getLastName())
                .candidateEmail(application.getCandidate().getEmail())
                .jobId(application.getJob().getId())
                .jobTitle(application.getJob().getTitle())
                .status(application.getStatus())
                .source(application.getSource())
                .appliedAt(application.getAppliedAt())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }
}
