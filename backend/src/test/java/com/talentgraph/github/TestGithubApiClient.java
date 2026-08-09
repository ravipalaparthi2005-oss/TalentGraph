package com.talentgraph.github;

import com.talentgraph.github.client.GithubApiClient;
import com.talentgraph.github.client.dto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Deterministic test-profile implementation of {@link GithubApiClient}.
 *
 * <p>Never makes real external HTTP calls. Allows tests to run fast and predictably.
 */
@Component
@Profile("test")
public class TestGithubApiClient implements GithubApiClient {

    @Override
    public GithubTokenResponseDto exchangeCodeForToken(String code, String state) {
        return GithubTokenResponseDto.builder()
                .accessToken("test-oauth-access-token-12345")
                .tokenType("bearer")
                .scope("read:user,repo")
                .build();
    }

    @Override
    public GithubUserDto getAuthenticatedUser(String accessToken) {
        return GithubUserDto.builder()
                .id(99887766L)
                .login("test-candidate-gh")
                .name("Test Candidate GitHub")
                .email("candidate@github.test")
                .avatarUrl("https://github.com/avatars/test.jpg")
                .htmlUrl("https://github.com/test-candidate-gh")
                .publicRepos(5)
                .build();
    }

    @Override
    public GithubUserDto getUser(String username) {
        return getAuthenticatedUser("dummy");
    }

    @Override
    public List<GithubRepoDto> listUserRepositories(String accessToken, int page, int perPage) {
        return List.of(
                GithubRepoDto.builder()
                        .id(1001L)
                        .name("talentgraph-demo")
                        .fullName("test-candidate-gh/talentgraph-demo")
                        .htmlUrl("https://github.com/test-candidate-gh/talentgraph-demo")
                        .description("Microservice architecture with Spring Boot & PostgreSQL")
                        .isPrivate(false)
                        .isFork(false)
                        .defaultBranch("main")
                        .language("Java")
                        .stargazersCount(42)
                        .forksCount(10)
                        .createdAt(Instant.parse("2024-01-15T10:00:00Z"))
                        .updatedAt(Instant.parse("2026-08-01T12:00:00Z"))
                        .pushedAt(Instant.parse("2026-08-05T15:30:00Z"))
                        .owner(new GithubRepoDto.Owner("test-candidate-gh"))
                        .build(),
                GithubRepoDto.builder()
                        .id(1002L)
                        .name("unknown-tech-lab")
                        .fullName("test-candidate-gh/unknown-tech-lab")
                        .htmlUrl("https://github.com/test-candidate-gh/unknown-tech-lab")
                        .description("Experimental project using SuperObscureLangXYZ99")
                        .isPrivate(false)
                        .isFork(false)
                        .defaultBranch("main")
                        .language("SuperObscureLangXYZ99")
                        .stargazersCount(5)
                        .forksCount(0)
                        .createdAt(Instant.parse("2025-03-01T10:00:00Z"))
                        .updatedAt(Instant.parse("2026-07-01T12:00:00Z"))
                        .pushedAt(Instant.parse("2026-07-10T15:30:00Z"))
                        .owner(new GithubRepoDto.Owner("test-candidate-gh"))
                        .build()
        );
    }

    @Override
    public GithubRepoDto getRepository(String accessToken, String owner, String repo) {
        return listUserRepositories(accessToken, 1, 10).get(0);
    }

    @Override
    public Map<String, Long> getRepositoryLanguages(String accessToken, String owner, String repo) {
        if ("talentgraph-demo".equals(repo)) {
            return Map.of("Java", 125000L, "Spring Boot", 45000L);
        }
        return Map.of("SuperObscureLangXYZ99", 50000L);
    }

    @Override
    public List<GithubCommitDto> listRepositoryCommits(String accessToken, String owner, String repo, int perPage) {
        return List.of(
                GithubCommitDto.builder()
                        .sha("a1b2c3d4e5f67890")
                        .htmlUrl("https://github.com/test-candidate-gh/talentgraph-demo/commit/a1b2c3d4e5f67890")
                        .author(new GithubCommitDto.Author("test-candidate-gh"))
                        .commit(new GithubCommitDto.CommitDetails(
                                "feat: implement Evidence Graph mapping",
                                new GithubCommitDto.CommitAuthor("Test Candidate", "candidate@test.com", Instant.parse("2026-08-05T15:00:00Z"))
                        ))
                        .build()
        );
    }

    @Override
    public List<GithubPullRequestDto> listRepositoryPullRequests(String accessToken, String owner, String repo, String state, int perPage) {
        return List.of(
                GithubPullRequestDto.builder()
                        .id(889911L)
                        .number(14)
                        .title("feat: Add OAuth integration")
                        .state("closed")
                        .htmlUrl("https://github.com/test-candidate-gh/talentgraph-demo/pull/14")
                        .createdAt(Instant.parse("2026-08-02T10:00:00Z"))
                        .mergedAt(Instant.parse("2026-08-03T11:00:00Z"))
                        .user(new GithubPullRequestDto.User("test-candidate-gh"))
                        .build()
        );
    }
}
