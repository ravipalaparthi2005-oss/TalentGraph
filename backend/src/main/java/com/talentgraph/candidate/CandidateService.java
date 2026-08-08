package com.talentgraph.candidate;

import com.talentgraph.audit.AuditEventService;
import com.talentgraph.candidate.dto.CandidateResponse;
import com.talentgraph.candidate.dto.CreateCandidateRequest;
import com.talentgraph.common.exception.DuplicateResourceException;
import com.talentgraph.common.exception.ResourceNotFoundException;
import com.talentgraph.organization.Organization;
import com.talentgraph.organization.OrganizationMemberRepository;
import com.talentgraph.organization.OrganizationRepository;
import com.talentgraph.organization.OrganizationRole;
import com.talentgraph.organization.service.OrganizationAuthorizationService;
import com.talentgraph.auth.User;
import com.talentgraph.auth.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationAuthorizationService authorizationService;
    private final CurrentUserService currentUserService;
    private final AuditEventService auditEventService;

    @Transactional
    public CandidateResponse createCandidate(CreateCandidateRequest request, UUID explicitOrgId) {
        User currentUser = currentUserService.getCurrentUser();
        UUID targetOrgId = resolveOrgId(request.getOrganizationId(), explicitOrgId, currentUser.getId());

        authorizationService.requireRole(currentUser, targetOrgId, OrganizationRole.RECRUITER);

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (candidateRepository.findByOrganizationIdAndEmailAndIsActiveTrue(targetOrgId, normalizedEmail).isPresent()) {
            throw new DuplicateResourceException("Candidate with email " + normalizedEmail + " already exists in this organization.");
        }

        Organization organization = organizationRepository.findById(targetOrgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + targetOrgId));

        Candidate candidate = Candidate.builder()
                .organization(organization)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(normalizedEmail)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .location(request.getLocation() != null ? request.getLocation().trim() : null)
                .linkedinUrl(request.getLinkedinUrl() != null ? request.getLinkedinUrl().trim() : null)
                .githubUsername(request.getGithubUsername() != null ? request.getGithubUsername().trim() : null)
                .portfolioUrl(request.getPortfolioUrl() != null ? request.getPortfolioUrl().trim() : null)
                .isActive(true)
                .build();

        candidate = candidateRepository.save(candidate);

        auditEventService.logEvent(
                organization,
                currentUser,
                "Candidate",
                candidate.getId(),
                "CANDIDATE_CREATED",
                String.format("{\"email\":\"%s\",\"name\":\"%s %s\"}", normalizedEmail, candidate.getFirstName(), candidate.getLastName())
        );

        return mapToResponse(candidate);
    }

    @Transactional(readOnly = true)
    public Page<CandidateResponse> getCandidates(UUID explicitOrgId, String search, int page, int size, String sortBy, String sortDirection) {
        User currentUser = currentUserService.getCurrentUser();
        UUID targetOrgId = resolveOrgId(null, explicitOrgId, currentUser.getId());

        authorizationService.requireRole(currentUser, targetOrgId, OrganizationRole.INTERVIEWER);

        int boundedSize = Math.min(Math.max(size, 1), 100);
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = "createdAt".equals(sortBy) || "firstName".equals(sortBy) || "lastName".equals(sortBy) ? sortBy : "createdAt";
        Pageable pageable = PageRequest.of(Math.max(0, page), boundedSize, Sort.by(direction, sortProperty));

        return candidateRepository.searchCandidates(targetOrgId, search, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public CandidateResponse getCandidateById(UUID candidateId, UUID explicitOrgId) {
        User currentUser = currentUserService.getCurrentUser();
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        authorizationService.requireRole(currentUser, candidate.getOrganization().getId(), OrganizationRole.INTERVIEWER);

        return mapToResponse(candidate);
    }

    @Transactional
    public CandidateResponse updateCandidate(UUID candidateId, CreateCandidateRequest request, UUID explicitOrgId) {
        User currentUser = currentUserService.getCurrentUser();
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        authorizationService.requireRole(currentUser, candidate.getOrganization().getId(), OrganizationRole.RECRUITER);

        candidate.setFirstName(request.getFirstName().trim());
        candidate.setLastName(request.getLastName().trim());
        if (request.getPhone() != null) candidate.setPhone(request.getPhone().trim());
        if (request.getLocation() != null) candidate.setLocation(request.getLocation().trim());
        if (request.getLinkedinUrl() != null) candidate.setLinkedinUrl(request.getLinkedinUrl().trim());
        if (request.getGithubUsername() != null) candidate.setGithubUsername(request.getGithubUsername().trim());
        if (request.getPortfolioUrl() != null) candidate.setPortfolioUrl(request.getPortfolioUrl().trim());

        candidate = candidateRepository.save(candidate);

        auditEventService.logEvent(
                candidate.getOrganization(),
                currentUser,
                "Candidate",
                candidate.getId(),
                "CANDIDATE_UPDATED",
                String.format("{\"name\":\"%s %s\"}", candidate.getFirstName(), candidate.getLastName())
        );

        return mapToResponse(candidate);
    }

    @Transactional
    public void deleteCandidate(UUID candidateId, UUID explicitOrgId) {
        User currentUser = currentUserService.getCurrentUser();
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        authorizationService.requireRole(currentUser, candidate.getOrganization().getId(), OrganizationRole.RECRUITER);

        candidate.setActive(false);
        candidateRepository.save(candidate);

        auditEventService.logEvent(
                candidate.getOrganization(),
                currentUser,
                "Candidate",
                candidate.getId(),
                "CANDIDATE_DEACTIVATED",
                String.format("{\"email\":\"%s\"}", candidate.getEmail())
        );
    }

    public CandidateResponse mapToResponse(Candidate candidate) {
        return CandidateResponse.builder()
                .id(candidate.getId())
                .organizationId(candidate.getOrganization().getId())
                .firstName(candidate.getFirstName())
                .lastName(candidate.getLastName())
                .email(candidate.getEmail())
                .phone(candidate.getPhone())
                .location(candidate.getLocation())
                .linkedinUrl(candidate.getLinkedinUrl())
                .githubUsername(candidate.getGithubUsername())
                .portfolioUrl(candidate.getPortfolioUrl())
                .active(candidate.isActive())
                .createdAt(candidate.getCreatedAt())
                .updatedAt(candidate.getUpdatedAt())
                .build();
    }

    private UUID resolveOrgId(UUID requestOrgId, UUID explicitOrgId, UUID userId) {
        if (requestOrgId != null) return requestOrgId;
        if (explicitOrgId != null) return explicitOrgId;
        return organizationMemberRepository.findByUserId(userId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User does not belong to any organization"))
                .getOrganization().getId();
    }
}
