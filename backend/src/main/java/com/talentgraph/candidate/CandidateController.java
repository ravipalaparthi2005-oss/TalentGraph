package com.talentgraph.candidate;

import com.talentgraph.candidate.dto.CandidateResponse;
import com.talentgraph.candidate.dto.CreateCandidateRequest;
import com.talentgraph.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;

    @PostMapping
    public ResponseEntity<ApiResponse<CandidateResponse>> createCandidate(
            @Valid @RequestBody CreateCandidateRequest request,
            @RequestParam(name = "organizationId", required = false) UUID organizationId
    ) {
        CandidateResponse response = candidateService.createCandidate(request, organizationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Candidate created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CandidateResponse>>> getCandidates(
            @RequestParam(name = "organizationId", required = false) UUID organizationId,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDirection", defaultValue = "DESC") String sortDirection
    ) {
        Page<CandidateResponse> response = candidateService.getCandidates(organizationId, search, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success(response, "Candidates retrieved successfully"));
    }

    @GetMapping("/{candidateId}")
    public ResponseEntity<ApiResponse<CandidateResponse>> getCandidateById(
            @PathVariable("candidateId") UUID candidateId,
            @RequestParam(name = "organizationId", required = false) UUID organizationId
    ) {
        CandidateResponse response = candidateService.getCandidateById(candidateId, organizationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Candidate retrieved successfully"));
    }

    @PutMapping("/{candidateId}")
    public ResponseEntity<ApiResponse<CandidateResponse>> updateCandidate(
            @PathVariable("candidateId") UUID candidateId,
            @Valid @RequestBody CreateCandidateRequest request,
            @RequestParam(name = "organizationId", required = false) UUID organizationId
    ) {
        CandidateResponse response = candidateService.updateCandidate(candidateId, request, organizationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Candidate updated successfully"));
    }

    @DeleteMapping("/{candidateId}")
    public ResponseEntity<ApiResponse<Void>> deleteCandidate(
            @PathVariable("candidateId") UUID candidateId,
            @RequestParam(name = "organizationId", required = false) UUID organizationId
    ) {
        candidateService.deleteCandidate(candidateId, organizationId);
        return ResponseEntity.ok(ApiResponse.success(null, "Candidate deactivated successfully"));
    }
}
