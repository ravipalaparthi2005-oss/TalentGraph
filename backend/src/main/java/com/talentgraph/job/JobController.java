package com.talentgraph.job;

import com.talentgraph.common.ApiResponse;
import com.talentgraph.job.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JobResponse>> createJob(
            @Valid @RequestBody CreateJobRequest request,
            @RequestParam(required = false) UUID organizationId
    ) {
        JobResponse response = jobService.createJob(request, organizationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Job created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<JobResponse>>> getJobs(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Page<JobResponse> jobs = jobService.getJobs(organizationId, status, employmentType, search, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<JobResponse>> getJobById(@PathVariable UUID jobId) {
        JobResponse response = jobService.getJobById(jobId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{jobId}")
    public ResponseEntity<ApiResponse<JobResponse>> updateJob(
            @PathVariable UUID jobId,
            @Valid @RequestBody UpdateJobRequest request
    ) {
        JobResponse response = jobService.updateJob(jobId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Job updated successfully"));
    }

    @PatchMapping("/{jobId}/status")
    public ResponseEntity<ApiResponse<JobResponse>> updateJobStatus(
            @PathVariable UUID jobId,
            @Valid @RequestBody UpdateJobStatusRequest request
    ) {
        JobResponse response = jobService.updateJobStatus(jobId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(response, "Job status updated successfully"));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable UUID jobId) {
        jobService.deleteJob(jobId);
        return ResponseEntity.ok(ApiResponse.success(null, "Job deleted/archived successfully"));
    }
}
