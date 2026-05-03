package com.agent.baki.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * GitHub Configuration
 * 
 * Loads GitHub credentials and settings from application.properties
 * 
 * Properties:
 * - github.username: GitHub username
 * - github.token: GitHub personal access token
 * - github.api.url: GitHub API base URL (default: https://api.github.com)
 * 
 * @author Team Baki
 */
@Configuration
@ConfigurationProperties(prefix = "github")
@Getter
@Setter
public class GithubConfig {
    
    /**
     * GitHub username for authentication
     */
    private String username;
    
    /**
     * GitHub personal access token for API authentication
     * Generate from: GitHub Settings > Developer settings > Personal access tokens
     * Required scopes: repo (for private repos), public_repo (for public repos)
     */
    private String token;
    
    /**
     * GitHub API base URL
     * Default: https://api.github.com
     */
    private String apiUrl = "https://api.github.com";
    
    /**
     * Default branch to check for updates
     * Can be overridden by application.repositoryBranch
     */
    private String defaultBranch = "UAT";
}

// Made with Bob
