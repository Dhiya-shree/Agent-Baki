package com.agent.baki.service.parser;

import com.agent.baki.dto.IncidentDTO;
import com.agent.baki.service.integration.OutlookService;
import com.agent.baki.service.integration.JiraService;
import com.agent.baki.service.storage.FileStorageService;
import com.agent.baki.util.IncidentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for parsing and processing incidents
 *
 * Handles:
 * - Extracting application name from incident
 * - Fetching and validating log files
 * - Saving logs to file system
 * - Updating incident DTO with extracted data
 *
 * @author Team Baki
 */
@Service
@Slf4j
public class IncidentParserService {
    
    @Autowired(required = false)
    private OutlookService outlookService;
    
    @Autowired(required = false)
    private JiraService jiraService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    /**
     * Parse and process incident
     *
     * Extracts application name and fetches log files
     *
     * @param incident The incident to parse
     * @return Updated incident DTO with extracted data
     */
    public IncidentDTO parseIncident(IncidentDTO incident) {
        // BobAndMe TODO: Parse and process incident
        if (incident == null) {
            log.error("Cannot parse null incident");
            return null;
        }
        
        log.info("Parsing incident: {} from {}", incident.getSourceId(), incident.getSource());
        
        // Extract application name
        extractApplicationName(incident);
        
        // Fetch log files if incident has attachments
        if (incident.isHasAttachments()) {
            fetchLogFiles(incident);
        }
        
        // Update readiness status
        incident.updateReadinessStatus();
        
        log.info("Parsed incident {}: appName={}, hasLogs={}, ready={}",
                incident.getSourceId(),
                incident.isHasApplicationName(),
                incident.isHasLogFiles(),
                incident.isReadyForProcessing());
        
        return incident;
    }
    
    /**
     * Extract application name from incident
     *
     * Uses IncidentMapper to extract app name from title/description
     *
     * @param incident The incident to process
     * @return true if application name was extracted, false otherwise
     */
    public boolean extractApplicationName(IncidentDTO incident) {
        // BobAndMe TODO: Extract application name from incident
        if (incident == null) {
            return false;
        }
        
        String applicationName = IncidentMapper.extractApplicationName(incident);
        
        if (applicationName != null && !applicationName.trim().isEmpty()) {
            incident.setApplicationNameAndUpdateStatus(applicationName);
            log.info("Extracted application name '{}' from incident {}",
                    applicationName, incident.getSourceId());
            return true;
        }
        
        log.warn("Could not extract application name from incident {}", incident.getSourceId());
        return false;
    }
    
    /**
     * Fetch and process log files for incident
     *
     * Fetches attachments based on source type (EMAIL or JIRA)
     *
     * @param incident The incident to process
     * @return true if log files were fetched, false otherwise
     */
    public boolean fetchLogFiles(IncidentDTO incident) {
        // BobAndMe TODO: Fetch and process log files for incident
        if (incident == null) {
            return false;
        }
        
        Map<String, byte[]> logFiles = null;
        
        // Fetch based on source type
        if (incident.isFromEmail()) {
            if (outlookService != null) {
                log.info("Fetching email attachments for incident {}", incident.getSourceId());
                logFiles = outlookService.getEmailAttachments(incident.getSourceId());
            } else {
                log.warn("OutlookService is not available (disabled). Cannot fetch email attachments for incident {}", incident.getSourceId());
                return false;
            }
            
        } else if (incident.isFromJira()) {
            if (jiraService != null) {
                log.warn("Jira attachment fetching not yet implemented for incident {}",
                        incident.getSourceId());
                // TODO: Implement Jira attachment fetching in future
                return false;
            } else {
                log.warn("JiraService is not available (disabled). Cannot fetch Jira attachments for incident {}", incident.getSourceId());
                return false;
            }
        }
        
        // Validate and update incident
        if (logFiles != null && !logFiles.isEmpty()) {
            incident.setLogFilesAndUpdateStatus(logFiles);
            log.info("Fetched {} log files for incident {}",
                    logFiles.size(), incident.getSourceId());
            return true;
        }
        
        log.warn("No log files found for incident {}", incident.getSourceId());
        return false;
    }
    
    /**
     * Save log files to file system
     *
     * Saves to: C:/Users/Baki/{appName}/issues/{sourceId}/logs/
     *
     * @param incident The incident with log files
     * @return true if files were saved, false otherwise
     */
    public boolean saveLogFilesToFileSystem(IncidentDTO incident) {
        // BobAndMe TODO: Save log files to file system
        if (incident == null) {
            log.error("Cannot save log files: incident is null");
            return false;
        }
        
        if (!incident.isHasApplicationName()) {
            log.error("Cannot save log files: application name is missing for incident {}",
                    incident.getSourceId());
            return false;
        }
        
        if (!incident.isHasLogFiles()) {
            log.warn("No log files to save for incident {}", incident.getSourceId());
            return false;
        }
        
        int savedCount = fileStorageService.saveLogFiles(
                incident.getApplicationName(),
                incident.getSourceId(),
                incident.getLogFiles()
        );
        
        if (savedCount > 0) {
            log.info("Saved {} log files to file system for incident {}",
                    savedCount, incident.getSourceId());
            return true;
        }
        
        log.error("Failed to save log files for incident {}", incident.getSourceId());
        return false;
    }
    
    /**
     * Validate if incident is ready for processing
     *
     * Checks if both application name and log files are present
     *
     * @param incident The incident to validate
     * @return true if ready for processing, false otherwise
     */
    public boolean validateIncidentReadiness(IncidentDTO incident) {
        // BobAndMe TODO: Validate if incident is ready for processing
        if (incident == null) {
            return false;
        }
        
        if (incident.isReadyForProcessing()) {
            log.info("Incident {} is ready for processing", incident.getSourceId());
            return true;
        }
        
        // Log what is missing
        StringBuilder missing = new StringBuilder("Incident " + incident.getSourceId() + " is not ready: missing ");
        if (!incident.isHasApplicationName()) {
            missing.append("application name");
        }
        if (!incident.isHasLogFiles()) {
            if (!incident.isHasApplicationName()) {
                missing.append(" and ");
            }
            missing.append("log files");
        }
        
        log.warn(missing.toString());
        return false;
    }
    
    /**
     * Process incident completely
     *
     * Main entry point for incident processing
     * Parses, validates, and saves log files
     *
     * @param incident The incident to process
     * @return Fully processed incident DTO
     */
    public IncidentDTO processIncident(IncidentDTO incident) {
        // BobAndMe TODO: Process incident completely
        if (incident == null) {
            log.error("Cannot process null incident");
            return null;
        }
        
        log.info("Processing incident: {} from {}", incident.getSourceId(), incident.getSource());
        
        // Parse incident (extract app name and fetch logs)
        parseIncident(incident);
        
        // Save log files to file system if we have both app name and logs
        if (incident.isHasApplicationName() && incident.isHasLogFiles()) {
            saveLogFilesToFileSystem(incident);
        }
        
        // Validate readiness
        validateIncidentReadiness(incident);
        
        log.info("Completed processing incident {}: ready={}",
                incident.getSourceId(), incident.isReadyForProcessing());
        
        return incident;
    }
}

// Made with Bob