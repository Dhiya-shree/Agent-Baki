package com.agent.baki.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity class representing a Jira Incident in the system
 * 
 * Maps to the 'jira' table in the database
 * Links Jira incidents to fixes and applications
 * 
 * Key Features:
 * - Many-to-One with Fix: Multiple Jira tickets can link to the same fix (deduplication)
 * - Many-to-One with Application: Each Jira ticket belongs to one application
 * - Reply Tracking: Tracks whether automated comment has been added
 * - Log Storage: Stores file system path to incident logs
 * 
 * Relationships:
 * - Many-to-One with Fix: Each jira can be linked to one fix (nullable)
 * - Many-to-One with Application: Each jira belongs to one application
 * 
 * @author Team Baki
 */
@Entity
@Table(name = "jira", indexes = {
    @Index(name = "idx_jira_replied_fix", columnList = "replied,fix_id"),
    @Index(name = "idx_jira_application", columnList = "application_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Jira {
    
    /**
     * Primary key - Auto-generated unique identifier for the jira incident
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "jira_id")
    private Long jiraId;
    
    /**
     * Many-to-One relationship with Fix
     * Links this Jira ticket to a code fix (nullable - set after analysis)
     * Foreign key: fix_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fix_id")
    private Fix fix;
    
    /**
     * Many-to-One relationship with Application
     * Links this Jira ticket to an application (nullable - set after parsing)
     * Foreign key: application_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;
    
    /**
     * Reply status flag
     * 'N' = Not replied (default)
     * 'Y' = Comment added
     * Used by batch job to find unreplied incidents
     */
    @Column(name = "replied", length = 1, nullable = false)
    private Character replied = 'N';
    
    /**
     * File system path where logs are stored
     * Example: C:/Users/MyApp/issues/jira_PROJ-123/logs
     * Used to read logs for analysis and cleanup after reply
     */
    @Column(name = "log_location", length = 500)
    private String logLocation;
    
    /**
     * Unique Jira ticket key
     * Example: "PROJ-123" or "BUG-456"
     * Used to identify and add comments to the specific ticket
     * Must be unique across all jira records
     */
    @Column(name = "jira_ticket_key", unique = true, length = 50, nullable = false)
    private String jiraTicketKey;
    
    /**
     * Summary/title of the Jira ticket
     * Example: "Production error in user authentication service"
     * Used for display and tracking
     */
    @Column(name = "jira_summary", length = 500)
    private String jiraSummary;
    
    /**
     * Type of Jira issue
     * Example: "Bug", "Incident", "Task"
     * Used for categorization and filtering
     */
    @Column(name = "jira_issue_type", length = 50)
    private String jiraIssueType;
    
    /**
     * Timestamp when the jira record was created
     * Automatically set by Hibernate on insert
     */
    @CreationTimestamp
    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;
    
    /**
     * Timestamp when the comment was added
     * Set by batch job when replied = 'Y'
     */
    @Column(name = "replied_time")
    private LocalDateTime repliedTime;
    
    /**
     * Check if this jira ticket has been replied to
     * 
     * @return true if comment has been added
     */
    public boolean isReplied() {
        return 'Y' == this.replied;
    }
    
    /**
     * Mark this jira ticket as replied
     * Sets replied flag to 'Y' and records the reply time
     */
    public void markAsReplied() {
        this.replied = 'Y';
        this.repliedTime = LocalDateTime.now();
    }
    
    /**
     * Check if this jira ticket is ready for reply
     * Ready when: not replied yet AND has a fix assigned
     * 
     * @return true if ready for automated comment
     */
    public boolean isReadyForReply() {
        return !isReplied() && this.fix != null;
    }
}

// Made with Bob
