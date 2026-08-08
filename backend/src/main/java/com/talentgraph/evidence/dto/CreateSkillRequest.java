package com.talentgraph.evidence.dto;

import com.talentgraph.evidence.SkillCategory;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSkillRequest {

    @NotBlank(message = "Skill name is required")
    private String name;

    private SkillCategory category;
}
