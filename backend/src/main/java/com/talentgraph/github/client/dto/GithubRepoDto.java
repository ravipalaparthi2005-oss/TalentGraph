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
public class GithubRepoDto {

    private Long id;
    private String name;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("html_url")
    private String htmlUrl;

    private String description;

    @JsonProperty("private")
    private Boolean isPrivate;

    @JsonProperty("fork")
    private Boolean isFork;

    @JsonProperty("default_branch")
    private String defaultBranch;

    private String language;

    @JsonProperty("stargazers_count")
    private Integer stargazersCount;

    @JsonProperty("forks_count")
    private Integer forksCount;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("pushed_at")
    private Instant pushedAt;

    private Owner owner;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Owner {
        private String login;
    }
}
