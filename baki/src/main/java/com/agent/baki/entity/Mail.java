package com.agent.baki.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity class representing an Email Incident in the system
 * 
 * Maps to the 'mail' table in the database
 * Links email incidents from Outlook to fixes and applications
 * 
 * Key Features:
 * - Many-to-One with Fix: Multiple emails can link to the same fix (deduplication)
 * - Many-to-One with Application: Each email belongs to one application
 * - Reply Tracking: Tracks whether automated reply has been sent
 * - Log Storage: Stores file system path to incident logs
 * 
 * Relationships:
 * - Many-to-One with Fix: Each mail can be linked to one fix (nullable)
 * - Many-to-One with Application: Each mail belongs to one application
 * 
 * @author Team Baki
 */
@Entity
@Table(name = "mail", indexes = {
    @Index(name = "idx_mail_replied_fix", columnList = "replied,fix_id"),
    @Index(name = "idx_mail_application", columnList = "application_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mail {
    
    /**
     * Primary key - Auto-generated unique identifier for the mail incident
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mail_id")
    private Long mailId;
    
    /**
     * Many-to-One relationship with Fix
     * Links this email to a code fix (nullable - set after analysis)
     * Foreign key: fix_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fix_id")
    private Fix fix;
    
    /**
     * Many-to-One relationship with Application
     * Links this email to an application (nullable - set after parsing)
     * Foreign key: application_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;
    
    /**
     * Reply status flag
     * 'N' = Not replied (default)
     * 'Y' = Reply sent
     * Used by batch job to find unreplied incidents
     */
    @Column(name = "replied", length = 1, nullable = false)
    private Character replied = 'N';
    
    /**
     * File system path where logs are stored
     * Example: C:/Users/MyApp/issues/mail_123/logs
     * Used to read logs for analysis and cleanup after reply
     */
    @Column(name = "log_location", length = 500)
    private String logLocation;
    
    /**
     * Subject line of the email
     * Example: "Issue: Production error in user service"
     * Used for display and tracking
     */
    @Column(name = "email_subject", length = 500)
    private String emailSubject;
    
    /**
     * Email address of the sender
     * Example: "developer@company.com"
     * Used for tracking and potential follow-up
     */
    @Column(name = "email_from", length = 255)
    private String emailFrom;
    
    /**
     * Unique message ID from Outlook
     * Used to identify and reply to the specific email
     * Must be unique across all mail records
     */
    @Column(name = "email_message_id", unique = true, length = 255)
    private String emailMessageId;
    
    /**
     * Timestamp when the mail record was created
     * Automatically set by Hibernate on insert
     */
    @CreationTimestamp
    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;
    
    /**
     * Timestamp when the reply was sent
     * Set by batch job when replied = 'Y'
     */
    @Column(name = "replied_time")
    private LocalDateTime repliedTime;
    
    /**
     * Check if this mail has been replied to
     * 
     * @return true if reply has been sent
     */
    public boolean isReplied() {
        return 'Y' == this.replied;
    }
    
    /**
     * Mark this mail as replied
     * Sets replied flag to 'Y' and records the reply time
     */
    public void markAsReplied() {
        this.replied = 'Y';
        this.repliedTime = LocalDateTime.now();
    }
    
    /**
     * Check if this mail is ready for reply
     * Ready when: not replied yet AND has a fix assigned
     * 
     * @return true if ready for automated reply
     */
    public boolean isReadyForReply() {
        return !isReplied() && this.fix != null;
    }
}

// Made with Bob
