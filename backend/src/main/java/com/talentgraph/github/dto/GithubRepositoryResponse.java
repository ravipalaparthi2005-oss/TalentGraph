package com.talentgraph.github.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class GithubRepositoryResponse {

    private UUID id;
    private Long githubRepositoryId;
    private String ownerLogin;
    private String name;
    private String fullName;
    private String htmlUrl;
    private String description;
    private boolean isPrivate;
    private boolean isFork;
    private String defaultBranch;
    private String primaryLanguage;
    private int starsCount;
    private int forksCount;
    private String createdAtGithub;
    private String updatedAtGithub;
    private String pushedAtGithub;
    private String lastSyncedAt;

    private Map<String, Long> languages;
    private List<CommitSummary> recentCommits;
    private List<PullRequestSummary> recentPullRequests;

    @Getter
    @Builder
    public static class CommitSummary {
        private String sha;
        private String authorLogin;
        private String message;
        private String commitUrl;
        private String committedAt;
    }

    @Getter
    @Builder
    public static class PullRequestSummary {
        private Long prId;
        private int number;
        private String title;
        private String state;
        private String authorLogin;
        private String htmlUrl;
        private String createdAt;
        private String mergedAt;
    }
}
