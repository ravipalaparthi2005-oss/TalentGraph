package com.talentgraph.assessment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AssessmentResponse {

    private UUID id;
    private UUID organizationId;
    private UUID jobId;
    private String jobTitle;
    private String name;
    private String description;
    private Integer durationMinutes;
    private String status;
    private int questionCount;
    private int totalPoints;
    private String createdByEmail;
    private String createdAt;
    private String updatedAt;

    private List<QuestionResponse> questions;
}
