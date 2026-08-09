package com.talentgraph;

import com.talentgraph.ai.AiExtractionResult;
import com.talentgraph.ai.AiProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Test-profile AI provider configuration.
 *
 * <p>Provides a no-op {@link AiProvider} bean that NEVER makes real HTTP calls.
 * This allows ALL test classes to share a single Spring application context,
 * avoiding the context reload overhead that occurs when @MockBean is used.
 *
 * <p>Tests that need specific mock behavior should use Mockito.when() to
 * configure the mock at the method level via the {@code @Autowired AiProvider} field.
 */
@TestConfiguration
@Profile("test")
public class TestAiProviderConfiguration {

    /**
     * No-op AI provider for test profile.
     * Returns a minimal valid extraction result — never calls any external API.
     * Individual tests override this via Mockito if they need specific behavior.
     */
    @Bean
    @Primary
    public AiProvider testAiProvider() {
        return new AiProvider() {
            @Override
            public AiExtractionResult extractResumeInformation(String resumeText, String promptVersion) {
                return AiExtractionResult.builder()
                        .summary("Test summary")
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
        };
    }
}
