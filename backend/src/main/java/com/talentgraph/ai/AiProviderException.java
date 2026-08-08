package com.talentgraph.ai;

/**
 * Thrown when an AI provider call cannot be completed successfully.
 *
 * <p>Callers should treat this as a transient or configuration failure and
 * must NOT propagate AI error details to end-users or include API secrets.
 */
public class AiProviderException extends RuntimeException {

    private final String errorCode;

    public AiProviderException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AiProviderException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
