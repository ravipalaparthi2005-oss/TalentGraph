package com.talentgraph.assessment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuestionRequest {

    @NotBlank(message = "Question title is required")
    private String title;

    @NotBlank(message = "Question description is required")
    private String description;

    private String constraints;

    private String inputFormat;

    private String outputFormat;

    private String examplesJson;

    private String difficulty; // EASY, MEDIUM, HARD

    @Min(value = 1, message = "Points must be at least 1")
    private Integer points;

    private Integer displayOrder;

    private Double timeLimitSeconds;

    private Integer memoryLimitMb;

    private List<String> allowedLanguages; // e.g. ["java", "python", "cpp"]

    private List<UUID> skillIds;
}
