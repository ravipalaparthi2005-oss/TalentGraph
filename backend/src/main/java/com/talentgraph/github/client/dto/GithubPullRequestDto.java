package com.talentgraph.github.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubPullRequestDto {

    private Long id;
    private Integer number;
    private String title;
    private String state;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("merged_at")
    private Instant mergedAt;

    private User user;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        private String login;
    }
}
