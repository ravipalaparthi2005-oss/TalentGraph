package com.talentgraph.assessment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class QuestionResponse {

    private UUID id;
    private UUID assessmentId;
    private String title;
    private String description;
    private String constraints;
    private String inputFormat;
    private String outputFormat;
    private String examplesJson;
    private String difficulty;
    private int points;
    private int displayOrder;
    private double timeLimitSeconds;
    private int memoryLimitMb;
    private List<String> allowedLanguages;
    private List<SkillDto> skills;
    private List<TestCaseResponse> testCases;

    @Getter
    @Builder
    public static class SkillDto {
        private UUID id;
        private String name;
        private String normalizedName;
        private String category;
    }
}
