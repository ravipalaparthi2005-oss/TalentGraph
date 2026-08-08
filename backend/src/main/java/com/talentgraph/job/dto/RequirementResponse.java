package com.talentgraph.job.dto;

import com.talentgraph.job.Importance;
import com.talentgraph.job.JobRequirement;
import com.talentgraph.job.RequirementType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequirementResponse {

    private UUID id;
    private UUID jobId;
    private UUID skillId;
    private String skillName;
    private String name;
    private String description;
    private RequirementType requirementType;
    private Importance importance;
    private String minimumLevel;
    private Instant createdAt;

    public static RequirementResponse fromEntity(JobRequirement requirement) {
        return RequirementResponse.builder()
                .id(requirement.getId())
                .jobId(requirement.getJob().getId())
                .skillId(requirement.getSkill() != null ? requirement.getSkill().getId() : null)
                .skillName(requirement.getSkill() != null ? requirement.getSkill().getName() : null)
                .name(requirement.getName())
                .description(requirement.getDescription())
                .requirementType(requirement.getRequirementType())
                .importance(requirement.getImportance())
                .minimumLevel(requirement.getMinimumLevel())
                .createdAt(requirement.getCreatedAt())
                .build();
    }
}
