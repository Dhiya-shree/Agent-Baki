package com.agent.baki.service.batch;

import com.agent.baki.dto.IncidentDTO;
import com.agent.baki.dto.IncidentDTO.IncidentSource;
import com.agent.baki.entity.Fix;
import com.agent.baki.entity.IssueStatus;
import com.agent.baki.entity.Mail;
import com.agent.baki.repository.MailRepository;
import com.agent.baki.service.integration.OutlookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Batch service for sending automated email replies
 *
 * Processes unreplied email incidents and sends fix details
 *
 * @author Team Baki
 */
@Service
@ConditionalOnProperty(name = "outlook.config.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class BatchEmailReplyService {
    
    private final MailRepository mailRepository;
    private final OutlookService outlookService;
    
    /**
     * Process all unreplied emails with assigned fixes
     *
     * Finds all mails with replied='N' and fix assigned, then sends automated replies
     *
     * @return List of IncidentDTO for successfully replied emails
     */
    @Transactional
    public List<IncidentDTO> processUnrepliedEmails() {
        log.info("Starting batch email reply processing");
        
        List<IncidentDTO> repliedIncidents = new ArrayList<>();
        
        try {
            // Find all unreplied mails with fix assigned
            List<Mail> unrepliedMails = mailRepository.findByRepliedAndFixIsNotNull('N');
            
            if (unrepliedMails.isEmpty()) {
                log.info("No unreplied emails found");
                return repliedIncidents;
            }
            
            log.info("Found {} unreplied emails to process", unrepliedMails.size());
            
            int failureCount = 0;
            
            for (Mail mail : unrepliedMails) {
                try {
                    // Build reply message
                    String replyMessage = buildReplyMessage(mail);
                    
                    // Send reply email
                    boolean sent = sendReplyEmail(mail, replyMessage);
                    
                    if (sent) {
                        // Mark as replied
                        mail.markAsReplied();
                        mailRepository.save(mail);
                        
                        // Build IncidentDTO and add to replied list
                        IncidentDTO incidentDTO = buildIncidentDTOFromMail(mail);
                        repliedIncidents.add(incidentDTO);
                        
                        log.info("Successfully replied to email: mailId={}, messageId={}",
                                mail.getMailId(), mail.getEmailMessageId());
                    } else {
                        failureCount++;
                        log.warn("Failed to send reply to email: mailId={}", mail.getMailId());
                    }
                    
                } catch (Exception e) {
                    failureCount++;
                    log.error("Error processing email mailId={}: {}",
                            mail.getMailId(), e.getMessage(), e);
                }
            }
            
            log.info("Batch email reply processing completed: success={}, failure={}",
                    repliedIncidents.size(), failureCount);
            
            return repliedIncidents;
            
        } catch (Exception e) {
            log.error("Error in batch email reply processing: {}", e.getMessage(), e);
            return repliedIncidents;
        }
    }
    
    /**
     * Build reply message with fix details
     * 
     * Creates formatted message based on fix status and details
     * 
     * @param mail The mail entity with fix
     * @return Formatted reply message
     */
    public String buildReplyMessage(Mail mail) {
        if (mail == null || mail.getFix() == null) {
            return "Unable to process your incident. Please contact support.";
        }
        
        Fix fix = mail.getFix();
        StringBuilder message = new StringBuilder();
        
        // Header
        message.append("Hello,\n\n");
        message.append("Thank you for reporting the incident. ");
        message.append("Our automated system has analyzed the issue.\n\n");
        
        // Code location
        message.append("Issue Location:\n");
        message.append("- Class: ").append(fix.getCodeClassName()).append("\n");
        message.append("- Line: ").append(fix.getCodeLine()).append("\n");
        
        if (fix.getIssueSummary() != null) {
            message.append("- Summary: ").append(fix.getIssueSummary()).append("\n");
        }
        message.append("\n");
        
        // Status-specific message
        IssueStatus status = fix.getIssueStatus();
        
        switch (status) {
            case RESOLVED:
                message.append("Status: RESOLVED ✓\n\n");
                message.append("The issue has been fixed and deployed to production.\n");
                if (fix.getGithubPr() != null) {
                    message.append("GitHub PR: ").append(fix.getGithubPr()).append("\n");
                }
                message.append("\nThe fix should now be live. Please verify and let us know if the issue persists.\n");
                break;
                
            case IN_PROGRESS:
                message.append("Status: IN PROGRESS ⚙\n\n");
                message.append("The development team is actively working on this issue.\n");
                if (fix.getGithubPr() != null) {
                    message.append("GitHub PR: ").append(fix.getGithubPr()).append("\n");
                }
                message.append("\nWe will notify you once the fix is deployed.\n");
                break;
                
            case DB_FIX:
                message.append("Status: DATABASE FIX APPLIED 🗄\n\n");
                message.append("This issue was resolved through a database change.\n");
                if (fix.getChangeNumber() != null) {
                    message.append("Change Number: ").append(fix.getChangeNumber()).append("\n");
                }
                if (fix.getReason() != null) {
                    message.append("Details: ").append(fix.getReason()).append("\n");
                }
                message.append("\nThe fix has been applied. Please verify the issue is resolved.\n");
                break;
                
            case IGNORED:
                message.append("Status: CLOSED ✗\n\n");
                message.append("This issue has been reviewed and closed.\n");
                if (fix.getReason() != null) {
                    message.append("Reason: ").append(fix.getReason()).append("\n");
                }
                message.append("\nIf you believe this is incorrect, please reply to this email.\n");
                break;
                
            case PENDING:
            default:
                message.append("Status: UNDER REVIEW 📋\n\n");
                message.append("The issue has been logged and is awaiting review by the development team.\n");
                message.append("We will update you once action is taken.\n");
                break;
        }
        
        // Footer
        message.append("\n---\n");
        message.append("This is an automated message from Agent Baki Incident Management System.\n");
        message.append("For questions, please reply to this email.\n");
        
        return message.toString();
    }
    
    /**
     * Send reply email via Outlook
     * 
     * Sends the reply message to the original email using Reply All
     * 
     * @param mail The mail entity
     * @param replyMessage The reply message content
     * @return true if sent successfully
     */
    public boolean sendReplyEmail(Mail mail, String replyMessage) {
        if (mail == null || mail.getEmailMessageId() == null) {
            log.error("Cannot send reply: mail or message ID is null");
            return false;
        }
        
        try {
            log.debug("Sending reply to email: messageId={}", mail.getEmailMessageId());
            
            // Send reply with Reply All to include all stakeholders
            outlookService.sendReply(mail.getEmailMessageId(), replyMessage, true);
            
            log.info("Reply sent successfully to: {}", mail.getEmailFrom());
            return true;
            
        } catch (Exception e) {
            log.error("Failed to send reply email to {}: {}", 
                    mail.getEmailFrom(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Build IncidentDTO from Mail entity
     *
     * Converts Mail entity to IncidentDTO for file cleanup
     *
     * @param mail The mail entity
     * @return IncidentDTO with application name and source ID
     */
    private IncidentDTO buildIncidentDTOFromMail(Mail mail) {
        IncidentDTO dto = new IncidentDTO();
        
        if (mail.getApplication() != null) {
            dto.setApplicationName(mail.getApplication().getApplicationName());
        }
        
        dto.setSourceId(mail.getEmailMessageId());
        dto.setSource(IncidentSource.EMAIL);
        
        return dto;
    }
}

// Made with Bob