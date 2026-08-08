package com.talentgraph.evidence.dto;

import com.talentgraph.evidence.Skill;
import com.talentgraph.evidence.SkillCategory;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillResponse {

    private UUID id;
    private String name;
    private String normalizedName;
    private SkillCategory category;
    private Instant createdAt;

    public static SkillResponse fromEntity(Skill skill) {
        return SkillResponse.builder()
                .id(skill.getId())
                .name(skill.getName())
                .normalizedName(skill.getNormalizedName())
                .category(skill.getCategory())
                .createdAt(skill.getCreatedAt())
                .build();
    }
}
