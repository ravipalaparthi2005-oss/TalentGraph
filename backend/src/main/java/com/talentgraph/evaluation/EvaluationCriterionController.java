package com.talentgraph.evaluation;

import com.talentgraph.common.ApiResponse;
import com.talentgraph.evaluation.dto.CreateCriterionRequest;
import com.talentgraph.evaluation.dto.CriterionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}/criteria")
public class EvaluationCriterionController {

    private final EvaluationCriterionService criterionService;

    public EvaluationCriterionController(EvaluationCriterionService criterionService) {
        this.criterionService = criterionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CriterionResponse>> addCriterion(
            @PathVariable UUID jobId,
            @Valid @RequestBody CreateCriterionRequest request
    ) {
        CriterionResponse response = criterionService.addCriterion(jobId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Evaluation criterion added successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CriterionResponse>>> getCriteria(@PathVariable UUID jobId) {
        List<CriterionResponse> criteria = criterionService.getCriteria(jobId);
        return ResponseEntity.ok(ApiResponse.success(criteria));
    }

    @PutMapping("/{criterionId}")
    public ResponseEntity<ApiResponse<CriterionResponse>> updateCriterion(
            @PathVariable UUID jobId,
            @PathVariable UUID criterionId,
            @Valid @RequestBody CreateCriterionRequest request
    ) {
        CriterionResponse response = criterionService.updateCriterion(jobId, criterionId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Evaluation criterion updated successfully"));
    }

    @DeleteMapping("/{criterionId}")
    public ResponseEntity<ApiResponse<Void>> deleteCriterion(
            @PathVariable UUID jobId,
            @PathVariable UUID criterionId
    ) {
        criterionService.deleteCriterion(jobId, criterionId);
        return ResponseEntity.ok(ApiResponse.success(null, "Evaluation criterion deleted successfully"));
    }
}
