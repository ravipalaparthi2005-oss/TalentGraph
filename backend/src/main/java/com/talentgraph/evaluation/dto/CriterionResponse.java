package com.talentgraph.evaluation.dto;

import com.talentgraph.evaluation.EvaluationCriterion;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriterionResponse {

    private UUID id;
    private UUID jobId;
    private UUID skillId;
    private String skillName;
    private String name;
    private String description;
    private BigDecimal weight;
    private Instant createdAt;

    public static CriterionResponse fromEntity(EvaluationCriterion criterion) {
        return CriterionResponse.builder()
                .id(criterion.getId())
                .jobId(criterion.getJob().getId())
                .skillId(criterion.getSkill() != null ? criterion.getSkill().getId() : null)
                .skillName(criterion.getSkill() != null ? criterion.getSkill().getName() : null)
                .name(criterion.getName())
                .description(criterion.getDescription())
                .weight(criterion.getWeight())
                .createdAt(criterion.getCreatedAt())
                .build();
    }
}
