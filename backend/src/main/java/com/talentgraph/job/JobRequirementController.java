package com.talentgraph.job;

import com.talentgraph.common.ApiResponse;
import com.talentgraph.job.dto.CreateRequirementRequest;
import com.talentgraph.job.dto.RequirementResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}/requirements")
public class JobRequirementController {

    private final JobRequirementService requirementService;

    public JobRequirementController(JobRequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RequirementResponse>> addRequirement(
            @PathVariable UUID jobId,
            @Valid @RequestBody CreateRequirementRequest request
    ) {
        RequirementResponse response = requirementService.addRequirement(jobId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Requirement added successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RequirementResponse>>> getRequirements(@PathVariable UUID jobId) {
        List<RequirementResponse> requirements = requirementService.getRequirements(jobId);
        return ResponseEntity.ok(ApiResponse.success(requirements));
    }

    @PutMapping("/{requirementId}")
    public ResponseEntity<ApiResponse<RequirementResponse>> updateRequirement(
            @PathVariable UUID jobId,
            @PathVariable UUID requirementId,
            @Valid @RequestBody CreateRequirementRequest request
    ) {
        RequirementResponse response = requirementService.updateRequirement(jobId, requirementId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Requirement updated successfully"));
    }

    @DeleteMapping("/{requirementId}")
    public ResponseEntity<ApiResponse<Void>> deleteRequirement(
            @PathVariable UUID jobId,
            @PathVariable UUID requirementId
    ) {
        requirementService.deleteRequirement(jobId, requirementId);
        return ResponseEntity.ok(ApiResponse.success(null, "Requirement deleted successfully"));
    }
}
