package com.agent.baki.service.reply;

import com.agent.baki.dto.IncidentDTO;
import com.agent.baki.service.integration.OutlookService;
import com.agent.baki.service.integration.JiraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for automated replies to incidents
 *
 * Handles:
 * - Sending replies for missing application name
 * - Sending replies for missing log files
 * - Determining what data is missing
 *
 * @author Team Baki
 */
@Service
@Slf4j
public class AutomatedReplyService {
    
    @Autowired(required = false)
    private OutlookService outlookService;
    
    @Autowired(required = false)
    private JiraService jiraService;
    
    /**
     * Send automated reply for missing data
     * 
     * Determines what is missing and sends appropriate reply
     * - If application name missing: request application name
     * - If log files missing: request log files
     * - If both missing: request both
     * 
     * @param incident The incident with missing data
     * @return true if reply sent successfully, false otherwise
     */
    public boolean sendMissingDataReply(IncidentDTO incident) {
        if (incident == null) {
            log.error("Cannot send reply: incident is null");
            return false;
        }
        
        if (incident.isReadyForProcessing()) {
            log.info("Incident {} is ready, no reply needed", incident.getSourceId());
            return false;
        }
        
        log.info("Sending missing data reply for incident {}", incident.getSourceId());
        
        boolean appNameMissing = incident.needsApplicationName();
        boolean logsMissing = incident.needsLogFiles();
        
        // Determine which reply to send
        if (appNameMissing && logsMissing) {
            return sendMissingBothReply(incident);
        } else if (appNameMissing) {
            return sendMissingApplicationNameReply(incident);
        } else if (logsMissing) {
            return sendMissingLogsReply(incident);
        }
        
        return false;
    }
    
    /**
     * Send reply requesting missing application name
     * 
     * Routes to appropriate service based on source type
     * 
     * @param incident The incident
     * @return true if reply sent successfully, false otherwise
     */
    public boolean sendMissingApplicationNameReply(IncidentDTO incident) {
        if (incident == null) {
            return false;
        }
        
        log.info("Sending missing application name reply for incident {}", incident.getSourceId());
        
        if (incident.isFromEmail()) {
            if (outlookService != null) {
                return outlookService.sendMissingApplicationNameReply(
                        incident.getSourceId(),
                        incident.getReporterName()
                );
            } else {
                log.warn("OutlookService is not available (disabled). Cannot send email reply for incident {}", incident.getSourceId());
                return false;
            }
        } else if (incident.isFromJira()) {
            if (jiraService != null) {
                String comment = buildMissingApplicationNameComment();
                return jiraService.addComment(incident.getSourceId(), comment);
            } else {
                log.warn("JiraService is not available (disabled). Cannot add Jira comment for incident {}", incident.getSourceId());
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Send reply requesting missing log files
     * 
     * Routes to appropriate service based on source type
     * 
     * @param incident The incident
     * @return true if reply sent successfully, false otherwise
     */
    public boolean sendMissingLogsReply(IncidentDTO incident) {
        if (incident == null) {
            return false;
        }
        
        log.info("Sending missing logs reply for incident {}", incident.getSourceId());
        
        if (incident.isFromEmail()) {
            if (outlookService != null) {
                return outlookService.sendMissingLogsReply(
                        incident.getSourceId(),
                        incident.getReporterName()
                );
            } else {
                log.warn("OutlookService is not available (disabled). Cannot send email reply for incident {}", incident.getSourceId());
                return false;
            }
        } else if (incident.isFromJira()) {
            if (jiraService != null) {
                String comment = buildMissingLogsComment();
                return jiraService.addComment(incident.getSourceId(), comment);
            } else {
                log.warn("JiraService is not available (disabled). Cannot add Jira comment for incident {}", incident.getSourceId());
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Send reply requesting both application name and log files
     * 
     * Routes to appropriate service based on source type
     * 
     * @param incident The incident
     * @return true if reply sent successfully, false otherwise
     */
    public boolean sendMissingBothReply(IncidentDTO incident) {
        if (incident == null) {
            return false;
        }
        
        log.info("Sending missing both (app name and logs) reply for incident {}",
                incident.getSourceId());
        
        if (incident.isFromEmail()) {
            if (outlookService != null) {
                // For email, send combined message
                String htmlContent = buildMissingBothEmailContent(incident.getReporterName());
                return outlookService.sendReply(incident.getSourceId(), htmlContent, false);
            } else {
                log.warn("OutlookService is not available (disabled). Cannot send email reply for incident {}", incident.getSourceId());
                return false;
            }
            
        } else if (incident.isFromJira()) {
            if (jiraService != null) {
                String comment = buildMissingBothComment();
                return jiraService.addComment(incident.getSourceId(), comment);
            } else {
                log.warn("JiraService is not available (disabled). Cannot add Jira comment for incident {}", incident.getSourceId());
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Build HTML content for missing both (app name and logs) email
     * 
     * @param senderName The sender name
     * @return HTML formatted content
     */
    private String buildMissingBothEmailContent(String senderName) {
        return String.format(
            "<html>" +
            "<body style='font-family: Arial, sans-serif;'>" +
            "<p>Dear %s,</p>" +
            "<p>Thank you for reporting this incident. To process your request, we need the following information:</p>" +
            "<ul>" +
            "<li><strong>Application Name</strong></li>" +
            "<li><strong>Log Files</strong> (.log, .txt, .xls, or .xlsx format)</li>" +
            "</ul>" +
            "<p>Please reply to this email with:</p>" +
            "<ol>" +
            "<li>The application name in the subject or body</li>" +
            "<li>The log files attached</li>" +
            "</ol>" +
            "<p>Note: Please ensure the log files contain the error stack trace or relevant error messages.</p>" +
            "<br>" +
            "<p>Best regards,<br>" +
            "<strong>Agent Baki</strong><br>" +
            "Automated Incident Management System</p>" +
            "</body>" +
            "</html>",
            senderName
        );
    }
    
    /**
     * Build comment text for missing application name (Jira)
     * 
     * @return Comment text
     */
    private String buildMissingApplicationNameComment() {
        return "Missing Information Required\n\n" +
               "To process this incident, please provide:\n" +
               "- Application Name\n\n" +
               "Please update the issue with the application name.\n\n" +
               "- Agent Baki (Automated Incident Management System)";
    }
    
    /**
     * Build comment text for missing log files (Jira)
     * 
     * @return Comment text
     */
    private String buildMissingLogsComment() {
        return "Missing Information Required\n\n" +
               "To analyze this incident, please provide:\n" +
               "- Log Files (.log, .txt, .xls, or .xlsx format)\n\n" +
               "Please attach the log files to this issue.\n" +
               "Ensure the log files contain the error stack trace or relevant error messages.\n\n" +
               "- Agent Baki (Automated Incident Management System)";
    }
    
    /**
     * Build comment text for missing both (Jira)
     * 
     * @return Comment text
     */
    private String buildMissingBothComment() {
        return "Missing Information Required\n\n" +
               "To process this incident, please provide:\n" +
               "- Application Name\n" +
               "- Log Files (.log, .txt, .xls, or .xlsx format)\n\n" +
               "Please update the issue with the application name and attach the log files.\n" +
               "Ensure the log files contain the error stack trace or relevant error messages.\n\n" +
               "- Agent Baki (Automated Incident Management System)";
    }
    
    /**
     * Check if incident needs a reply
     * 
     * @param incident The incident to check
     * @return true if reply is needed, false otherwise
     */
    public boolean needsReply(IncidentDTO incident) {
        if (incident == null) {
            return false;
        }
        
        return !incident.isReadyForProcessing();
    }
}

// Made with Bob