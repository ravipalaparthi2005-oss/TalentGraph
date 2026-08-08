package com.talentgraph.candidate;

import com.talentgraph.auth.User;
import com.talentgraph.auth.service.CurrentUserService;
import com.talentgraph.common.ApiResponse;
import com.talentgraph.common.exception.ResourceNotFoundException;
import com.talentgraph.organization.service.OrganizationAuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidates")
public class TestCandidateAccessController {

    private final CandidateRepository candidateRepository;
    private final CurrentUserService currentUserService;
    private final OrganizationAuthorizationService authorizationService;

    public TestCandidateAccessController(
            CandidateRepository candidateRepository,
            CurrentUserService currentUserService,
            OrganizationAuthorizationService authorizationService
    ) {
        this.candidateRepository = candidateRepository;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/{candidateId}")
    public ResponseEntity<ApiResponse<Candidate>> getCandidateById(@PathVariable UUID candidateId) {
        User currentUser = currentUserService.getCurrentUser();
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate with ID " + candidateId + " not found"));

        // MANDATORY IDOR GUARD: Verify that candidate's organization matches current user's organization membership
        authorizationService.verifyResourceAccess(currentUser, candidate.getOrganization().getId());

        return ResponseEntity.ok(ApiResponse.success(candidate));
    }
}
