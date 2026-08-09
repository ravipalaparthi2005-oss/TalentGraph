package com.talentgraph.ai;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * No-op AI provider active only in the "test" Spring profile.
 *
 * <p>Replaces {@code OpenRouterAiProvider} (excluded via {@code @Profile("!test")}) so
 * all test classes share a single Spring application context without making real HTTP calls.
 *
 * <p>Tests that need custom behavior call {@link #setNextResult(AiExtractionResult)}
 * before invoking the service under test.
 */
@Component
@Profile("test")
public class TestAiProvider implements AiProvider {

    private volatile AiExtractionResult nextResult;

    /**
     * Configure the result that will be returned by the next call to
     * {@link #extractResumeInformation}. After it is consumed, the provider
     * resets to the default empty result.
     */
    public void setNextResult(AiExtractionResult result) {
        this.nextResult = result;
    }

    @Override
    public AiExtractionResult extractResumeInformation(String resumeText, String promptVersion) {
        AiExtractionResult result = nextResult;
        nextResult = null; // consume once
        if (result != null) return result;

        // Default: empty extraction
        return AiExtractionResult.builder()
                .summary("Test-profile AI extraction")
                .skills(List.of())
                .experiences(List.of())
                .projects(List.of())
                .education(List.of())
                .certifications(List.of())
                .build();
    }

    @Override
    public String providerName() { return "test-no-op"; }

    @Override
    public String modelName() { return "test-no-op-model"; }
}
