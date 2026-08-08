package com.talentgraph.job;

import com.talentgraph.audit.AuditEventService;
import com.talentgraph.audit.dto.AuditEventResponse;
import com.talentgraph.auth.User;
import com.talentgraph.auth.service.CurrentUserService;
import com.talentgraph.common.ApiResponse;
import com.talentgraph.common.exception.ResourceNotFoundException;
import com.talentgraph.organization.service.OrganizationAuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}/activity")
public class JobActivityController {

    private final JobRepository jobRepository;
    private final CurrentUserService currentUserService;
    private final OrganizationAuthorizationService authorizationService;
    private final AuditEventService auditEventService;

    public JobActivityController(
            JobRepository jobRepository,
            CurrentUserService currentUserService,
            OrganizationAuthorizationService authorizationService,
            AuditEventService auditEventService
    ) {
        this.jobRepository = jobRepository;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditEventResponse>>> getJobActivity(@PathVariable UUID jobId) {
        User currentUser = currentUserService.getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID " + jobId + " not found"));

        authorizationService.verifyResourceAccess(currentUser, job.getOrganization().getId());

        List<AuditEventResponse> activity = auditEventService.getEventsForEntity(job.getOrganization().getId(), jobId);
        return ResponseEntity.ok(ApiResponse.success(activity));
    }
}
