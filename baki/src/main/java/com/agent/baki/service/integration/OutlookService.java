package com.agent.baki.service.integration;

import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.MessageCreateReplyAllParameterSet;
import com.microsoft.graph.models.MessageCreateReplyParameterSet;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Service for Microsoft Outlook integration using Microsoft Graph API
 *
 * This service handles all interactions with Outlook emails:
 * - Fetching incident-related emails
 * - Sending reply emails
 * - Managing email attachments
 *
 * Uses GraphServiceClient configured in OutlookConfig
 *
 * @author Team Baki
 */
@Service
@ConditionalOnProperty(name = "outlook.config.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class OutlookService {
    
    private final GraphServiceClient<?> graphServiceClient;
    
    /**
     * Fetch unread emails from Outlook inbox with incident-related subjects
     *
     * Filters emails by:
     * - Unread status (isRead eq false)
     * - Subject contains: "Issue" OR "Ticket" OR "Incident" (case-insensitive)
     *
     * Returns top 50 emails ordered by received date (newest first)
     *
     * @return List of Message objects containing unread incident-related emails
     */
    public List<Message> fetchIncidentEmails() {
        // BobAndMe TODO: Fetch unread emails from Outlook inbox with incident-related subjects
        try {
            log.info("Fetching unread incident-related emails from Outlook");
            
            // Build filter for unread emails with incident-related subjects
            // isRead eq false - Only unread emails
            // AND (subject contains Issue OR Ticket OR Incident)
            String filter = "isRead eq false and " +
                          "(contains(subject, 'Issue') or contains(subject, 'Ticket') or contains(subject, 'Incident'))";
            
            // Fetch emails with filter, select specific fields, order by date, limit to 50
            List<Message> messages = graphServiceClient.me()
                    .messages()
                    .buildRequest()
                    .filter(filter)
                    .select("id,subject,from,receivedDateTime,body,hasAttachments,isRead")
                    .orderBy("receivedDateTime DESC")
                    .top(50)
                    .get()
                    .getCurrentPage();
            
            log.info("Successfully fetched {} unread incident emails", messages.size());
            return messages;
            
        } catch (Exception e) {
            log.error("Error fetching incident emails from Outlook: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Fetch a specific email by its ID
     *
     * Retrieves complete email details including body content
     *
     * @param messageId The unique ID of the email message
     * @return Message object or null if not found
     */
    public Message getEmailById(String messageId) {
        // BobAndMe TODO: Fetch a specific email by its ID
        try {
            log.info("Fetching email by ID: {}", messageId);
            
            // Fetch the message by ID with all fields
            Message message = graphServiceClient.me()
                    .messages(messageId)
                    .buildRequest()
                    .get();
            
            log.info("Successfully fetched email: {}", messageId);
            return message;
            
        } catch (Exception e) {
            log.error("Error fetching email by ID {}: {}", messageId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get email attachments for a specific message
     *
     * Downloads only log and text files (.log, .txt, .xls, .xlsx)
     * Validates file extensions to prevent corruption
     * Returns map of filename to content bytes
     *
     * @param messageId The unique ID of the email message
     * @return Map of attachment filename to content bytes (only valid file types)
     */
    public Map<String, byte[]> getEmailAttachments(String messageId) {
        // BobAndMe TODO: Get email attachments for a specific message
        Map<String, byte[]> attachments = new HashMap<>();
        
        try {
            log.info("Fetching attachments for email: {}", messageId);
            
            // Get all attachments for the message
            var attachmentPage = graphServiceClient.me()
                    .messages(messageId)
                    .attachments()
                    .buildRequest()
                    .get();
            
            if (attachmentPage == null || attachmentPage.getCurrentPage() == null) {
                log.info("No attachments found for email: {}", messageId);
                return attachments;
            }
            
            // Process each attachment
            for (var attachment : attachmentPage.getCurrentPage()) {
                // Only process file attachments (not inline/embedded images)
                if (attachment.oDataType != null &&
                    attachment.oDataType.equals("#microsoft.graph.fileAttachment")) {
                    
                    String fileName = attachment.name;
                    
                    // Validate file extension - only accept log, txt, xls, xlsx files
                    if (!isValidLogFile(fileName)) {
                        log.warn("Skipping invalid file type: {}", fileName);
                        continue;
                    }
                    
                    // Ensure file has extension
                    fileName = ensureFileExtension(fileName);
                    
                    // Get attachment content as byte array
                    var fileAttachment = graphServiceClient.me()
                            .messages(messageId)
                            .attachments(attachment.id)
                            .buildRequest()
                            .get();
                    
                    // Extract content bytes
                    if (fileAttachment instanceof com.microsoft.graph.models.FileAttachment) {
                        byte[] content = ((com.microsoft.graph.models.FileAttachment) fileAttachment).contentBytes;
                        
                        if (content != null && content.length > 0) {
                            attachments.put(fileName, content);
                            log.info("Downloaded attachment: {} ({} bytes)", fileName, content.length);
                        } else {
                            log.warn("Attachment {} has no content", fileName);
                        }
                    }
                }
            }
            
            log.info("Successfully fetched {} valid attachments for email: {}", attachments.size(), messageId);
            
        } catch (Exception e) {
            log.error("Error fetching attachments for email {}: {}", messageId, e.getMessage(), e);
        }
        
        return attachments;
    }
    
    /**
     * Validate if file is a valid log file type
     *
     * Accepts: .log, .txt, .xls, .xlsx
     *
     * @param fileName The file name to validate
     * @return true if valid log file type, false otherwise
     */
    private boolean isValidLogFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }
        
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".log") ||
               lowerFileName.endsWith(".txt") ||
               lowerFileName.endsWith(".xls") ||
               lowerFileName.endsWith(".xlsx");
    }
    
    /**
     * Ensure file name has proper extension
     *
     * If file name doesn't have extension, adds .txt as default
     *
     * @param fileName The original file name
     * @return File name with extension
     */
    private String ensureFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "attachment.txt";
        }
        
        // Check if file already has extension
        if (fileName.contains(".")) {
            return fileName;
        }
        
        // Add .txt extension as default
        return fileName + ".txt";
    }
    
    /**
     * Send a reply email to the original sender (with Reply All option)
     *
     * Creates and sends a reply with HTML formatted content
     * Supports both "Reply" and "Reply All" actions
     *
     * @param messageId The unique ID of the original email message
     * @param replyContent The HTML content to send in the reply
     * @param replyAll If true, uses Reply All (includes CC recipients); if false, uses Reply (sender only)
     * @return true if reply sent successfully, false otherwise
     */
    public boolean sendReply(String messageId, String replyContent, boolean replyAll) {
        // BobAndMe TODO: Send a reply email to the original sender
        try {
            String replyType = replyAll ? "Reply All" : "Reply";
            log.info("Sending {} to email: {}", replyType, messageId);
            
            // Create a reply message (Reply or Reply All based on parameter)
            var reply = replyAll
                ? graphServiceClient.me()
                        .messages(messageId)
                        .createReplyAll(MessageCreateReplyAllParameterSet.newBuilder().withComment(replyContent).build())
                        .buildRequest()
                        .post()
                : graphServiceClient.me()
                        .messages(messageId)
                        .createReply(MessageCreateReplyParameterSet.newBuilder().withComment(replyContent).build())
                        .buildRequest()
                        .post();
            
            // Set the reply body content (HTML format)
            com.microsoft.graph.models.ItemBody body = new com.microsoft.graph.models.ItemBody();
            body.contentType = com.microsoft.graph.models.BodyType.HTML;
            body.content = replyContent;
            reply.body = body;
            
            // Update the reply with the body content
            graphServiceClient.me()
                    .messages(reply.id)
                    .buildRequest()
                    .patch(reply);
            
            // Send the reply
            graphServiceClient.me()
                    .messages(reply.id)
                    .send()
                    .buildRequest()
                    .post();
            
            log.info("Successfully sent {} to email: {}", replyType, messageId);
            return true;
            
        } catch (Exception e) {
            log.error("Error sending reply to email {}: {}", messageId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Send a reply requesting missing application name
     *
     * Constructs professional HTML template asking for application name
     * Uses Reply (not Reply All) for information requests
     *
     * @param messageId The unique ID of the original email message
     * @param senderName The name of the email sender
     * @return true if reply sent successfully, false otherwise
     */
    public boolean sendMissingApplicationNameReply(String messageId, String senderName) {
        // BobAndMe TODO: Send a reply requesting missing application name
        log.info("Sending missing application name request for email: {}", messageId);
        
        String htmlContent = String.format(
            "<html>" +
            "<body style='font-family: Arial, sans-serif;'>" +
            "<p>Dear %s,</p>" +
            "<p>Thank you for reporting this incident. To process your request, we need the following information:</p>" +
            "<ul>" +
            "<li><strong>Application Name</strong></li>" +
            "</ul>" +
            "<p>Please reply to this email with the application name.</p>" +
            "<br>" +
            "<p>Best regards,<br>" +
            "<strong>Agent Baki</strong><br>" +
            "Automated Incident Management System</p>" +
            "</body>" +
            "</html>",
            senderName
        );
        
        // Use Reply (not Reply All) for information requests
        return sendReply(messageId, htmlContent, false);
    }
    
    /**
     * Send a reply requesting missing log files
     *
     * Constructs professional HTML template asking for log files
     * Specifies acceptable file formats (.log, .txt, .xls, .xlsx)
     * Uses Reply (not Reply All) for information requests
     *
     * @param messageId The unique ID of the original email message
     * @param senderName The name of the email sender
     * @return true if reply sent successfully, false otherwise
     */
    public boolean sendMissingLogsReply(String messageId, String senderName) {
        // BobAndMe TODO: Send a reply requesting missing log files
        log.info("Sending missing logs request for email: {}", messageId);
        
        String htmlContent = String.format(
            "<html>" +
            "<body style='font-family: Arial, sans-serif;'>" +
            "<p>Dear %s,</p>" +
            "<p>Thank you for reporting this incident. To analyze the issue, we need the log files.</p>" +
            "<p><strong>Please reply to this email with the log files attached.</strong></p>" +
            "<p>Acceptable file formats:</p>" +
            "<ul>" +
            "<li>.log files</li>" +
            "<li>.txt files</li>" +
            "<li>.xls or .xlsx files</li>" +
            "</ul>" +
            "<p>Note: Please ensure the log files contain the error stack trace or relevant error messages.</p>" +
            "<br>" +
            "<p>Best regards,<br>" +
            "<strong>Agent Baki</strong><br>" +
            "Automated Incident Management System</p>" +
            "</body>" +
            "</html>",
            senderName
        );
        
        // Use Reply (not Reply All) for information requests
        return sendReply(messageId, htmlContent, false);
    }
    
    /**
     * Send a reply with fix details after issue resolution
     *
     * Constructs professional HTML template with complete fix information
     * Includes fix ID, code location, status, and change number (if applicable)
     * Uses Reply All to keep all stakeholders informed
     *
     * @param messageId The unique ID of the original email message
     * @param fixId The fix ID from database
     * @param className The problematic code class name
     * @param lineNumber The problematic code line number
     * @param status The fix status (Ignored, In Progress, DB Fix)
     * @param changeNumber The database change number (optional, can be null)
     * @return true if reply sent successfully, false otherwise
     */
    public boolean sendFixDetailsReply(String messageId, Long fixId, String className,
                                      Integer lineNumber, String status, String changeNumber) {
        // BobAndMe TODO: Send a reply with fix details after issue resolution
        log.info("Sending fix details reply for email: {} with fix ID: {}", messageId, fixId);
        
        // Build change number section if available
        String changeNumberSection = "";
        if (changeNumber != null && !changeNumber.trim().isEmpty()) {
            changeNumberSection = String.format(
                "<tr>" +
                "<td style='padding: 8px; border: 1px solid #ddd;'><strong>Change Number:</strong></td>" +
                "<td style='padding: 8px; border: 1px solid #ddd;'>%s</td>" +
                "</tr>",
                changeNumber
            );
        }
        
        String htmlContent = String.format(
            "<html>" +
            "<body style='font-family: Arial, sans-serif;'>" +
            "<p>Dear User,</p>" +
            "<p>Your incident has been analyzed and resolved. Below are the fix details:</p>" +
            "<table style='border-collapse: collapse; width: 100%%; max-width: 600px; margin: 20px 0;'>" +
            "<tr style='background-color: #f2f2f2;'>" +
            "<td style='padding: 8px; border: 1px solid #ddd;'><strong>Fix ID:</strong></td>" +
            "<td style='padding: 8px; border: 1px solid #ddd;'>%d</td>" +
            "</tr>" +
            "<tr>" +
            "<td style='padding: 8px; border: 1px solid #ddd;'><strong>Code Location:</strong></td>" +
            "<td style='padding: 8px; border: 1px solid #ddd;'>%s (Line %d)</td>" +
            "</tr>" +
            "<tr style='background-color: #f2f2f2;'>" +
            "<td style='padding: 8px; border: 1px solid #ddd;'><strong>Status:</strong></td>" +
            "<td style='padding: 8px; border: 1px solid #ddd;'>%s</td>" +
            "</tr>" +
            "%s" +
            "</table>" +
            "<p>The issue has been addressed. If you have any questions, please contact the development team.</p>" +
            "<br>" +
            "<p>Best regards,<br>" +
            "<strong>Agent Baki</strong><br>" +
            "Automated Incident Management System</p>" +
            "</body>" +
            "</html>",
            fixId, className, lineNumber, status, changeNumberSection
        );
        
        // Use Reply All to keep all stakeholders informed about resolution
        return sendReply(messageId, htmlContent, true);
    }
    
    /**
     * Extract sender email address from Message object
     *
     * @param message The email message
     * @return Sender email address or empty string if not found
     */
    public String extractSenderEmail(Message message) {
        // BobAndMe TODO: Extract sender email address from Message object
        try {
            if (message != null && message.from != null &&
                message.from.emailAddress != null &&
                message.from.emailAddress.address != null) {
                return message.from.emailAddress.address;
            }
        } catch (Exception e) {
            log.warn("Error extracting sender email: {}", e.getMessage());
        }
        return "";
    }
    
    /**
     * Extract sender name from Message object
     *
     * @param message The email message
     * @return Sender name or "User" as default if not found
     */
    public String extractSenderName(Message message) {
        // BobAndMe TODO: Extract sender name from Message object
        try {
            if (message != null && message.from != null &&
                message.from.emailAddress != null &&
                message.from.emailAddress.name != null &&
                !message.from.emailAddress.name.trim().isEmpty()) {
                return message.from.emailAddress.name;
            }
        } catch (Exception e) {
            log.warn("Error extracting sender name: {}", e.getMessage());
        }
        return "User";
    }
    
    /**
     * Check if email has attachments
     *
     * @param message The email message
     * @return true if email has attachments, false otherwise
     */
    public boolean hasAttachments(Message message) {
        // BobAndMe TODO: Check if email has attachments
        try {
            if (message != null && message.hasAttachments != null) {
                return message.hasAttachments;
            }
        } catch (Exception e) {
            log.warn("Error checking attachments: {}", e.getMessage());
        }
        return false;
    }
}

// Made with Bob