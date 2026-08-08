package com.talentgraph.job.dto;

import com.talentgraph.job.Importance;
import com.talentgraph.job.RequirementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRequirementRequest {

    @NotBlank(message = "Requirement name is required")
    private String name;

    private String description;

    @NotNull(message = "Requirement type is required")
    private RequirementType requirementType;

    @NotNull(message = "Importance level is required")
    private Importance importance;

    private String minimumLevel;

    private UUID skillId;
}
