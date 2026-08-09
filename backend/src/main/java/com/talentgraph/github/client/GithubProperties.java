package com.talentgraph.github.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "github")
@Getter
@Setter
public class GithubProperties {

    /** GitHub OAuth Client ID */
    private String clientId = "";

    /** GitHub OAuth Client Secret — NEVER exposed to frontend or logged */
    private String clientSecret = "";

    /** OAuth Redirect URI */
    private String redirectUri = "http://localhost:8080/api/v1/github/oauth/callback";

    /** Base URL for GitHub API */
    private String baseUrl = "https://api.github.com";

    /** Max repositories to sync per candidate */
    private int maxRepositories = 50;

    /** Max commits to sync per repository */
    private int maxCommitsPerRepo = 100;

    /** Max pull requests to sync per repository */
    private int maxPullRequestsPerRepo = 100;

    /** HTTP connect and read timeout in seconds */
    private int timeoutSeconds = 30;

    /** Token encryption secret key */
    private String encryptionSecret = "talentgraph-default-32-byte-github-encryption-secret-key!";
}
