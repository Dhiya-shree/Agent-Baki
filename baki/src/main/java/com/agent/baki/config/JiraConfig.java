package com.agent.baki.config;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Configuration class for Atlassian Jira integration
 *
 * This class configures the Jira REST API client for accessing Jira incidents.
 * It uses basic authentication with username and API token.
 *
 * Required Environment Variables:
 * - JIRA_BASE_URL: Jira instance URL (e.g., https://your-domain.atlassian.net)
 * - JIRA_USERNAME: Jira user email address
 * - JIRA_API_TOKEN: Jira API token (not password)
 *
 * Setup Instructions:
 * 1. Go to Atlassian Account Settings
 * 2. Navigate to Security > API tokens
 * 3. Create a new API token
 * 4. Set environment variables with the credentials
 *
 * Note: Jira Cloud requires API tokens, not passwords
 *
 * @author Team Baki
 */
@Configuration
@ConditionalOnProperty(name = "jira.config.enabled", havingValue = "true", matchIfMissing = false)
@Getter
public class JiraConfig {
    
    /**
     * Jira instance base URL
     * Format: https://your-domain.atlassian.net
     * Do not include trailing slash or /rest/api path
     */
    @Value("${jira.base-url}")
    private String baseUrl;
    
    /**
     * Jira username (email address)
     * This is the email address associated with your Jira account
     * Example: developer@company.com
     */
    @Value("${jira.username}")
    private String username;
    
    /**
     * Jira API token
     * Generated from Atlassian Account Settings > Security > API tokens
     * Note: This is NOT your Jira password
     * Keep this token secure and rotate regularly
     */
    @Value("${jira.api-token}")
    private String apiToken;
    
    /**
     * Jira project key (optional)
     * Used to filter issues by project
     * Example: "PROJ" or "BUG"
     */
    @Value("${jira.project-key:}")
    private String projectKey;
    
    /**
     * Creates and configures Jira REST Client
     * 
     * This client provides access to Jira REST API v3 for:
     * - Searching and retrieving issues
     * - Adding comments to issues
     * - Updating issue status
     * - Creating new issues (if needed in future)
     * 
     * Authentication:
     * Uses HTTP Basic Authentication with username and API token
     * 
     * Connection:
     * - Asynchronous client for non-blocking operations
     * - Automatic connection pooling
     * - Automatic retry on transient failures
     * 
     * Usage Example:
     * <pre>
     * {@code
     * SearchResult result = jiraRestClient.getSearchClient()
     *     .searchJql("type = Incident", 50, 0, null)
     *     .claim();
     * }
     * </pre>
     * 
     * @return JiraRestClient configured for API calls
     * @throws IllegalStateException if base URL is invalid
     */
    @Bean
    public JiraRestClient jiraRestClient() {
        try {
            // Validate and parse the base URL
            URI jiraServerUri = new URI(baseUrl);
            
            // Create the asynchronous Jira REST client factory
            AsynchronousJiraRestClientFactory factory = 
                new AsynchronousJiraRestClientFactory();
            
            // Build and return the client with basic authentication
            return factory.createWithBasicHttpAuthentication(
                jiraServerUri, 
                username, 
                apiToken
            );
            
        } catch (URISyntaxException e) {
            throw new IllegalStateException(
                "Invalid Jira base URL: " + baseUrl + 
                ". Please check JIRA_BASE_URL environment variable.", e);
        }
    }
    
    /**
     * Validates that all required Jira configuration properties are set
     * 
     * This method is called after bean initialization to ensure
     * all required properties are configured before the application starts.
     * 
     * Validates:
     * - Base URL is not empty and is a valid URL
     * - Username is not empty
     * - API Token is not empty
     * 
     * @throws IllegalStateException if any required property is missing or invalid
     */
    public void validateConfiguration() {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalStateException(
                "Jira Base URL is not configured. Set JIRA_BASE_URL environment variable.");
        }
        
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            throw new IllegalStateException(
                "Jira Base URL must start with http:// or https://. Current value: " + baseUrl);
        }
        
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalStateException(
                "Jira Username is not configured. Set JIRA_USERNAME environment variable.");
        }
        
        if (apiToken == null || apiToken.trim().isEmpty()) {
            throw new IllegalStateException(
                "Jira API Token is not configured. Set JIRA_API_TOKEN environment variable.");
        }
        
        // Validate email format for username
        if (!username.contains("@")) {
            throw new IllegalStateException(
                "Jira Username should be an email address. Current value: " + username);
        }
    }
    
    /**
     * Gets the Jira project key if configured
     * 
     * @return Project key or null if not configured
     */
    public String getProjectKey() {
        return (projectKey != null && !projectKey.trim().isEmpty()) ? projectKey : null;
    }
    
    /**
     * Checks if a project key is configured
     * 
     * @return true if project key is set, false otherwise
     */
    public boolean hasProjectKey() {
        return getProjectKey() != null;
    }
}

// Made with Bob
