package com.talentgraph.github.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class GithubSyncResponse {

    private UUID syncRunId;
    private String status;
    private String startedAt;
    private String completedAt;
    private int repositoriesProcessed;
    private int observationsCreated;
    private String errorCode;
    private String errorMessage;
}
