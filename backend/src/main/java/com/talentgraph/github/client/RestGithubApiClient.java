package com.talentgraph.github.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgraph.github.client.dto.*;
import com.talentgraph.common.exception.ResourceNotFoundException;
import com.talentgraph.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Production implementation of {@link GithubApiClient} using Java standard {@link HttpClient}.
 *
 * <p>Enforces bounded retries for transient 5xx/429 errors and protects secrets.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class RestGithubApiClient implements GithubApiClient {

    private static final int MAX_RETRY_ATTEMPTS = 2;

    private final GithubProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public GithubTokenResponseDto exchangeCodeForToken(String code, String state) {
        String url = "https://github.com/login/oauth/access_token";
        Map<String, String> bodyMap = Map.of(
                "client_id", properties.getClientId(),
                "client_secret", properties.getClientSecret(),
                "code", code,
                "redirect_uri", properties.getRedirectUri(),
                "state", state != null ? state : ""
        );

        try {
            String requestBody = objectMapper.writeValueAsString(bodyMap);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            String responseBody = sendHttpRequest(request);
            return objectMapper.readValue(responseBody, GithubTokenResponseDto.class);
        } catch (Exception e) {
            log.error("Failed to exchange GitHub authorization code for token", e);
            throw new RuntimeException("GitHub OAuth token exchange failed.", e);
        }
    }

    @Override
    public GithubUserDto getAuthenticatedUser(String accessToken) {
        String url = properties.getBaseUrl() + "/user";
        return fetchGet(url, accessToken, GithubUserDto.class);
    }

    @Override
    public GithubUserDto getUser(String username) {
        String url = properties.getBaseUrl() + "/users/" + encode(username);
        return fetchGet(url, null, GithubUserDto.class);
    }

    @Override
    public List<GithubRepoDto> listUserRepositories(String accessToken, int page, int perPage) {
        String url = String.format("%s/user/repos?type=all&sort=pushed&page=%d&per_page=%d",
                properties.getBaseUrl(), page, perPage);
        return fetchGetList(url, accessToken, new TypeReference<List<GithubRepoDto>>() {});
    }

    @Override
    public GithubRepoDto getRepository(String accessToken, String owner, String repo) {
        String url = String.format("%s/repos/%s/%s",
                properties.getBaseUrl(), encode(owner), encode(repo));
        return fetchGet(url, accessToken, GithubRepoDto.class);
    }

    @Override
    public Map<String, Long> getRepositoryLanguages(String accessToken, String owner, String repo) {
        String url = String.format("%s/repos/%s/%s/languages",
                properties.getBaseUrl(), encode(owner), encode(repo));
        try {
            String json = fetchString(url, accessToken);
            return objectMapper.readValue(json, new TypeReference<Map<String, Long>>() {});
        } catch (Exception e) {
            log.warn("Failed to fetch repository languages for {}/{}: {}", owner, repo, e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public List<GithubCommitDto> listRepositoryCommits(String accessToken, String owner, String repo, int perPage) {
        String url = String.format("%s/repos/%s/%s/commits?per_page=%d",
                properties.getBaseUrl(), encode(owner), encode(repo), perPage);
        return fetchGetList(url, accessToken, new TypeReference<List<GithubCommitDto>>() {});
    }

    @Override
    public List<GithubPullRequestDto> listRepositoryPullRequests(String accessToken, String owner, String repo, String state, int perPage) {
        String url = String.format("%s/repos/%s/%s/pulls?state=%s&per_page=%d",
                properties.getBaseUrl(), encode(owner), encode(repo), encode(state), perPage);
        return fetchGetList(url, accessToken, new TypeReference<List<GithubPullRequestDto>>() {});
    }

    // ---- Internal HTTP Helpers ----

    private <T> T fetchGet(String url, String accessToken, Class<T> responseType) {
        String body = fetchString(url, accessToken);
        try {
            return objectMapper.readValue(body, responseType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse GitHub API response.", e);
        }
    }

    private <T> List<T> fetchGetList(String url, String accessToken, TypeReference<List<T>> typeRef) {
        try {
            String body = fetchString(url, accessToken);
            return objectMapper.readValue(body, typeRef);
        } catch (Exception e) {
            log.warn("Failed to parse GitHub API list response from {}: {}", url, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String fetchString(String url, String accessToken) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github.v3+json")
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .GET();

        if (accessToken != null && !accessToken.isBlank()) {
            builder.header("Authorization", "Bearer " + accessToken);
        }

        return sendHttpRequest(builder.build());
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

                if (status == 401) {
                    throw new UnauthorizedException("GitHub API authentication failed.");
                }

                if (status == 404) {
                    throw new ResourceNotFoundException("GitHub resource not found.");
                }

                if ((status == 429 || status >= 500) && attempts < MAX_RETRY_ATTEMPTS) {
                    log.warn("GitHub API transient error status={} attempt={} — retrying", status, attempts);
                    Thread.sleep(1000L * attempts);
                    continue;
                }

                throw new RuntimeException("GitHub API returned error status: " + status);
            } catch (UnauthorizedException | ResourceNotFoundException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("GitHub API request interrupted.", e);
            } catch (IOException e) {
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    log.warn("GitHub API IO error on attempt {} — retrying: {}", attempts, e.getMessage());
                    try { Thread.sleep(1000L * attempts); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    throw new RuntimeException("GitHub API network error after " + attempts + " attempts.", e);
                }
            }
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
