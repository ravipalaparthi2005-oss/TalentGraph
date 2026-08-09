package com.talentgraph.github.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Public response DTO for candidate GitHub identity status.
 *
 * <p>CRITICAL SECURITY RULE: The OAuth access token is NEVER included in this response.
 */
@Getter
@Builder
public class GithubIdentityResponse {

    private UUID id;
    private UUID candidateId;
    private Long githubUserId;
    private String login;
    private String profileUrl;
    private boolean connected;
    private String connectedAt;
    private String lastSyncedAt;
    private String syncStatus;
    private Integer repositoriesProcessed;
    private Integer observationsCreated;
}
