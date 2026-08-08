package com.talentgraph.ai.analysis;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for AI resume intelligence endpoints.
 *
 * <p>Endpoints:
 * <pre>
 *   POST /api/v1/candidates/{candidateId}/documents/{documentId}/ai-analysis
 *   GET  /api/v1/candidates/{candidateId}/documents/{documentId}/ai-analysis
 * </pre>
 *
 * <p>Security: Requires authenticated RECRUITER role (enforced in the service layer).
 * Cross-organization access returns 403 Forbidden.
 * The OpenRouter API key is NEVER included in any response.
 */
@RestController
@RequestMapping("/api/v1/candidates/{candidateId}/documents/{documentId}/ai-analysis")
@RequiredArgsConstructor
public class AiAnalysisController {

    private final ResumeAiAnalysisService analysisService;

    /**
     * Trigger AI analysis for a candidate document.
     *
     * @param candidateId  owning candidate UUID
     * @param documentId   document UUID to analyze
     * @param reanalyze    when true, creates a new analysis run even if a cached result exists
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiAnalysisResponse> triggerAnalysis(
            @PathVariable UUID candidateId,
            @PathVariable UUID documentId,
            @RequestParam(defaultValue = "false") boolean reanalyze
    ) {
        AiAnalysisResponse response = analysisService.analyze(candidateId, documentId, reanalyze);
        return ResponseEntity.ok(response);
    }

    /**
     * Get the latest AI analysis status and results for a document.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiAnalysisResponse> getAnalysis(
            @PathVariable UUID candidateId,
            @PathVariable UUID documentId
    ) {
        AiAnalysisResponse response = analysisService.getLatestAnalysis(candidateId, documentId);
        return ResponseEntity.ok(response);
    }
}
