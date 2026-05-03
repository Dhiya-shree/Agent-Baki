package com.agent.baki.service.batch;

import com.agent.baki.entity.Jira;
import com.agent.baki.entity.Mail;
import com.agent.baki.repository.JiraRepository;
import com.agent.baki.repository.MailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service for cleaning up log files after successful reply
 * 
 * Deletes log files from file system once incidents have been replied to
 * 
 * @author Team Baki
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileCleanupService {
    
    private final MailRepository mailRepository;
    private final JiraRepository jiraRepository;
    
    /**
     * Clean up log files for all replied incidents
     * 
     * Finds all replied incidents and deletes their log files from file system
     * 
     * @return Number of log directories cleaned up
     */
    @Transactional(readOnly = true)
    public int cleanupRepliedIncidents() {
        log.info("Starting file cleanup for replied incidents");
        
        int cleanedCount = 0;
        
        try {
            // Clean up replied emails
            List<Mail> repliedMails = mailRepository.findByReplied('Y');
            for (Mail mail : repliedMails) {
                if (mail.getLogLocation() != null && !mail.getLogLocation().isEmpty()) {
                    if (deleteLogDirectory(mail.getLogLocation())) {
                        cleanedCount++;
                        log.debug("Cleaned up logs for mail: mailId={}", mail.getMailId());
                    }
                }
            }
            
            // Clean up replied jiras
            List<Jira> repliedJiras = jiraRepository.findByReplied('Y');
            for (Jira jira : repliedJiras) {
                if (jira.getLogLocation() != null && !jira.getLogLocation().isEmpty()) {
                    if (deleteLogDirectory(jira.getLogLocation())) {
                        cleanedCount++;
                        log.debug("Cleaned up logs for jira: jiraId={}", jira.getJiraId());
                    }
                }
            }
            
            log.info("File cleanup completed: {} log directories cleaned", cleanedCount);
            return cleanedCount;
            
        } catch (Exception e) {
            log.error("Error during file cleanup: {}", e.getMessage(), e);
            return cleanedCount;
        }
    }
    
    /**
     * Delete log directory and all its contents
     * 
     * Recursively deletes the directory and all files within it
     * 
     * @param logLocation The log directory path
     * @return true if deleted successfully
     */
    public boolean deleteLogDirectory(String logLocation) {
        if (logLocation == null || logLocation.trim().isEmpty()) {
            log.warn("Cannot delete: log location is null or empty");
            return false;
        }
        
        try {
            Path dirPath = Paths.get(logLocation);
            
            if (!Files.exists(dirPath)) {
                log.debug("Log directory does not exist: {}", logLocation);
                return true; // Already deleted or never created
            }
            
            if (!Files.isDirectory(dirPath)) {
                log.warn("Log location is not a directory: {}", logLocation);
                return false;
            }
            
            // Delete directory and all contents recursively
            try (Stream<Path> walk = Files.walk(dirPath)) {
                walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
            
            log.info("Successfully deleted log directory: {}", logLocation);
            return true;
            
        } catch (IOException e) {
            log.error("Failed to delete log directory {}: {}", logLocation, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error deleting log directory {}: {}", logLocation, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Clean up logs for a specific mail incident
     * 
     * Deletes log files for a single mail incident
     * 
     * @param mail The mail entity
     * @return true if cleaned successfully
     */
    public boolean cleanupMailLogs(Mail mail) {
        if (mail == null) {
            log.warn("Cannot cleanup: mail is null");
            return false;
        }
        
        if (!mail.isReplied()) {
            log.warn("Cannot cleanup: mail has not been replied yet (mailId={})", mail.getMailId());
            return false;
        }
        
        if (mail.getLogLocation() == null || mail.getLogLocation().isEmpty()) {
            log.debug("No log location for mail: mailId={}", mail.getMailId());
            return true;
        }
        
        log.info("Cleaning up logs for mail: mailId={}, location={}", 
                mail.getMailId(), mail.getLogLocation());
        
        return deleteLogDirectory(mail.getLogLocation());
    }
    
    /**
     * Clean up logs for a specific Jira incident
     * 
     * Deletes log files for a single Jira incident
     * 
     * @param jira The jira entity
     * @return true if cleaned successfully
     */
    public boolean cleanupJiraLogs(Jira jira) {
        if (jira == null) {
            log.warn("Cannot cleanup: jira is null");
            return false;
        }
        
        if (!jira.isReplied()) {
            log.warn("Cannot cleanup: jira has not been replied yet (jiraId={})", jira.getJiraId());
            return false;
        }
        
        if (jira.getLogLocation() == null || jira.getLogLocation().isEmpty()) {
            log.debug("No log location for jira: jiraId={}", jira.getJiraId());
            return true;
        }
        
        log.info("Cleaning up logs for jira: jiraId={}, location={}", 
                jira.getJiraId(), jira.getLogLocation());
        
        return deleteLogDirectory(jira.getLogLocation());
    }
    
    /**
     * Clean up log files for replied incidents
     *
     * Deletes log files and directories for a list of replied incidents
     *
     * @param repliedIncidents List of IncidentDTO that have been replied
     * @return true if all cleanups successful, false if any failed
     */
    public boolean cleanupRepliedIncidentLogs(List<com.agent.baki.dto.IncidentDTO> repliedIncidents) {
        if (repliedIncidents == null || repliedIncidents.isEmpty()) {
            log.debug("No incidents to cleanup");
            return true;
        }
        
        log.info("Cleaning up logs for {} replied incidents", repliedIncidents.size());
        
        int successCount = 0;
        int failureCount = 0;
        
        for (com.agent.baki.dto.IncidentDTO incident : repliedIncidents) {
            try {
                // Build log location path
                String logLocation = buildLogLocation(incident);
                
                if (logLocation != null && !logLocation.isEmpty()) {
                    if (deleteLogDirectory(logLocation)) {
                        successCount++;
                        log.debug("Cleaned up logs for incident: {}", incident.getSourceId());
                    } else {
                        failureCount++;
                        log.warn("Failed to cleanup logs for incident: {}", incident.getSourceId());
                    }
                } else {
                    log.debug("No log location for incident: {}", incident.getSourceId());
                    successCount++; // Count as success since there's nothing to clean
                }
                
            } catch (Exception e) {
                failureCount++;
                log.error("Error cleaning up logs for incident {}: {}",
                        incident.getSourceId(), e.getMessage(), e);
            }
        }
        
        log.info("Log cleanup completed: success={}, failure={}", successCount, failureCount);
        
        return failureCount == 0;
    }
    
    /**
     * Build log location path from IncidentDTO
     *
     * Constructs the file system path where logs are stored for an incident
     *
     * @param incident The incident DTO
     * @return Log location path
     */
    private String buildLogLocation(com.agent.baki.dto.IncidentDTO incident) {
        if (incident == null || incident.getApplicationName() == null || incident.getSourceId() == null) {
            return null;
        }
        
        // Format: C:/Users/Baki/{appName}/issues/{sourceId}/logs/
        return String.format("C:/Users/Baki/%s/issues/%s/logs/",
                incident.getApplicationName(),
                incident.getSourceId());
    }
    
    /**
     * Clean up old unreplied incident logs (older than specified days)
     *
     * Deletes log files for incidents that are older than the retention period
     * even if they haven't been replied to (to prevent disk space issues)
     *
     * @param retentionDays Number of days to retain logs
     * @return Number of log directories cleaned up
     */
    @Transactional(readOnly = true)
    public int cleanupOldLogs(int retentionDays) {
        log.info("Starting cleanup of logs older than {} days", retentionDays);
        
        // This would require adding a query to find old incidents
        // For now, just log the intent
        log.warn("Old log cleanup not yet implemented - retention days: {}", retentionDays);
        
        return 0;
    }
    
    /**
     * Get total size of log files
     * 
     * Calculates the total disk space used by log files
     * 
     * @return Total size in bytes
     */
    public long getTotalLogSize() {
        long totalSize = 0;
        
        try {
            // Get all mail log locations
            List<Mail> allMails = mailRepository.findAll();
            for (Mail mail : allMails) {
                if (mail.getLogLocation() != null) {
                    totalSize += getDirectorySize(mail.getLogLocation());
                }
            }
            
            // Get all jira log locations
            List<Jira> allJiras = jiraRepository.findAll();
            for (Jira jira : allJiras) {
                if (jira.getLogLocation() != null) {
                    totalSize += getDirectorySize(jira.getLogLocation());
                }
            }
            
            log.info("Total log size: {} bytes ({} MB)", totalSize, totalSize / (1024 * 1024));
            
        } catch (Exception e) {
            log.error("Error calculating total log size: {}", e.getMessage(), e);
        }
        
        return totalSize;
    }
    
    /**
     * Get size of a directory
     * 
     * Calculates the total size of all files in a directory
     * 
     * @param dirPath The directory path
     * @return Size in bytes
     */
    private long getDirectorySize(String dirPath) {
        try {
            Path path = Paths.get(dirPath);
            
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                return 0;
            }
            
            try (Stream<Path> walk = Files.walk(path)) {
                return walk
                        .filter(Files::isRegularFile)
                        .mapToLong(p -> {
                            try {
                                return Files.size(p);
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .sum();
            }
            
        } catch (IOException e) {
            log.error("Error calculating directory size for {}: {}", dirPath, e.getMessage());
            return 0;
        }
    }
}

// Made with Bob