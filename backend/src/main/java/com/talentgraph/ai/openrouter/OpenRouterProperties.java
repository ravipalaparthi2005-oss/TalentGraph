package com.talentgraph.ai.openrouter;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OpenRouter provider configuration.
 *
 * <p>All values are read from environment variables / application properties.
 * The API key is NEVER logged, serialized to JSON, or exposed to the frontend.
 *
 * <p>Environment variables:
 * <pre>
 *   OPENROUTER_API_KEY    - required when AI_RESUME_ANALYSIS_ENABLED=true
 *   OPENROUTER_BASE_URL   - defaults to https://openrouter.ai/api/v1
 *   OPENROUTER_MODEL      - defaults to openrouter/free
 *   OPENROUTER_TIMEOUT_SECONDS - defaults to 60
 *   AI_RESUME_ANALYSIS_ENABLED - defaults to true
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "openrouter")
@Getter
@Setter
public class OpenRouterProperties {

    /** OpenRouter API key — never logged or exposed to frontend. */
    private String apiKey = "";

    /** Base URL for the OpenRouter API. */
    private String baseUrl = "https://openrouter.ai/api/v1";

    /** Model identifier. Keep configurable — don't hardcode a specific free model. */
    private String model = "openrouter/free";

    /** HTTP connect + read timeout in seconds. */
    private int timeoutSeconds = 60;

    /** Maximum response body size in characters (safety guard). */
    private int maxResponseChars = 32_000;

    /**
     * Whether AI analysis is enabled globally.
     * When false, uploads still parse deterministically; AI analysis returns unavailable.
     */
    private boolean analysisEnabled = true;
}
