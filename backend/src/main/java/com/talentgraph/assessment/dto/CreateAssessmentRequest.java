package com.talentgraph.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAssessmentRequest {

    @NotBlank(message = "Assessment name is required")
    private String name;

    private String description;

    private Integer durationMinutes;

    private UUID jobId;
}
