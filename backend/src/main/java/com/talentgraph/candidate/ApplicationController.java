package com.talentgraph.candidate;

import com.talentgraph.candidate.dto.ApplicationResponse;
import com.talentgraph.candidate.dto.CreateApplicationRequest;
import com.talentgraph.candidate.dto.UpdateApplicationStatusRequest;
import com.talentgraph.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/api/v1/jobs/{jobId}/applications")
    public ResponseEntity<ApiResponse<ApplicationResponse>> createApplication(
            @PathVariable("jobId") UUID jobId,
            @Valid @RequestBody CreateApplicationRequest request
    ) {
        ApplicationResponse response = applicationService.createApplication(jobId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Application created successfully"));
    }

    @GetMapping("/api/v1/jobs/{jobId}/applications")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getJobApplications(
            @PathVariable("jobId") UUID jobId
    ) {
        List<ApplicationResponse> response = applicationService.getJobApplications(jobId);
        return ResponseEntity.ok(ApiResponse.success(response, "Job applications retrieved successfully"));
    }

    @GetMapping("/api/v1/candidates/{candidateId}/applications")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getCandidateApplications(
            @PathVariable("candidateId") UUID candidateId
    ) {
        List<ApplicationResponse> response = applicationService.getCandidateApplications(candidateId);
        return ResponseEntity.ok(ApiResponse.success(response, "Candidate applications retrieved successfully"));
    }

    @GetMapping("/api/v1/applications/{applicationId}")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getApplicationById(
            @PathVariable("applicationId") UUID applicationId
    ) {
        ApplicationResponse response = applicationService.getApplicationById(applicationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Application retrieved successfully"));
    }

    @PatchMapping("/api/v1/applications/{applicationId}/status")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplicationStatus(
            @PathVariable("applicationId") UUID applicationId,
            @Valid @RequestBody UpdateApplicationStatusRequest request
    ) {
        ApplicationResponse response = applicationService.updateApplicationStatus(applicationId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(response, "Application status updated successfully"));
    }
}
