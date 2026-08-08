package com.talentgraph.ai;

/**
 * Abstraction over any external AI provider.
 *
 * <p>The rest of the application depends on this interface, not on any
 * provider-specific implementation. Replacing OpenRouter with Anthropic,
 * OpenAI, Azure OpenAI, or a local model requires only a new implementation
 * of this interface — no changes to the domain logic.
 *
 * <p>IMPORTANT: The AI provider is an extraction assistant, never an
 * authoritative source of truth. All AI outputs must be validated against
 * the source document before being stored as evidence.
 */
public interface AiProvider {

    /**
     * Extract structured resume information from raw resume text.
     *
     * @param resumeText the full plain-text content of the resume
     * @param promptVersion the versioned prompt identifier (e.g. "resume-extraction-v1")
     * @return structured extraction result; never null
     * @throws AiProviderException if the provider is unavailable or returns an unusable response
     */
    AiExtractionResult extractResumeInformation(String resumeText, String promptVersion);

    /**
     * Return the canonical name of this provider (e.g. "openrouter").
     */
    String providerName();

    /**
     * Return the model identifier currently configured (e.g. "openrouter/free").
     */
    String modelName();
}
