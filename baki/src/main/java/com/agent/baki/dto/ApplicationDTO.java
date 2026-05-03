package com.agent.baki.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Application entity
 * Used for REST API request/response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDTO {
    
    private Long applicationId;
    
    @NotBlank(message = "Application name is required")
    @Size(max = 255, message = "Application name must not exceed 255 characters")
    private String applicationName;
    
    @Size(max = 500, message = "Repository link must not exceed 500 characters")
    private String repositoryLink;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Summary statistics
    private Long totalFixes;
    private Long pendingFixes;
    private Long inProgressFixes;
    private Long resolvedFixes;
    private Long ignoredFixes;
    private Long dbFixes;
    
    // List of fixes (optional, for detailed view)
    private List<FixDTO> fixes;
}

// Made with Bob
