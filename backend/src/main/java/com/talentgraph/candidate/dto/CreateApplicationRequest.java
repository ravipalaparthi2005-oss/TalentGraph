package com.talentgraph.candidate.dto;

import com.talentgraph.candidate.ApplicationSource;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApplicationRequest {

    @NotNull(message = "Candidate ID is required")
    private UUID candidateId;

    private ApplicationSource source;
}
