package com.talentgraph.assessment.judge0;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgraph.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Production Judge0 execution provider using HTTP REST API.
 *
 * <p>Supports hosted RapidAPI Judge0 and self-hosted Judge0 instances.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class Judge0CodeExecutionProvider implements CodeExecutionProvider {

    private static final int MAX_RETRY_ATTEMPTS = 2;

    private final Judge0Properties properties;
    private final ObjectMapper objectMapper;

    @Override
    public Judge0SubmissionResponseDto submitCode(String sourceCode, int languageId, String stdin, String expectedOutput, Double cpuTimeLimitSeconds, Integer memoryLimitMb) {
        String url = properties.getBaseUrl() + "/submissions?base64_encoded=false&wait=false";

        Judge0SubmissionRequestDto body = Judge0SubmissionRequestDto.builder()
                .sourceCode(sourceCode)
                .languageId(languageId)
                .stdin(stdin)
                .expectedOutput(expectedOutput)
                .cpuTimeLimit(cpuTimeLimitSeconds != null ? cpuTimeLimitSeconds : 2.0)
                .memoryLimit(memoryLimitMb != null ? memoryLimitMb * 1024 : 256000)
                .build();

        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = buildHttpRequest(url)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            String responseBody = sendHttpRequest(request);
            return objectMapper.readValue(responseBody, Judge0SubmissionResponseDto.class);
        } catch (Exception e) {
            log.error("Failed to submit code to Judge0 API", e);
            throw new RuntimeException("Judge0 code submission failed.", e);
        }
    }

    @Override
    public Judge0SubmissionResponseDto getSubmissionResult(String token) {
        String url = String.format("%s/submissions/%s?base64_encoded=false", properties.getBaseUrl(), token);
        HttpRequest request = buildHttpRequest(url).GET().build();

        try {
            String responseBody = sendHttpRequest(request);
            return objectMapper.readValue(responseBody, Judge0SubmissionResponseDto.class);
        } catch (Exception e) {
            log.error("Failed to fetch Judge0 submission result for token={}", token, e);
            throw new RuntimeException("Failed to fetch Judge0 result.", e);
        }
    }

    @Override
    public Judge0SubmissionResponseDto submitAndPoll(String sourceCode, int languageId, String stdin, String expectedOutput, Double cpuTimeLimitSeconds, Integer memoryLimitMb) {
        Judge0SubmissionResponseDto initial = submitCode(sourceCode, languageId, stdin, expectedOutput, cpuTimeLimitSeconds, memoryLimitMb);
        if (initial == null || initial.getToken() == null) {
            throw new IllegalStateException("Judge0 did not return a submission token.");
        }

        String token = initial.getToken();
        int attempts = 0;

        while (attempts < properties.getMaxPollingAttempts()) {
            attempts++;
            try {
                Thread.sleep(properties.getPollingIntervalMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Judge0 polling interrupted.", e);
            }

            Judge0SubmissionResponseDto result = getSubmissionResult(token);
            if (result != null && result.getStatus() != null && result.getStatus().getId() != null) {
                int statusId = result.getStatus().getId();
                // Judge0 status: 1 = In Queue, 2 = Processing
                if (statusId != 1 && statusId != 2) {
                    return result;
                }
            }
        }

        log.warn("Judge0 submission polling timed out after {} attempts for token={}", attempts, token);
        return Judge0SubmissionResponseDto.builder()
                .token(token)
                .status(new Judge0SubmissionResponseDto.Status(13, "EXECUTION_TIMEOUT"))
                .message("Execution timed out waiting for Judge0 response.")
                .build();
    }

    private HttpRequest.Builder buildHttpRequest(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()));

        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            builder.header("X-RapidAPI-Key", properties.getApiKey());
        }
        if (properties.getRapidApiHost() != null && !properties.getRapidApiHost().isBlank()) {
            builder.header("X-RapidAPI-Host", properties.getRapidApiHost());
        }

        return builder;
    }

    private String sendHttpRequest(HttpRequest request) {
        int attempts = 0;
        while (true) {
            attempts++;
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();

                if (status >= 200 && status < 300) {
                    return response.body();
                }

                if (status == 401 || status == 403) {
                    throw new UnauthorizedException("Judge0 API authentication failed.");
                }

                if ((status == 429 || status >= 500) && attempts < MAX_RETRY_ATTEMPTS) {
                    log.warn("Judge0 API transient error status={} attempt={} — retrying", status, attempts);
                    Thread.sleep(1000L * attempts);
                    continue;
                }

                throw new RuntimeException("Judge0 API returned status: " + status);
            } catch (UnauthorizedException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Judge0 request interrupted.", e);
            } catch (IOException e) {
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    log.warn("Judge0 IO error attempt {} — retrying: {}", attempts, e.getMessage());
                    try { Thread.sleep(1000L * attempts); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    throw new RuntimeException("Judge0 network error after " + attempts + " attempts.", e);
                }
            }
        }
    }
}
