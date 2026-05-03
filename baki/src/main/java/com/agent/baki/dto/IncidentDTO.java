package com.agent.baki.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Data Transfer Object for unified incident representation
 * 
 * Encapsulates data from both:
 * - Outlook emails (com.microsoft.graph.models.Message)
 * - Jira issues (com.atlassian.jira.rest.client.api.domain.Issue)
 * 
 * Provides common interface for data processing services
 * 
 * @author Team Baki
 */
@Data
@Builder
@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class IncidentDTO {
    
    // Source identification
    /**
     * Source type: "EMAIL" or "JIRA"
     */
    private IncidentSource source;
    
    /**
     * Unique identifier from source system
     * - For Email: Message ID
     * - For Jira: Issue Key (e.g., "PROJ-123")
     */
    private String sourceId;
    
    // Common incident information
    /**
     * Incident title/subject
     * - For Email: Email subject
     * - For Jira: Issue summary
     */
    private String title;
    
    /**
     * Incident description/body
     * - For Email: Email body content
     * - For Jira: Issue description
     */
    private String description;
    
    /**
     * Reporter/sender name
     * - For Email: Sender name
     * - For Jira: Reporter display name
     */
    private String reporterName;
    
    /**
     * Reporter/sender email
     * - For Email: Sender email address
     * - For Jira: Reporter email (if available)
     */
    private String reporterEmail;
    
    /**
     * Incident creation/received date
     * - For Email: Received date time
     * - For Jira: Created date time
     */
    private LocalDateTime createdDate;
    
    // Application and log information
    /**
     * Application name extracted from incident
     * Null if not yet extracted
     */
    private String applicationName;
    
    /**
     * Indicates if incident has attachments
     */
    private boolean hasAttachments;
    
    /**
     * Log file attachments
     * Map of filename to content bytes
     * Empty if no log files attached
     */
    @Builder.Default
    private Map<String, byte[]> logFiles = new HashMap<>();
    
    // Processing status
    /**
     * Indicates if application name is present
     */
    private boolean hasApplicationName;
    
    /**
     * Indicates if log files are present
     */
    private boolean hasLogFiles;
    
    /**
     * Indicates if incident is ready for processing
     * (has both application name and log files)
     */
    private boolean readyForProcessing;
    
    // Additional metadata
    /**
     * Original source object for reference
     * - For Email: com.microsoft.graph.models.Message
     * - For Jira: com.atlassian.jira.rest.client.api.domain.Issue
     */
    private transient Object sourceObject;
    
    /**
     * Enum for incident source type
     */
    public enum IncidentSource {
        EMAIL,
        JIRA
    }
    
    /**
     * Check if incident is from email source
     * 
     * @return true if source is EMAIL
     */
    public boolean isFromEmail() {
        return source == IncidentSource.EMAIL;
    }
    
    /**
     * Check if incident is from Jira source
     * 
     * @return true if source is JIRA
     */
    public boolean isFromJira() {
        return source == IncidentSource.JIRA;
    }
    
    /**
     * Check if incident needs application name
     * 
     * @return true if application name is missing
     */
    public boolean needsApplicationName() {
        return !hasApplicationName;
    }
    
    /**
     * Check if incident needs log files
     * 
     * @return true if log files are missing
     */
    public boolean needsLogFiles() {
        return !hasLogFiles;
    }
    
    /**
     * Update processing readiness status
     * Sets readyForProcessing based on hasApplicationName and hasLogFiles
     */
    public void updateReadinessStatus() {
        this.readyForProcessing = hasApplicationName && hasLogFiles;
    }
    
    /**
     * Set application name and update status
     * 
     * @param applicationName The application name
     */
    public void setApplicationNameAndUpdateStatus(String applicationName) {
        this.applicationName = applicationName;
        this.hasApplicationName = (applicationName != null && !applicationName.trim().isEmpty());
        updateReadinessStatus();
    }
    
    /**
     * Set log files and update status
     * 
     * @param logFiles Map of log files
     */
    public void setLogFilesAndUpdateStatus(Map<String, byte[]> logFiles) {
        this.logFiles = logFiles;
        this.hasLogFiles = (logFiles != null && !logFiles.isEmpty());
        updateReadinessStatus();
    }
}

// Made with Bob