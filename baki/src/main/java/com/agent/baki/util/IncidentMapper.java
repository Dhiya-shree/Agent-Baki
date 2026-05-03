package com.agent.baki.util;

import com.agent.baki.dto.IncidentDTO;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.microsoft.graph.models.Message;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for mapping source objects to IncidentDTO
 * 
 * Provides static methods to convert:
 * - Outlook Message → IncidentDTO
 * - Jira Issue → IncidentDTO
 * 
 * @author Team Baki
 */
@Slf4j
public class IncidentMapper {
    
    // Private constructor to prevent instantiation
    private IncidentMapper() {
        throw new IllegalStateException("Utility class");
    }
    
    /**
     * Convert Outlook Message to IncidentDTO
     * 
     * Maps Message fields to IncidentDTO:
     * - sourceId: message.id
     * - title: message.subject
     * - description: message.body.content (plain text or HTML)
     * - reporterName: message.from.emailAddress.name
     * - reporterEmail: message.from.emailAddress.address
     * - createdDate: message.receivedDateTime (converted to LocalDateTime)
     * - hasAttachments: message.hasAttachments
     * - source: IncidentSource.EMAIL
     * - sourceObject: original Message object
     * 
     * Note: logFiles will be empty initially and populated later by OutlookService
     * 
     * @param message The Outlook Message object
     * @return IncidentDTO with email data, or null if message is null
     */
    public static IncidentDTO fromMessage(Message message) {
        if (message == null) {
            log.warn("Cannot map null Message to IncidentDTO");
            return null;
        }
        
        try {
            // Extract sender information
            String reporterName = "Unknown";
            String reporterEmail = "";
            if (message.from != null && message.from.emailAddress != null) {
                if (message.from.emailAddress.name != null) {
                    reporterName = message.from.emailAddress.name;
                }
                if (message.from.emailAddress.address != null) {
                    reporterEmail = message.from.emailAddress.address;
                }
            }
            
            // Extract body content
            String description = "";
            if (message.body != null && message.body.content != null) {
                description = message.body.content;
            }
            
            // Convert received date to LocalDateTime
            LocalDateTime createdDate = null;
            if (message.receivedDateTime != null) {
                createdDate = message.receivedDateTime
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
            }
            
            // Check if has attachments
            boolean hasAttachments = message.hasAttachments != null && message.hasAttachments;
            
            // Build IncidentDTO
            IncidentDTO incident = IncidentDTO.builder()
                    .source(IncidentDTO.IncidentSource.EMAIL)
                    .sourceId(message.id)
                    .title(message.subject != null ? message.subject : "")
                    .description(description)
                    .reporterName(reporterName)
                    .reporterEmail(reporterEmail)
                    .createdDate(createdDate)
                    .hasAttachments(hasAttachments)
                    .logFiles(new HashMap<>())
                    .hasApplicationName(false)
                    .hasLogFiles(false)
                    .readyForProcessing(false)
                    .sourceObject(message)
                    .build();
            
            log.debug("Mapped Message {} to IncidentDTO", message.id);
            return incident;
            
        } catch (Exception e) {
            log.error("Error mapping Message to IncidentDTO: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Convert Jira Issue to IncidentDTO
     * 
     * Maps Issue fields to IncidentDTO:
     * - sourceId: issue.key (e.g., "PROJ-123")
     * - title: issue.summary
     * - description: issue.description
     * - reporterName: issue.reporter.displayName
     * - reporterEmail: issue.reporter.emailAddress (if available)
     * - createdDate: issue.creationDate (converted to LocalDateTime)
     * - hasAttachments: check if issue.attachments is not empty
     * - source: IncidentSource.JIRA
     * - sourceObject: original Issue object
     * 
     * Note: logFiles will be empty initially and populated later if needed
     * 
     * @param issue The Jira Issue object
     * @return IncidentDTO with Jira data, or null if issue is null
     */
    public static IncidentDTO fromIssue(Issue issue) {
        if (issue == null) {
            log.warn("Cannot map null Issue to IncidentDTO");
            return null;
        }
        
        try {
            // Extract reporter information
            String reporterName = "Unknown";
            String reporterEmail = "";
            if (issue.getReporter() != null) {
                if (issue.getReporter().getDisplayName() != null) {
                    reporterName = issue.getReporter().getDisplayName();
                }
                if (issue.getReporter().getEmailAddress() != null) {
                    reporterEmail = issue.getReporter().getEmailAddress();
                }
            }
            
            // Extract description
            String description = "";
            if (issue.getDescription() != null) {
                description = issue.getDescription();
            }
            
            // Convert creation date to LocalDateTime
            LocalDateTime createdDate = null;
            if (issue.getCreationDate() != null) {
                createdDate = LocalDateTime.now();
            }
            
            // Check if has attachments
            boolean hasAttachments = false;
            if (issue.getAttachments() != null) {
                hasAttachments = issue.getAttachments().iterator().hasNext();
            }
            
            // Build IncidentDTO
            IncidentDTO incident = IncidentDTO.builder()
                    .source(IncidentDTO.IncidentSource.JIRA)
                    .sourceId(issue.getKey())
                    .title(issue.getSummary() != null ? issue.getSummary() : "")
                    .description(description)
                    .reporterName(reporterName)
                    .reporterEmail(reporterEmail)
                    .createdDate(createdDate)
                    .hasAttachments(hasAttachments)
                    .logFiles(new HashMap<>())
                    .hasApplicationName(false)
                    .hasLogFiles(false)
                    .readyForProcessing(false)
                    .sourceObject(issue)
                    .build();
            
            log.debug("Mapped Issue {} to IncidentDTO", issue.getKey());
            return incident;
            
        } catch (Exception e) {
            log.error("Error mapping Issue to IncidentDTO: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Batch convert multiple Messages to IncidentDTOs
     * 
     * Iterates through messages and converts each to IncidentDTO
     * Skips null messages and continues processing
     * 
     * @param messages Iterable of Message objects
     * @return Map of sourceId to IncidentDTO
     */
    public static Map<String, IncidentDTO> fromMessages(Iterable<Message> messages) {
        Map<String, IncidentDTO> incidents = new HashMap<>();
        
        if (messages == null) {
            log.warn("Cannot map null messages collection");
            return incidents;
        }
        
        int count = 0;
        for (Message message : messages) {
            IncidentDTO incident = fromMessage(message);
            if (incident != null) {
                incidents.put(incident.getSourceId(), incident);
                count++;
            }
        }
        
        log.info("Mapped {} Messages to IncidentDTOs", count);
        return incidents;
    }
    
    /**
     * Batch convert multiple Issues to IncidentDTOs
     * 
     * Iterates through issues and converts each to IncidentDTO
     * Skips null issues and continues processing
     * 
     * @param issues Iterable of Issue objects
     * @return Map of sourceId to IncidentDTO
     */
    public static Map<String, IncidentDTO> fromIssues(Iterable<Issue> issues) {
        Map<String, IncidentDTO> incidents = new HashMap<>();
        
        if (issues == null) {
            log.warn("Cannot map null issues collection");
            return incidents;
        }
        
        int count = 0;
        for (Issue issue : issues) {
            IncidentDTO incident = fromIssue(issue);
            if (incident != null) {
                incidents.put(incident.getSourceId(), incident);
                count++;
            }
        }
        
        log.info("Mapped {} Issues to IncidentDTOs", count);
        return incidents;
    }
    
    /**
     * Extract application name from incident title or description
     * 
     * Searches for application name patterns in:
     * 1. Title/subject
     * 2. Description/body
     * 
     * Common patterns:
     * - "Application: AppName"
     * - "App: AppName"
     * - "[AppName]"
     * - "AppName -"
     * 
     * @param incident The IncidentDTO
     * @return Extracted application name, or null if not found
     */
    public static String extractApplicationName(IncidentDTO incident) {
        if (incident == null) {
            return null;
        }
        
        String title = incident.getTitle() != null ? incident.getTitle() : "";
        String description = incident.getDescription() != null ? incident.getDescription() : "";
        
        // Pattern 1: "Application: AppName" or "App: AppName"
        String pattern1 = extractPattern(title, "(?i)(?:application|app):\\s*([\\w-]+)");
        if (pattern1 != null) {
            return pattern1;
        }
        
        pattern1 = extractPattern(description, "(?i)(?:application|app):\\s*([\\w-]+)");
        if (pattern1 != null) {
            return pattern1;
        }
        
        // Pattern 2: "[AppName]"
        String pattern2 = extractPattern(title, "\\[([\\w-]+)\\]");
        if (pattern2 != null) {
            return pattern2;
        }
        
        log.debug("Could not extract application name from incident {}", incident.getSourceId());
        return null;
    }
    
    /**
     * Extract text matching regex pattern
     * 
     * Helper method for pattern matching
     * 
     * @param text The text to search
     * @param regex The regex pattern
     * @return Matched text (first group), or null if not found
     */
    private static String extractPattern(String text, String regex) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
            java.util.regex.Matcher matcher = pattern.matcher(text);
            
            if (matcher.find() && matcher.groupCount() >= 1) {
                return matcher.group(1).trim();
            }
        } catch (Exception e) {
            log.warn("Error extracting pattern: {}", e.getMessage());
        }
        
        return null;
    }
}

// Made with Bob