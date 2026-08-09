package com.talentgraph.ai.openrouter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgraph.ai.AiExtractionResult;
import com.talentgraph.ai.AiProvider;
import com.talentgraph.ai.AiProviderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenRouter implementation of {@link AiProvider}.
 *
 * <p>Security constraints enforced here:
 * <ul>
 *   <li>API key is read from configuration — never logged, never serialized.</li>
 *   <li>Resume text is sent to OpenRouter over HTTPS only.</li>
 *   <li>Raw OpenRouter error bodies are NOT forwarded to callers.</li>
 *   <li>Connection + read timeouts prevent hanging threads.</li>
 *   <li>Retries only on 429/5xx — not on auth or config errors.</li>
 * </ul>
 *
 * <p>This class is active only when {@code openrouter.analysis-enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "openrouter.analysis-enabled", havingValue = "true", matchIfMissing = true)
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class OpenRouterAiProvider implements AiProvider {

    private static final String PROVIDER_NAME = "openrouter";
    private static final int MAX_RETRY_ATTEMPTS = 2;

    private final OpenRouterProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public String modelName() {
        return properties.getModel();
    }

    @Override
    public AiExtractionResult extractResumeInformation(String resumeText, String promptVersion) {
        if (!properties.isAnalysisEnabled()) {
            throw new AiProviderException("AI_DISABLED", "AI resume analysis is disabled in configuration.");
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new AiProviderException("API_KEY_MISSING",
                    "OpenRouter API key is not configured. Set OPENROUTER_API_KEY.");
        }

        String prompt = buildExtractionPrompt(resumeText, promptVersion);
        String requestBody = buildRequestBody(prompt);

        // log only non-sensitive metadata
        log.info("AI extraction starting: provider={} model={} promptVersion={} resumeLength={}",
                PROVIDER_NAME, properties.getModel(), promptVersion, resumeText.length());

        String rawResponse = callWithRetry(requestBody);
        return parseExtractionResponse(rawResponse);
    }

    // ---- Private helpers ----

    private String callWithRetry(String requestBody) {
        int attempts = 0;
        while (true) {
            attempts++;
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(properties.getBaseUrl() + "/chat/completions"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + properties.getApiKey())
                        .header("HTTP-Referer", "https://github.com/ravipalaparthi2005-oss/TalentGraph")
                        .header("X-Title", "TalentGraph Resume Intelligence")
                        .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();

                log.info("AI provider response: status={} provider={}", status, PROVIDER_NAME);

                if (status == 200) {
                    return response.body();
                }

                // Don't retry auth/config errors
                if (status == 401) {
                    throw new AiProviderException("AUTH_FAILED", "OpenRouter authentication failed. Check your API key.");
                }
                if (status == 403) {
                    throw new AiProviderException("FORBIDDEN", "OpenRouter request forbidden.");
                }

                // Retry on 429 (rate limit) or 5xx (transient server error)
                if ((status == 429 || status >= 500) && attempts < MAX_RETRY_ATTEMPTS) {
                    log.warn("AI provider transient error: status={} attempt={} — retrying", status, attempts);
                    Thread.sleep(1500L * attempts);
                    continue;
                }

                throw new AiProviderException("HTTP_" + status,
                        "OpenRouter returned non-success status: " + status);

            } catch (AiProviderException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AiProviderException("INTERRUPTED", "AI request interrupted.", e);
            } catch (IOException e) {
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    log.warn("AI provider IO error on attempt {} — retrying: {}", attempts, e.getMessage());
                    try { Thread.sleep(1500L * attempts); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    throw new AiProviderException("NETWORK_ERROR",
                            "AI provider network error after " + attempts + " attempts.", e);
                }
            }
        }
    }

    private String buildExtractionPrompt(String resumeText, String promptVersion) {
        // prompt-version: resume-extraction-v1
        return """
You are a structured information extraction assistant. Extract information ONLY from the resume text provided below.

RULES (NON-NEGOTIABLE):
1. Extract ONLY information explicitly present in the resume text.
2. Do NOT invent, infer, or hallucinate skills, companies, dates, titles, projects, or education.
3. For every extracted item, include an exact short quote from the resume text.
4. If a field is not present, use null — never guess.
5. Do NOT generate a score, rating, rank, or hiring recommendation.
6. Return ONLY valid JSON matching the schema below — no markdown, no explanation.

SCHEMA:
{
  "summary": "<one sentence factual summary or null>",
  "skills": [
    {"name": "<skill>", "evidence_quote": "<exact short quote>", "source_section": "<Skills|Experience|Projects|null>"}
  ],
  "experiences": [
    {"company": "<company or null>", "job_title": "<title or null>", "start_date": "<raw string or null>",
     "end_date": "<raw string or null>", "responsibilities": "<text or null>", "evidence_quote": "<exact quote>"}
  ],
  "projects": [
    {"project_name": "<name>", "description": "<text or null>", "technologies": "<comma-separated or null>", "evidence_quote": "<exact quote>"}
  ],
  "education": [
    {"institution": "<name or null>", "degree": "<degree or null>", "field_of_study": "<field or null>",
     "start_date": "<raw string or null>", "end_date": "<raw string or null>", "grade": "<explicit grade or null>", "evidence_quote": "<exact quote>"}
  ],
  "certifications": [
    {"certification_name": "<name>", "issuing_body": "<body or null>", "issued_date": "<raw string or null>", "evidence_quote": "<exact quote>"}
  ]
}

RESUME TEXT:
""" + resumeText;
    }

    private String buildRequestBody(String prompt) {
        try {
            var messages = List.of(
                    new ChatMessage("user", prompt)
            );
            var requestMap = new java.util.LinkedHashMap<String, Object>();
            requestMap.put("model", properties.getModel());
            requestMap.put("messages", messages);
            requestMap.put("temperature", 0.0);
            requestMap.put("max_tokens", 4096);
            return objectMapper.writeValueAsString(requestMap);
        } catch (Exception e) {
            throw new AiProviderException("REQUEST_BUILD_ERROR", "Failed to build AI request body.", e);
        }
    }

    private AiExtractionResult parseExtractionResponse(String rawResponse) {
        try {
            OpenRouterResponse response = objectMapper.readValue(rawResponse, OpenRouterResponse.class);

            if (response.choices == null || response.choices.isEmpty()) {
                throw new AiProviderException("EMPTY_RESPONSE", "AI provider returned no choices.");
            }

            String content = response.choices.get(0).message != null
                    ? response.choices.get(0).message.content
                    : null;

            if (content == null || content.isBlank()) {
                throw new AiProviderException("EMPTY_CONTENT", "AI provider returned empty content.");
            }

            if (content.length() > properties.getMaxResponseChars()) {
                throw new AiProviderException("RESPONSE_TOO_LARGE", "AI response exceeded maximum size limit.");
            }

            return parseStructuredContent(content);

        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("PARSE_ERROR", "Failed to parse AI provider response.", e);
        }
    }

    private AiExtractionResult parseStructuredContent(String content) {
        try {
            // Strip possible markdown code fences
            String json = content.strip();
            if (json.startsWith("```")) {
                json = json.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").strip();
            }

            ExtractionSchema schema = objectMapper.readValue(json, ExtractionSchema.class);

            List<AiExtractionResult.ExtractedSkill> skills = new ArrayList<>();
            if (schema.skills != null) {
                for (var s : schema.skills) {
                    if (s.name != null && s.evidenceQuote != null) {
                        skills.add(AiExtractionResult.ExtractedSkill.builder()
                                .name(s.name.strip())
                                .evidenceQuote(s.evidenceQuote.strip())
                                .sourceSection(s.sourceSection)
                                .build());
                    }
                }
            }

            List<AiExtractionResult.ExtractedExperience> experiences = new ArrayList<>();
            if (schema.experiences != null) {
                for (var e : schema.experiences) {
                    if (e.evidenceQuote != null) {
                        experiences.add(AiExtractionResult.ExtractedExperience.builder()
                                .company(e.company)
                                .jobTitle(e.jobTitle)
                                .startDate(e.startDate)
                                .endDate(e.endDate)
                                .responsibilities(e.responsibilities)
                                .evidenceQuote(e.evidenceQuote.strip())
                                .build());
                    }
                }
            }

            List<AiExtractionResult.ExtractedProject> projects = new ArrayList<>();
            if (schema.projects != null) {
                for (var p : schema.projects) {
                    if (p.projectName != null && p.evidenceQuote != null) {
                        projects.add(AiExtractionResult.ExtractedProject.builder()
                                .projectName(p.projectName)
                                .description(p.description)
                                .technologies(p.technologies)
                                .evidenceQuote(p.evidenceQuote.strip())
                                .build());
                    }
                }
            }

            List<AiExtractionResult.ExtractedEducation> education = new ArrayList<>();
            if (schema.education != null) {
                for (var ed : schema.education) {
                    if (ed.evidenceQuote != null) {
                        education.add(AiExtractionResult.ExtractedEducation.builder()
                                .institution(ed.institution)
                                .degree(ed.degree)
                                .fieldOfStudy(ed.fieldOfStudy)
                                .startDate(ed.startDate)
                                .endDate(ed.endDate)
                                .grade(ed.grade)
                                .evidenceQuote(ed.evidenceQuote.strip())
                                .build());
                    }
                }
            }

            List<AiExtractionResult.ExtractedCertification> certs = new ArrayList<>();
            if (schema.certifications != null) {
                for (var c : schema.certifications) {
                    if (c.certificationName != null && c.evidenceQuote != null) {
                        certs.add(AiExtractionResult.ExtractedCertification.builder()
                                .certificationName(c.certificationName)
                                .issuingBody(c.issuingBody)
                                .issuedDate(c.issuedDate)
                                .evidenceQuote(c.evidenceQuote.strip())
                                .build());
                    }
                }
            }

            return AiExtractionResult.builder()
                    .summary(schema.summary)
                    .skills(skills)
                    .experiences(experiences)
                    .projects(projects)
                    .education(education)
                    .certifications(certs)
                    .build();

        } catch (Exception e) {
            throw new AiProviderException("MALFORMED_JSON",
                    "AI response did not match expected extraction schema.", e);
        }
    }

    // ---- Internal Jackson deserialization types ----

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OpenRouterResponse {
        public List<Choice> choices;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Choice {
        public ChatMessage message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ChatMessage {
        public String role;
        public String content;

        ChatMessage() {}
        ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ExtractionSchema {
        public String summary;
        public List<SkillItem> skills;
        public List<ExperienceItem> experiences;
        public List<ProjectItem> projects;
        public List<EducationItem> education;
        public List<CertItem> certifications;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SkillItem {
        public String name;
        @JsonProperty("evidence_quote") public String evidenceQuote;
        @JsonProperty("source_section") public String sourceSection;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ExperienceItem {
        public String company;
        @JsonProperty("job_title") public String jobTitle;
        @JsonProperty("start_date") public String startDate;
        @JsonProperty("end_date") public String endDate;
        public String responsibilities;
        @JsonProperty("evidence_quote") public String evidenceQuote;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ProjectItem {
        @JsonProperty("project_name") public String projectName;
        public String description;
        public String technologies;
        @JsonProperty("evidence_quote") public String evidenceQuote;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EducationItem {
        public String institution;
        public String degree;
        @JsonProperty("field_of_study") public String fieldOfStudy;
        @JsonProperty("start_date") public String startDate;
        @JsonProperty("end_date") public String endDate;
        public String grade;
        @JsonProperty("evidence_quote") public String evidenceQuote;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CertItem {
        @JsonProperty("certification_name") public String certificationName;
        @JsonProperty("issuing_body") public String issuingBody;
        @JsonProperty("issued_date") public String issuedDate;
        @JsonProperty("evidence_quote") public String evidenceQuote;
    }
}
