package com.agent.baki.service.integration;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;

/**
 * Service for Atlassian Jira integration using Jira REST API
 *
 * This service handles all interactions with Jira:
 * - Fetching incident and bug issues
 * - Adding comments to issues
 * - Retrieving issue details
 *
 * Uses JiraRestClient configured in JiraConfig
 *
 * @author Team Baki
 */
@Service
@ConditionalOnProperty(name = "jira.config.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class JiraService {
    
    private final JiraRestClient jiraRestClient;
    
    /**
     * Fetch incident and bug issues from Jira
     *
     * Fetches open incidents and bugs (excludes Closed and Resolved)
     * Returns top 50 issues ordered by creation date (newest first)
     *
     * @return List of Issue objects containing incidents and bugs
     */
    public List<Issue> fetchIncidentIssues() {
        // BobAndMe TODO: Fetch incident and bug issues from Jira
        try {
            log.info("Fetching incident and bug issues from Jira");
            
            // Build JQL query to fetch incidents and bugs that are not closed or resolved
            String jql = "(type = Incident OR type = Bug) " +
                        "AND status != Closed " +
                        "AND status != Resolved " +
                        "ORDER BY created DESC";
            
            // Execute search with limit of 50 issues
            SearchResult searchResult = jiraRestClient.getSearchClient()
                    .searchJql(jql, 50, 0, null)
                    .claim();
            
            // Extract issues from search result
            List<Issue> issues = new ArrayList<>();
            searchResult.getIssues().forEach(issues::add);
            
            log.info("Successfully fetched {} incident/bug issues from Jira", issues.size());
            return issues;
            
        } catch (Exception e) {
            log.error("Error fetching incident issues from Jira: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Fetch a specific Jira issue by key
     *
     * @param issueKey The Jira issue key (e.g., "PROJ-123")
     * @return Issue object or null if not found
     */
    public Issue getIssueByKey(String issueKey) {
        // BobAndMe TODO: Fetch a specific Jira issue by key
        try {
            log.info("Fetching Jira issue: {}", issueKey);
            
            Issue issue = jiraRestClient.getIssueClient()
                    .getIssue(issueKey)
                    .claim();
            
            log.info("Successfully fetched Jira issue: {}", issueKey);
            return issue;
            
        } catch (Exception e) {
            log.error("Error fetching Jira issue {}: {}", issueKey, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Add a comment to a Jira issue
     *
     * @param issueKey The Jira issue key (e.g., "PROJ-123")
     * @param commentText The comment text to add
     * @return true if comment added successfully, false otherwise
     */
    public boolean addComment(String issueKey, String commentText) {
        // BobAndMe TODO: Add a comment to a Jira issue
        try {
            log.info("Adding comment to Jira issue: {}", issueKey);
            
            // Get the issue first
            Issue issue = getIssueByKey(issueKey);
            if (issue == null) {
                log.error("Cannot add comment - issue not found: {}", issueKey);
                return false;
            }
            
            // Build and add comment
            Comment comment = Comment.valueOf(commentText);
            jiraRestClient.getIssueClient()
                    .addComment(issue.getCommentsUri(), comment)
                    .claim();
            
            log.info("Successfully added comment to Jira issue: {}", issueKey);
            return true;
            
        } catch (Exception e) {
            log.error("Error adding comment to Jira issue {}: {}", issueKey, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Add a comment with fix details to a Jira issue
     *
     * Constructs formatted comment with complete fix information
     *
     * @param issueKey The Jira issue key
     * @param fixId The fix ID from database
     * @param className The problematic code class name
     * @param lineNumber The problematic code line number
     * @param status The fix status (Ignored, In Progress, DB Fix)
     * @param changeNumber The database change number (optional, can be null)
     * @return true if comment added successfully, false otherwise
     */
    public boolean addFixDetailsComment(String issueKey, Long fixId, String className,
                                       Integer lineNumber, String status, String changeNumber) {
        // BobAndMe TODO: Add a comment with fix details to a Jira issue
        log.info("Adding fix details comment to Jira issue: {} with fix ID: {}", issueKey, fixId);
        
        // Build change number section if available
        String changeNumberSection = "";
        if (changeNumber != null && !changeNumber.trim().isEmpty()) {
            changeNumberSection = String.format("- Change Number: %s%n", changeNumber);
        }
        
        // Construct formatted comment text
        String commentText = String.format(
            "Incident Analysis Complete%n%n" +
            "Fix Details:%n" +
            "- Fix ID: %d%n" +
            "- Code Location: %s (Line %d)%n" +
            "- Status: %s%n" +
            "%s%n" +
            "The issue has been addressed.%n%n" +
            "- Agent Baki (Automated Incident Management System)",
            fixId, className, lineNumber, status, changeNumberSection
        );
        
        return addComment(issueKey, commentText);
    }
    
    /**
     * Extract issue summary (title) from Issue object
     *
     * @param issue The Jira issue
     * @return Issue summary or empty string
     */
    public String extractIssueSummary(Issue issue) {
        // BobAndMe TODO: Extract issue summary (title) from Issue object
        try {
            if (issue != null && issue.getSummary() != null) {
                return issue.getSummary();
            }
        } catch (Exception e) {
            log.warn("Error extracting issue summary: {}", e.getMessage());
        }
        return "";
    }
    
    /**
     * Extract issue description from Issue object
     *
     * @param issue The Jira issue
     * @return Issue description or empty string
     */
    public String extractIssueDescription(Issue issue) {
        // BobAndMe TODO: Extract issue description from Issue object
        try {
            if (issue != null && issue.getDescription() != null) {
                return issue.getDescription();
            }
        } catch (Exception e) {
            log.warn("Error extracting issue description: {}", e.getMessage());
        }
        return "";
    }
    
    /**
     * Extract issue reporter name from Issue object
     *
     * @param issue The Jira issue
     * @return Reporter name or "Unknown" as default
     */
    public String extractReporterName(Issue issue) {
        // BobAndMe TODO: Extract issue reporter name from Issue object
        try {
            if (issue != null && issue.getReporter() != null &&
                issue.getReporter().getDisplayName() != null) {
                return issue.getReporter().getDisplayName();
            }
        } catch (Exception e) {
            log.warn("Error extracting reporter name: {}", e.getMessage());
        }
        return "Unknown";
    }
    
    /**
     * Check if issue has attachments
     *
     * @param issue The Jira issue
     * @return true if issue has attachments, false otherwise
     */
    public boolean hasAttachments(Issue issue) {
        // BobAndMe TODO: Check if issue has attachments
        try {
            if (issue != null && issue.getAttachments() != null) {
                return issue.getAttachments().iterator().hasNext();
            }
        } catch (Exception e) {
            log.warn("Error checking attachments: {}", e.getMessage());
        }
        return false;
    }
}

// Made with Bob