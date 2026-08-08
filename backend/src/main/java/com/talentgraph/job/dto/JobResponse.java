package com.talentgraph.job.dto;

import com.talentgraph.job.EmploymentType;
import com.talentgraph.job.Job;
import com.talentgraph.job.JobStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobResponse {

    private UUID id;
    private UUID organizationId;
    private String title;
    private String department;
    private String location;
    private EmploymentType employmentType;
    private String description;
    private JobStatus status;
    private UUID createdById;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;

    public static JobResponse fromEntity(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .organizationId(job.getOrganization().getId())
                .title(job.getTitle())
                .department(job.getDepartment())
                .location(job.getLocation())
                .employmentType(job.getEmploymentType())
                .description(job.getDescription())
                .status(job.getStatus())
                .createdById(job.getCreatedBy() != null ? job.getCreatedBy().getId() : null)
                .createdByName(job.getCreatedBy() != null ? (job.getCreatedBy().getFirstName() + " " + job.getCreatedBy().getLastName()) : null)
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
