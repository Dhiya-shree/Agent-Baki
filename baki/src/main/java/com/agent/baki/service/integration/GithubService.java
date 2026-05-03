package com.agent.baki.service.integration;

import com.agent.baki.config.GithubConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Base64;

/**
 * GitHub Integration Service
 * 
 * Provides methods to interact with GitHub API for checking recent updates
 * 
 * @author Team Baki
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GithubService {
    
    private final GithubConfig githubConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Check for recent updates in a specific file and line range
     * 
     * Searches GitHub commits for changes to the specified file and line number
     * Returns PR links and commit messages if found
     * 
     * @param repositoryUrl Full repository URL (e.g., https://github.com/owner/repo)
     * @param branch Branch to check (default: UAT)
     * @param filePath File path in repository (e.g., src/main/java/com/example/MyClass.java)
     * @param lineNumber Line number to check
     * @return JSON string with PR links and commit messages, or null if no updates found
     */
    public String checkRecentUpdates(String repositoryUrl, String branch, String filePath, int lineNumber) {
        try {
            log.info("Checking GitHub for recent updates: repo={}, branch={}, file={}, line={}", 
                    repositoryUrl, branch, filePath, lineNumber);
            
            // Extract owner and repo from URL
            String[] parts = extractOwnerAndRepo(repositoryUrl);
            if (parts == null) {
                log.error("Invalid repository URL: {}", repositoryUrl);
                return null;
            }
            
            String owner = parts[0];
            String repo = parts[1];
            String branchToUse = (branch != null && !branch.isEmpty()) ? branch : githubConfig.getDefaultBranch();
            
            // Get recent commits for the file
            String commitsUrl = String.format("%s/repos/%s/%s/commits?path=%s&sha=%s&per_page=10",
                    githubConfig.getApiUrl(), owner, repo, filePath, branchToUse);
            
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    commitsUrl, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseCommitsForLineChanges(response.getBody(), owner, repo, filePath, lineNumber);
            }
            
            log.warn("No commits found for file: {}", filePath);
            return null;
            
        } catch (Exception e) {
            log.error("Error checking GitHub updates: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Extract owner and repository name from GitHub URL
     * 
     * @param repositoryUrl Full repository URL
     * @return Array with [owner, repo] or null if invalid
     */
    private String[] extractOwnerAndRepo(String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.isEmpty()) {
            return null;
        }
        
        try {
            // Remove .git suffix if present
            String url = repositoryUrl.replace(".git", "");
            
            // Handle both HTTPS and SSH URLs
            // HTTPS: https://github.com/owner/repo
            // SSH: git@github.com:owner/repo
            
            if (url.contains("github.com/")) {
                String[] parts = url.split("github.com/")[1].split("/");
                if (parts.length >= 2) {
                    return new String[]{parts[0], parts[1]};
                }
            } else if (url.contains("github.com:")) {
                String[] parts = url.split("github.com:")[1].split("/");
                if (parts.length >= 2) {
                    return new String[]{parts[0], parts[1]};
                }
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Error parsing repository URL: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Create HTTP headers with GitHub authentication
     * 
     * @return HttpHeaders with authorization
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/vnd.github.v3+json");
        
        // Use token authentication
        if (githubConfig.getToken() != null && !githubConfig.getToken().isEmpty()) {
            headers.set("Authorization", "token " + githubConfig.getToken());
        } else if (githubConfig.getUsername() != null && !githubConfig.getUsername().isEmpty()) {
            // Fallback to basic auth (not recommended)
            String auth = githubConfig.getUsername() + ":";
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);
        }
        
        return headers;
    }
    
    /**
     * Parse commits JSON and find changes related to specific line
     * 
     * @param commitsJson JSON response from GitHub commits API
     * @param owner Repository owner
     * @param repo Repository name
     * @param filePath File path
     * @param lineNumber Line number to check
     * @return Formatted string with PR links and commit messages
     */
    private String parseCommitsForLineChanges(String commitsJson, String owner, String repo, 
                                             String filePath, int lineNumber) {
        try {
            JSONArray commits = new JSONArray(commitsJson);
            
            if (commits.length() == 0) {
                return null;
            }
            
            StringBuilder result = new StringBuilder();
            result.append("Recent updates found:\n\n");
            
            int foundCount = 0;
            for (int i = 0; i < Math.min(commits.length(), 5); i++) {
                JSONObject commit = commits.getJSONObject(i);
                
                String sha = commit.getString("sha");
                String message = commit.getJSONObject("commit").getString("message");
                String date = commit.getJSONObject("commit").getJSONObject("committer").getString("date");
                String commitUrl = commit.getString("html_url");
                
                // Check if commit has associated PR
                String prInfo = checkCommitForPR(owner, repo, sha);
                
                result.append(String.format("Commit: %s\n", sha.substring(0, 7)));
                result.append(String.format("Date: %s\n", date));
                result.append(String.format("Message: %s\n", message.split("\n")[0]));
                result.append(String.format("URL: %s\n", commitUrl));
                
                if (prInfo != null) {
                    result.append(prInfo);
                }
                
                result.append("\n");
                foundCount++;
            }
            
            return foundCount > 0 ? result.toString() : null;
            
        } catch (Exception e) {
            log.error("Error parsing commits: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Check if a commit is associated with a Pull Request
     * 
     * @param owner Repository owner
     * @param repo Repository name
     * @param sha Commit SHA
     * @return PR information string or null
     */
    private String checkCommitForPR(String owner, String repo, String sha) {
        try {
            String prUrl = String.format("%s/repos/%s/%s/commits/%s/pulls",
                    githubConfig.getApiUrl(), owner, repo, sha);
            
            HttpHeaders headers = createAuthHeaders();
            headers.set("Accept", "application/vnd.github.groot-preview+json");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    prUrl, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JSONArray prs = new JSONArray(response.getBody());
                
                if (prs.length() > 0) {
                    JSONObject pr = prs.getJSONObject(0);
                    int prNumber = pr.getInt("number");
                    String prTitle = pr.getString("title");
                    String prHtmlUrl = pr.getString("html_url");
                    
                    return String.format("PR #%d: %s\nPR URL: %s\n", prNumber, prTitle, prHtmlUrl);
                }
            }
            
            return null;
            
        } catch (Exception e) {
            log.debug("No PR found for commit: {}", sha);
            return null;
        }
    }
}

// Made with Bob
