package com.talentgraph.evaluation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCriterionRequest {

    @NotBlank(message = "Criterion name is required")
    private String name;

    private String description;

    private UUID skillId;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.01", message = "Weight must be greater than 0.0")
    @DecimalMax(value = "1.00", message = "Weight cannot exceed 1.0")
    private BigDecimal weight;
}
