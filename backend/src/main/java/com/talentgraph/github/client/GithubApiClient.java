package com.talentgraph.github.client;

import com.talentgraph.github.client.dto.*;

import java.util.List;
import java.util.Map;

/**
 * Backend abstraction for GitHub REST API calls.
 */
public interface GithubApiClient {

    /** Exchange OAuth authorization code for access token */
    GithubTokenResponseDto exchangeCodeForToken(String code, String state);

    /** Get authenticated user profile using access token */
    GithubUserDto getAuthenticatedUser(String accessToken);

    /** Get user profile by login username */
    GithubUserDto getUser(String username);

    /** List user repositories (supports pagination up to maxLimit) */
    List<GithubRepoDto> listUserRepositories(String accessToken, int page, int perPage);

    /** Get repository details */
    GithubRepoDto getRepository(String accessToken, String owner, String repo);

    /** List repository language bytes breakdown */
    Map<String, Long> getRepositoryLanguages(String accessToken, String owner, String repo);

    /** List recent commits in a repository */
    List<GithubCommitDto> listRepositoryCommits(String accessToken, String owner, String repo, int perPage);

    /** List recent pull requests in a repository */
    List<GithubPullRequestDto> listRepositoryPullRequests(String accessToken, String owner, String repo, String state, int perPage);
}
