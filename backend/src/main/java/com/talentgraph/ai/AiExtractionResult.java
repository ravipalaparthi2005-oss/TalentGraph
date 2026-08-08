package com.talentgraph.ai;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Structured result returned by an AI provider after resume extraction.
 *
 * <p>Every field in this object is a <em>proposal</em> derived from AI output.
 * Nothing here is authoritative until the backend has validated evidence quotes
 * against the actual source resume text and mapped skills to the catalog.
 */
@Getter
@Builder
public class AiExtractionResult {

    private final String summary;
    private final List<ExtractedSkill> skills;
    private final List<ExtractedExperience> experiences;
    private final List<ExtractedProject> projects;
    private final List<ExtractedEducation> education;
    private final List<ExtractedCertification> certifications;

    // ---- Nested proposal types ----

    @Getter
    @Builder
    public static class ExtractedSkill {
        private final String name;
        /** Short exact quote from resume text supporting this skill claim. */
        private final String evidenceQuote;
        private final String sourceSection;
    }

    @Getter
    @Builder
    public static class ExtractedExperience {
        private final String company;
        private final String jobTitle;
        /** Raw string from resume; may be "2021" or "Jan 2021" — never inferred. */
        private final String startDate;
        /** Null when not present in resume text. */
        private final String endDate;
        private final String responsibilities;
        private final String evidenceQuote;
    }

    @Getter
    @Builder
    public static class ExtractedProject {
        private final String projectName;
        private final String description;
        private final String technologies;
        private final String evidenceQuote;
    }

    @Getter
    @Builder
    public static class ExtractedEducation {
        private final String institution;
        private final String degree;
        private final String fieldOfStudy;
        private final String startDate;
        private final String endDate;
        private final String grade;
        private final String evidenceQuote;
    }

    @Getter
    @Builder
    public static class ExtractedCertification {
        private final String certificationName;
        private final String issuingBody;
        private final String issuedDate;
        private final String evidenceQuote;
    }
}
