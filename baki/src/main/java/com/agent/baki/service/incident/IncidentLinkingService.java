package com.agent.baki.service.incident;

import com.agent.baki.dto.IncidentDTO;
import com.agent.baki.entity.Application;
import com.agent.baki.entity.Fix;
import com.agent.baki.entity.Jira;
import com.agent.baki.entity.Mail;
import com.agent.baki.repository.JiraRepository;
import com.agent.baki.repository.MailRepository;
import com.agent.baki.service.fix.FixLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for linking incidents to applications and fixes
 * 
 * Handles:
 * - Creating Mail entries from email incidents
 * - Creating Jira entries from Jira incidents
 * - Linking incidents to Application entities
 * - Linking incidents to Fix entities
 * - Preventing duplicate incident entries
 * 
 * This service bridges the gap between incident data (IncidentDTO)
 * and database entities (Mail/Jira), establishing proper relationships.
 * 
 * @author Team Baki
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentLinkingService {
    
    private final MailRepository mailRepository;
    private final JiraRepository jiraRepository;
    private final FixLookupService fixLookupService;
    
    /**
     * BobAndMe TODO: Create or update Mail entry for email incident
     * 
     * Implementation Requirements:
     * 1. Validate incident is EMAIL type
     * 2. Check if mail already exists by emailMessageId (avoid duplicates)
     * 3. If exists, return existing mail
     * 4. If not exists, create new Mail entity
     * 5. Set all mail fields from incident
     * 6. Link to application (get or create)
     * 7. Link to fix if provided
     * 8. Save and return mail
     * 9. Log creation
     * 
     * @param incident The email incident DTO
     * @param fix Optional fix to link (can be null)
     * @return Mail entity (existing or newly created)
     */
    @Transactional
    public Mail createOrUpdateMailEntry(IncidentDTO incident, Fix fix) {
        // Validate incident type
        if (incident == null) {
            throw new IllegalArgumentException("Incident cannot be null");
        }
        
        if (incident.getSource() != IncidentDTO.IncidentSource.EMAIL) {
            throw new IllegalArgumentException("Incident must be EMAIL type, got: " + incident.getSource());
        }
        
        if (incident.getSourceId() == null || incident.getSourceId().trim().isEmpty()) {
            throw new IllegalArgumentException("Email message ID cannot be null or empty");
        }
        
        try {
            String messageId = incident.getSourceId().trim();
            
            // Check for existing mail
            if (mailRepository.existsByEmailMessageId(messageId)) {
                Mail existingMail = mailRepository.findByEmailMessageId(messageId)
                        .orElseThrow(() -> new RuntimeException("Mail exists but not found: " + messageId));
                
                log.info("Found existing mail entry: mailId={}, messageId={}", 
                        existingMail.getMailId(), messageId);
                
                // Update fix if provided and not already set
                if (fix != null && existingMail.getFix() == null) {
                    existingMail.setFix(fix);
                    existingMail = mailRepository.save(existingMail);
                    log.info("Updated existing mail with fix: mailId={}, fixId={}", 
                            existingMail.getMailId(), fix.getFixId());
                }
                
                return existingMail;
            }
            
            // Create new mail entry
            Mail mail = new Mail();
            mail.setEmailMessageId(messageId);
            mail.setEmailSubject(incident.getTitle());
            mail.setEmailFrom(incident.getReporterName());
            mail.setReplied('N');
            
            // Set log location if available
            if (incident.getApplicationName() != null) {
                String logLocation = String.format("C:/Users/Baki/%s/issues/%s/logs/", 
                        incident.getApplicationName(), messageId);
                mail.setLogLocation(logLocation);
            }
            
            // Link to application
            if (incident.getApplicationName() != null && !incident.getApplicationName().trim().isEmpty()) {
                Application application = fixLookupService.getOrCreateApplication(
                        incident.getApplicationName()
                );
                mail.setApplication(application);
            }
            
            // Link to fix if provided
            if (fix != null) {
                mail.setFix(fix);
            }
            
            Mail savedMail = mailRepository.save(mail);
            
            log.info("Created new mail entry: mailId={}, messageId={}, fixId={}", 
                    savedMail.getMailId(), messageId, 
                    fix != null ? fix.getFixId() : "none");
            
            return savedMail;
            
        } catch (Exception e) {
            log.error("Error creating mail entry for incident {}: {}", 
                    incident.getSourceId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create mail entry", e);
        }
    }
    
    /**
     * BobAndMe TODO: Create or update Jira entry for Jira incident
     * 
     * Implementation Requirements:
     * 1. Validate incident is JIRA type
     * 2. Check if jira already exists by jiraTicketKey (avoid duplicates)
     * 3. If exists, return existing jira
     * 4. If not exists, create new Jira entity
     * 5. Set all jira fields from incident
     * 6. Link to application (get or create)
     * 7. Link to fix if provided
     * 8. Save and return jira
     * 9. Log creation
     * 
     * @param incident The Jira incident DTO
     * @param fix Optional fix to link (can be null)
     * @return Jira entity (existing or newly created)
     */
    @Transactional
    public Jira createOrUpdateJiraEntry(IncidentDTO incident, Fix fix) {
        // Validate incident type
        if (incident == null) {
            throw new IllegalArgumentException("Incident cannot be null");
        }
        
        if (incident.getSource() != IncidentDTO.IncidentSource.JIRA) {
            throw new IllegalArgumentException("Incident must be JIRA type, got: " + incident.getSource());
        }
        
        if (incident.getSourceId() == null || incident.getSourceId().trim().isEmpty()) {
            throw new IllegalArgumentException("Jira ticket key cannot be null or empty");
        }
        
        try {
            String ticketKey = incident.getSourceId().trim();
            
            // Check for existing jira
            if (jiraRepository.existsByJiraTicketKey(ticketKey)) {
                Jira existingJira = jiraRepository.findByJiraTicketKey(ticketKey)
                        .orElseThrow(() -> new RuntimeException("Jira exists but not found: " + ticketKey));
                
                log.info("Found existing jira entry: jiraId={}, ticketKey={}", 
                        existingJira.getJiraId(), ticketKey);
                
                // Update fix if provided and not already set
                if (fix != null && existingJira.getFix() == null) {
                    existingJira.setFix(fix);
                    existingJira = jiraRepository.save(existingJira);
                    log.info("Updated existing jira with fix: jiraId={}, fixId={}", 
                            existingJira.getJiraId(), fix.getFixId());
                }
                
                return existingJira;
            }
            
            // Create new jira entry
            Jira jira = new Jira();
            jira.setJiraTicketKey(ticketKey);
            jira.setJiraSummary(incident.getTitle());
            jira.setJiraIssueType(extractIssueType(incident.getReporterName()));
            jira.setReplied('N');
            
            // Set log location if available
            if (incident.getApplicationName() != null) {
                String logLocation = String.format("C:/Users/Baki/%s/issues/%s/logs/", 
                        incident.getApplicationName(), ticketKey);
                jira.setLogLocation(logLocation);
            }
            
            // Link to application
            if (incident.getApplicationName() != null && !incident.getApplicationName().trim().isEmpty()) {
                Application application = fixLookupService.getOrCreateApplication(
                        incident.getApplicationName()
                );
                jira.setApplication(application);
            }
            
            // Link to fix if provided
            if (fix != null) {
                jira.setFix(fix);
            }
            
            Jira savedJira = jiraRepository.save(jira);
            
            log.info("Created new jira entry: jiraId={}, ticketKey={}, fixId={}", 
                    savedJira.getJiraId(), ticketKey, 
                    fix != null ? fix.getFixId() : "none");
            
            return savedJira;
            
        } catch (Exception e) {
            log.error("Error creating jira entry for incident {}: {}", 
                    incident.getSourceId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create jira entry", e);
        }
    }
    
    /**
     * BobAndMe TODO: Link incident to fix
     * 
     * Implementation Requirements:
     * 1. Determine incident type (EMAIL or JIRA)
     * 2. Call appropriate method (createOrUpdateMailEntry or createOrUpdateJiraEntry)
     * 3. Pass fix for linking
     * 4. Return created/updated entity
     * 5. Log linking
     * 
     * Convenience method that handles both incident types
     * 
     * @param incident The incident DTO
     * @param fix The fix to link
     * @return Mail or Jira entity (as Object)
     */
    @Transactional
    public Object linkIncidentToFix(IncidentDTO incident, Fix fix) {
        if (incident == null) {
            throw new IllegalArgumentException("Incident cannot be null");
        }
        
        if (fix == null) {
            throw new IllegalArgumentException("Fix cannot be null");
        }
        
        log.info("Linking incident {} to fix {}", incident.getSourceId(), fix.getFixId());
        
        if (incident.getSource() == IncidentDTO.IncidentSource.EMAIL) {
            return createOrUpdateMailEntry(incident, fix);
        } else if (incident.getSource() == IncidentDTO.IncidentSource.JIRA) {
            return createOrUpdateJiraEntry(incident, fix);
        } else {
            throw new IllegalArgumentException("Unknown incident source: " + incident.getSource());
        }
    }
    
    /**
     * BobAndMe TODO: Create incident entry without fix (for initial processing)
     * 
     * Implementation Requirements:
     * 1. Determine incident type
     * 2. Call appropriate method with null fix
     * 3. Return created entity
     * 4. Log creation
     * 
     * Used when incident is first received, before analysis
     * 
     * @param incident The incident DTO
     * @return Mail or Jira entity (as Object)
     */
    @Transactional
    public Object createIncidentEntry(IncidentDTO incident) {
        if (incident == null) {
            throw new IllegalArgumentException("Incident cannot be null");
        }
        
        log.info("Creating incident entry for: {}", incident.getSourceId());
        
        if (incident.getSource() == IncidentDTO.IncidentSource.EMAIL) {
            return createOrUpdateMailEntry(incident, null);
        } else if (incident.getSource() == IncidentDTO.IncidentSource.JIRA) {
            return createOrUpdateJiraEntry(incident, null);
        } else {
            throw new IllegalArgumentException("Unknown incident source: " + incident.getSource());
        }
    }
    
    /**
     * BobAndMe TODO: Check if incident already exists in database
     * 
     * Implementation Requirements:
     * 1. Determine incident type
     * 2. Check existence using appropriate repository
     * 3. Return boolean
     * 4. Log check result
     * 
     * Used to avoid processing duplicate incidents
     * 
     * @param incident The incident DTO
     * @return true if incident already exists
     */
    @Transactional(readOnly = true)
    public boolean incidentExists(IncidentDTO incident) {
        if (incident == null || incident.getSourceId() == null) {
            return false;
        }
        
        boolean exists;
        
        if (incident.getSource() == IncidentDTO.IncidentSource.EMAIL) {
            exists = mailRepository.existsByEmailMessageId(incident.getSourceId());
        } else if (incident.getSource() == IncidentDTO.IncidentSource.JIRA) {
            exists = jiraRepository.existsByJiraTicketKey(incident.getSourceId());
        } else {
            exists = false;
        }
        
        log.debug("Incident exists check for {}: {}", incident.getSourceId(), exists);
        
        return exists;
    }
    
    /**
     * BobAndMe TODO: Extract issue type from Jira summary
     * 
     * Implementation Requirements:
     * 1. Check if summary contains "Bug", "Incident", "Issue", "Ticket"
     * 2. Return matched type or "Incident" as default
     * 3. Case-insensitive matching
     * 
     * Helper method to categorize Jira tickets
     * 
     * @param summary The Jira summary/subject
     * @return Issue type string
     */
    private String extractIssueType(String summary) {
        if (summary == null) {
            return "Incident";
        }
        
        String lowerSummary = summary.toLowerCase();
        
        if (lowerSummary.contains("bug")) {
            return "Bug";
        } else if (lowerSummary.contains("incident")) {
            return "Incident";
        } else if (lowerSummary.contains("issue")) {
            return "Issue";
        } else if (lowerSummary.contains("ticket")) {
            return "Ticket";
        } else {
            return "Incident";
        }
    }
    
    /**
     * BobAndMe TODO: Update incident with fix link
     * 
     * Implementation Requirements:
     * 1. Find existing incident by sourceId
     * 2. Update fix reference
     * 3. Save and return updated entity
     * 4. Log update
     * 
     * Used when fix is created after incident entry
     * 
     * @param incident The incident DTO
     * @param fix The fix to link
     * @return Updated Mail or Jira entity
     */
    @Transactional
    public Object updateIncidentWithFix(IncidentDTO incident, Fix fix) {
        if (incident == null) {
            throw new IllegalArgumentException("Incident cannot be null");
        }
        
        if (fix == null) {
            throw new IllegalArgumentException("Fix cannot be null");
        }
        
        log.info("Updating incident {} with fix {}", incident.getSourceId(), fix.getFixId());
        
        if (incident.getSource() == IncidentDTO.IncidentSource.EMAIL) {
            Mail mail = mailRepository.findByEmailMessageId(incident.getSourceId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Mail not found: " + incident.getSourceId()));
            
            mail.setFix(fix);
            Mail updated = mailRepository.save(mail);
            
            log.info("Updated mail {} with fix {}", updated.getMailId(), fix.getFixId());
            return updated;
            
        } else if (incident.getSource() == IncidentDTO.IncidentSource.JIRA) {
            Jira jira = jiraRepository.findByJiraTicketKey(incident.getSourceId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Jira not found: " + incident.getSourceId()));
            
            jira.setFix(fix);
            Jira updated = jiraRepository.save(jira);
            
            log.info("Updated jira {} with fix {}", updated.getJiraId(), fix.getFixId());
            return updated;
            
        } else {
            throw new IllegalArgumentException("Unknown incident source: " + incident.getSource());
        }
    }
}

// Made with Bob