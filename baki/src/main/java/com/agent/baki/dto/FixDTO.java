package com.agent.baki.dto;

import com.agent.baki.entity.IssueStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Fix entity
 * Used for REST API request/response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FixDTO {
    
    private Long fixId;
    
    @NotNull(message = "Application ID is required")
    private Long applicationId;
    
    private String applicationName;
    
    @Size(max = 250, message = "Issue summary must not exceed 250 characters")
    private String issueSummary;
    
    @Size(max = 255, message = "Code class name must not exceed 255 characters")
    private String codeClassName;
    
    private Integer codeLine;
    
    @Size(max = 255, message = "GitHub PR must not exceed 255 characters")
    private String githubPr;
    
    @NotNull(message = "Issue status is required")
    private IssueStatus issueStatus;
    
    private String reason;
    
    @Size(max = 100, message = "Change number must not exceed 100 characters")
    private String changeNumber;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Counts for linked incidents
    private Integer mailCount;
    private Integer jiraCount;
}

// Made with Bob
