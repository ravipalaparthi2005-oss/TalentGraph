package com.talentgraph.job.dto;

import com.talentgraph.job.JobStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateJobStatusRequest {

    @NotNull(message = "Job status is required")
    private JobStatus status;
}
