package com.talentgraph.ai.analysis;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO returned from AI analysis API endpoints.
 * Contains run metadata + all validated extracted proposals.
 */
@Getter
@Builder
public class AiAnalysisResponse {

    private UUID runId;
    private String provider;
    private String model;
    private String analysisType;
    private String status;
    private String promptVersion;

    private String summary;
    private List<SkillResult> skills;
    private List<ExperienceResult> experiences;
    private List<ProjectResult> projects;
    private List<EducationResult> education;
    private List<CertificationResult> certifications;

    private String errorCode;
    private String errorMessage;

    private String startedAt;
    private String completedAt;

    /** Whether AI analysis is enabled in server configuration. */
    private boolean analysisEnabled;

    // ---- Nested result types ----

    @Getter @Builder
    public static class SkillResult {
        private UUID id;
        private String skillName;
        private String evidenceQuote;
        private String sourceSection;
        private UUID catalogSkillId;
        private boolean validated;
    }

    @Getter @Builder
    public static class ExperienceResult {
        private UUID id;
        private String company;
        private String jobTitle;
        private String startDate;
        private String endDate;
        private String responsibilities;
        private String evidenceQuote;
    }

    @Getter @Builder
    public static class ProjectResult {
        private UUID id;
        private String projectName;
        private String description;
        private String technologies;
        private String evidenceQuote;
    }

    @Getter @Builder
    public static class EducationResult {
        private UUID id;
        private String institution;
        private String degree;
        private String fieldOfStudy;
        private String startDate;
        private String endDate;
        private String grade;
        private String evidenceQuote;
    }

    @Getter @Builder
    public static class CertificationResult {
        private UUID id;
        private String certificationName;
        private String issuingBody;
        private String issuedDate;
        private String evidenceQuote;
    }
}
