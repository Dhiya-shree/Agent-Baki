package com.agent.baki.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for log analysis results
 * 
 * Contains the AI-identified problematic code location
 * 
 * @author Team Baki
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogAnalysisResult {
    
    /**
     * The problematic code class name
     * Example: "com.example.service.UserService"
     */
    private String className;
    
    /**
     * The problematic code line number
     * Example: 45
     */
    private Integer lineNumber;
    
    /**
     * Error description or summary
     * Example: "NullPointerException in getUserById method"
     */
    private String errorDescription;
    
    /**
     * Full AI analysis response
     * Contains complete analysis from Watsonx AI
     */
    private String fullAnalysis;
    
    /**
     * Indicates if analysis was successful
     */
    private boolean successful;
    
    /**
     * Error message if analysis failed
     */
    private String errorMessage;
    
    /**
     * Check if analysis result is valid
     * 
     * @return true if className and lineNumber are present
     */
    public boolean isValid() {
        return successful && 
               className != null && !className.trim().isEmpty() &&
               lineNumber != null && lineNumber > 0;
    }
}

// Made with Bob