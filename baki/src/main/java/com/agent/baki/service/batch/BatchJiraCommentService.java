package com.agent.baki.service.batch;

import com.agent.baki.dto.IncidentDTO;
import com.agent.baki.dto.IncidentDTO.IncidentSource;
import com.agent.baki.entity.Fix;
import com.agent.baki.entity.IssueStatus;
import com.agent.baki.entity.Jira;
import com.agent.baki.repository.JiraRepository;
import com.agent.baki.service.integration.JiraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Batch service for adding automated Jira comments
 *
 * Processes unreplied Jira incidents and adds fix details as comments
 *
 * @author Team Baki
 */
@Service
@ConditionalOnProperty(name = "jira.config.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class BatchJiraCommentService {
    
    private final JiraRepository jiraRepository;
    private final JiraService jiraService;
    
    /**
     * Process all unreplied Jira tickets with assigned fixes
     *
     * Finds all jiras with replied='N' and fix assigned, then adds automated comments
     *
     * @return List of IncidentDTO for successfully commented Jira tickets
     */
    @Transactional
    public List<IncidentDTO> processUnrepliedJiras() {
        log.info("Starting batch Jira comment processing");
        
        List<IncidentDTO> repliedIncidents = new ArrayList<>();
        
        try {
            // Find all unreplied jiras with fix assigned
            List<Jira> unrepliedJiras = jiraRepository.findByRepliedAndFixIsNotNull('N');
            
            if (unrepliedJiras.isEmpty()) {
                log.info("No unreplied Jira tickets found");
                return repliedIncidents;
            }
            
            log.info("Found {} unreplied Jira tickets to process", unrepliedJiras.size());
            
            int failureCount = 0;
            
            for (Jira jira : unrepliedJiras) {
                try {
                    // Build comment message
                    String commentMessage = buildCommentMessage(jira);
                    
                    // Add comment to Jira ticket
                    boolean added = addJiraComment(jira, commentMessage);
                    
                    if (added) {
                        // Mark as replied
                        jira.markAsReplied();
                        jiraRepository.save(jira);
                        
                        // Build IncidentDTO and add to replied list
                        IncidentDTO incidentDTO = buildIncidentDTOFromJira(jira);
                        repliedIncidents.add(incidentDTO);
                        
                        log.info("Successfully added comment to Jira: jiraId={}, ticketKey={}",
                                jira.getJiraId(), jira.getJiraTicketKey());
                    } else {
                        failureCount++;
                        log.warn("Failed to add comment to Jira: jiraId={}", jira.getJiraId());
                    }
                    
                } catch (Exception e) {
                    failureCount++;
                    log.error("Error processing Jira jiraId={}: {}",
                            jira.getJiraId(), e.getMessage(), e);
                }
            }
            
            log.info("Batch Jira comment processing completed: success={}, failure={}",
                    repliedIncidents.size(), failureCount);
            
            return repliedIncidents;
            
        } catch (Exception e) {
            log.error("Error in batch Jira comment processing: {}", e.getMessage(), e);
            return repliedIncidents;
        }
    }
    
    /**
     * Build comment message with fix details
     * 
     * Creates formatted comment based on fix status and details
     * 
     * @param jira The jira entity with fix
     * @return Formatted comment message
     */
    public String buildCommentMessage(Jira jira) {
        if (jira == null || jira.getFix() == null) {
            return "Unable to process this incident. Please contact support.";
        }
        
        Fix fix = jira.getFix();
        StringBuilder comment = new StringBuilder();
        
        // Header
        comment.append("*Automated Update from Agent Baki*\n\n");
        comment.append("Our automated system has analyzed this incident.\n\n");
        
        // Code location
        comment.append("h4. Issue Location\n");
        comment.append("* *Class:* {{").append(fix.getCodeClassName()).append("}}\n");
        comment.append("* *Line:* {{").append(fix.getCodeLine()).append("}}\n");
        
        if (fix.getIssueSummary() != null) {
            comment.append("* *Summary:* ").append(fix.getIssueSummary()).append("\n");
        }
        comment.append("\n");
        
        // Status-specific message
        IssueStatus status = fix.getIssueStatus();
        
        switch (status) {
            case RESOLVED:
                comment.append("h4. Status: {color:green}RESOLVED{color} (/)\\n\\n");
                comment.append("The issue has been fixed and deployed to production.\\n");
                if (fix.getGithubPr() != null) {
                    comment.append("*GitHub PR:* ").append(fix.getGithubPr()).append("\\n");
                }
                comment.append("\\nThe fix should now be live. Please verify and update this ticket if the issue persists.\\n");
                break;
                
            case IN_PROGRESS:
                comment.append("h4. Status: {color:blue}IN PROGRESS{color} (!)\\n\\n");
                comment.append("The development team is actively working on this issue.\\n");
                if (fix.getGithubPr() != null) {
                    comment.append("*GitHub PR:* ").append(fix.getGithubPr()).append("\\n");
                }
                comment.append("\\nWe will update this ticket once the fix is deployed.\\n");
                break;
                
            case DB_FIX:
                comment.append("h4. Status: {color:purple}DATABASE FIX APPLIED{color} (/)\\n\\n");
                comment.append("This issue was resolved through a database change.\\n");
                if (fix.getChangeNumber() != null) {
                    comment.append("*Change Number:* ").append(fix.getChangeNumber()).append("\\n");
                }
                if (fix.getReason() != null) {
                    comment.append("*Details:* ").append(fix.getReason()).append("\\n");
                }
                comment.append("\\nThe fix has been applied. Please verify the issue is resolved.\\n");
                break;
                
            case IGNORED:
                comment.append("h4. Status: {color:red}CLOSED{color} (x)\\n\\n");
                comment.append("This issue has been reviewed and closed.\\n");
                if (fix.getReason() != null) {
                    comment.append("*Reason:* ").append(fix.getReason()).append("\\n");
                }
                comment.append("\\nIf you believe this is incorrect, please reopen this ticket with additional details.\\n");
                break;
                
            case PENDING:
            default:
                comment.append("h4. Status: {color:orange}UNDER REVIEW{color} (?)\\n\\n");
                comment.append("The issue has been logged and is awaiting review by the development team.\\n");
                comment.append("We will update this ticket once action is taken.\\n");
                break;
        }
        
        // Footer
        comment.append("\\n----\\n");
        comment.append("_This is an automated comment from Agent Baki Incident Management System._\\n");
        
        return comment.toString();
    }
    
    /**
     * Add comment to Jira ticket
     * 
     * Adds the comment message to the Jira ticket using Jira REST API
     * 
     * @param jira The jira entity
     * @param commentMessage The comment message content
     * @return true if added successfully
     */
    public boolean addJiraComment(Jira jira, String commentMessage) {
        if (jira == null || jira.getJiraTicketKey() == null) {
            log.error("Cannot add comment: jira or ticket key is null");
            return false;
        }
        
        try {
            log.debug("Adding comment to Jira ticket: ticketKey={}", jira.getJiraTicketKey());
            
            // Add comment using JiraService
            jiraService.addComment(jira.getJiraTicketKey(), commentMessage);
            
            log.info("Comment added successfully to Jira ticket: {}", jira.getJiraTicketKey());
            return true;
            
        } catch (Exception e) {
            log.error("Failed to add comment to Jira ticket {}: {}", 
                    jira.getJiraTicketKey(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Add fix details comment to Jira ticket
     * 
     * Specialized method for adding detailed fix information
     * Used when fix status changes to RESOLVED, IN_PROGRESS, or DB_FIX
     * 
     * @param jira The jira entity
     * @return true if added successfully
     */
    public boolean addFixDetailsComment(Jira jira) {
        if (jira == null || jira.getFix() == null) {
            log.error("Cannot add fix details: jira or fix is null");
            return false;
        }
        
        try {
            Fix fix = jira.getFix();
            
            log.debug("Adding fix details comment to Jira: ticketKey={}, fixId={}", 
                    jira.getJiraTicketKey(), fix.getFixId());
            
            // Use JiraService's addFixDetailsComment method
            jiraService.addFixDetailsComment(
                    jira.getJiraTicketKey(), 
                    fix.getFixId(),
                    fix.getCodeClassName(),
                    fix.getCodeLine(),
                    fix.getGithubPr(),
                    fix.getIssueStatus().name()
            );
            
            log.info("Fix details comment added to Jira ticket: {}", jira.getJiraTicketKey());
            return true;
            
        } catch (Exception e) {
            log.error("Failed to add fix details to Jira ticket {}: {}", 
                    jira.getJiraTicketKey(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Build IncidentDTO from Jira entity
     *
     * Converts Jira entity to IncidentDTO for file cleanup
     *
     * @param jira The jira entity
     * @return IncidentDTO with application name and source ID
     */
    private IncidentDTO buildIncidentDTOFromJira(Jira jira) {
        IncidentDTO dto = new IncidentDTO();
        
        if (jira.getApplication() != null) {
            dto.setApplicationName(jira.getApplication().getApplicationName());
        }
        
        dto.setSourceId(jira.getJiraTicketKey());
        dto.setSource(IncidentSource.JIRA);
        
        return dto;
    }
}

// Made with Bob