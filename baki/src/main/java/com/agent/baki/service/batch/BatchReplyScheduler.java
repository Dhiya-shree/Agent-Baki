package com.agent.baki.service.batch;

import com.agent.baki.dto.IncidentDTO;
import com.agent.baki.repository.JiraRepository;
import com.agent.baki.repository.MailRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Batch Job Scheduler for Automated Replies
 *
 * Runs scheduled jobs to process unreplied incidents and send automated responses
 *
 * Schedule:
 * */
 //- Runs every 2 hours (cron: 0 0 */2 * * ?)
 /* - Processes all incidents with replied='N' and fix assigned
 * - Sends email replies for Mail incidents
 * - Adds Jira comments for Jira incidents
 * - Cleans up log files after successful reply
 *
 * Flow:
 * 1. Find all unreplied mails with fix_id (replied='N' AND fix_id IS NOT NULL)
 * 2. Find all unreplied jiras with fix_id (replied='N' AND fix_id IS NOT NULL)
 * 3. For each mail: send reply with fix details
 * 4. For each jira: add comment with fix details
 * 5. Mark as replied (replied='Y', replied_time=NOW)
 * 6. Delete log files from file system
 * 7. Log batch statistics
 *
 * @author Team Baki
 */
@Service
@Slf4j
public class BatchReplyScheduler {
    
    @Autowired(required = false)
    private BatchEmailReplyService batchEmailReplyService;
    
    @Autowired(required = false)
    private BatchJiraCommentService batchJiraCommentService;
    
    @Autowired
    private FileCleanupService fileCleanupService;
    
    @Autowired
    private MailRepository mailRepository;
    
    @Autowired
    private JiraRepository jiraRepository;
    
    private String lastRunTime = "Never";
    
    /**
     * Scheduled batch job to process unreplied incidents
     *
     * Runs every 2 hours to:
     * 1. Process unreplied emails and get list of replied incidents
     * 2. Process unreplied Jira tickets and get list of replied incidents
     * 3. Cleanup log files for all replied incidents
     * 4. Log batch statistics
     *
     * Schedule: Every 2 hours (00:00, 02:00, 04:00, etc.)
     */
    @Scheduled(cron = "0 0 */2 * * ?")
    public void processUnrepliedIncidents() {
        log.info("========================================");
        log.info("Starting batch reply processing job");
        log.info("========================================");
        
        LocalDateTime startTime = LocalDateTime.now();
        List<IncidentDTO> allRepliedIncidents = new ArrayList<>();
        
        try {
            // Step 1: Process unreplied emails
            log.info("Step 1: Processing unreplied emails...");
            List<IncidentDTO> repliedEmails = new ArrayList<>();
            if (batchEmailReplyService != null) {
                repliedEmails = batchEmailReplyService.processUnrepliedEmails();
                allRepliedIncidents.addAll(repliedEmails);
                log.info("Processed {} emails successfully", repliedEmails.size());
            } else {
                log.warn("BatchEmailReplyService is not available (Outlook disabled). Skipping email processing.");
            }
            
            // Step 2: Process unreplied Jira tickets
            log.info("Step 2: Processing unreplied Jira tickets...");
            List<IncidentDTO> repliedJiras = new ArrayList<>();
            if (batchJiraCommentService != null) {
                repliedJiras = batchJiraCommentService.processUnrepliedJiras();
                allRepliedIncidents.addAll(repliedJiras);
                log.info("Processed {} Jira tickets successfully", repliedJiras.size());
            } else {
                log.warn("BatchJiraCommentService is not available (Jira disabled). Skipping Jira processing.");
            }
            
            // Step 3: Cleanup log files for all replied incidents
            log.info("Step 3: Cleaning up log files for {} replied incidents...", allRepliedIncidents.size());
            boolean cleanupSuccess = fileCleanupService.cleanupRepliedIncidentLogs(allRepliedIncidents);
            
            if (cleanupSuccess) {
                log.info("Log cleanup completed successfully");
            } else {
                log.warn("Log cleanup completed with some failures");
            }
            
            // Update last run time
            lastRunTime = startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            // Log final statistics
            LocalDateTime endTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
            
            log.info("========================================");
            log.info("Batch reply processing completed");
            log.info("Total emails replied: {}", repliedEmails.size());
            log.info("Total Jira tickets commented: {}", repliedJiras.size());
            log.info("Total incidents processed: {}", allRepliedIncidents.size());
            log.info("Duration: {} seconds", durationSeconds);
            log.info("========================================");
            
        } catch (Exception e) {
            log.error("Error in batch reply processing: {}", e.getMessage(), e);
            log.error("========================================");
        }
    }
    
    /**
     * Manual trigger for batch processing (for testing/admin)
     *
     * Allows immediate processing without waiting for scheduled time
     * Useful for testing or when immediate processing is needed
     */
    public void triggerManualProcessing() {
        log.info("Manual batch processing triggered");
        processUnrepliedIncidents();
    }
    
    /**
     * Get batch processing statistics
     *
     * Returns current counts of unreplied incidents
     * Used for monitoring and dashboard display
     *
     * @return BatchStatistics object with counts
     */
    public BatchStatistics getBatchStatistics() {
        try {
            // Count unreplied mails with fix assigned
            int unrepliedEmails = 0;
            if (batchEmailReplyService != null) {
                unrepliedEmails = mailRepository.findByRepliedAndFixIsNotNull('N').size();
            }
            
            // Count unreplied jiras with fix assigned
            int unrepliedJiras = 0;
            if (batchJiraCommentService != null) {
                unrepliedJiras = jiraRepository.findByRepliedAndFixIsNotNull('N').size();
            }
            
            // Total pending
            int totalPending = unrepliedEmails + unrepliedJiras;
            
            return new BatchStatistics(unrepliedEmails, unrepliedJiras, totalPending, lastRunTime);
            
        } catch (Exception e) {
            log.error("Error getting batch statistics: {}", e.getMessage(), e);
            return new BatchStatistics(0, 0, 0, lastRunTime);
        }
    }
    
    /**
     * Inner class for batch statistics
     */
    public static class BatchStatistics {
        private int unrepliedEmails;
        private int unrepliedJiras;
        private int totalPending;
        private String lastRunTime;
        
        public BatchStatistics(int unrepliedEmails, int unrepliedJiras, int totalPending, String lastRunTime) {
            this.unrepliedEmails = unrepliedEmails;
            this.unrepliedJiras = unrepliedJiras;
            this.totalPending = totalPending;
            this.lastRunTime = lastRunTime;
        }
        
        // Getters
        public int getUnrepliedEmails() { return unrepliedEmails; }
        public int getUnrepliedJiras() { return unrepliedJiras; }
        public int getTotalPending() { return totalPending; }
        public String getLastRunTime() { return lastRunTime; }
    }
}

// Made with Bob