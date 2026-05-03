package com.agent.baki.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating fix status
 * Used for REST API requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FixUpdateRequest {
    
    @Size(max = 255, message = "GitHub PR must not exceed 255 characters")
    private String githubPr;
    
    private String reason;
    
    @Size(max = 100, message = "Change number must not exceed 100 characters")
    private String changeNumber;
}

// Made with Bob
