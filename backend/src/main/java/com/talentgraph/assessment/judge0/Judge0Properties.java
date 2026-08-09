package com.talentgraph.assessment.judge0;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "judge0")
@Getter
@Setter
public class Judge0Properties {

    /** Judge0 API Base URL */
    private String baseUrl = "https://judge0-ce.p.rapidapi.com";

    /** RapidAPI or self-hosted Judge0 API key — NEVER exposed to frontend */
    private String apiKey = "";

    /** RapidAPI host header */
    private String rapidApiHost = "judge0-ce.p.rapidapi.com";

    /** Polling interval in milliseconds */
    private long pollingIntervalMs = 1000L;

    /** Max polling attempts before returning EXECUTION_TIMEOUT */
    private int maxPollingAttempts = 20;

    /** HTTP connect and read timeout in seconds */
    private int timeoutSeconds = 30;

    /** Max source code size in KB (default 64KB) */
    private int maxSourceCodeKb = 64;

    /** Max submissions per attempt question */
    private int maxSubmissionsPerQuestion = 10;
}
