package com.agent.baki.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity class representing an Application in the system
 * 
 * Maps to the 'application_data' table in the database
 * Stores application metadata including name and repository link
 * 
 * Relationships:
 * - One-to-Many with Fix: An application can have multiple fixes
 * - One-to-Many with Mail: An application can receive multiple email incidents
 * - One-to-Many with Jira: An application can have multiple Jira incidents
 * 
 * @author Team Baki
 */
@Entity
@Table(name = "application_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    
    /**
     * Primary key - Auto-generated unique identifier for the application
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long applicationId;
    
    /**
     * Unique name of the application
     * Used to identify and group incidents by application
     * Must be unique across all applications
     */
    @Column(name = "application_name", nullable = false, unique = true, length = 255)
    private String applicationName;
    
    /**
     * Local repository path or Git URL for the application
     * Used by AI analysis to locate code files
     * Example: C:/Users/Developer/repos/myapp
     */
    @Column(name = "repository_link", length = 500)
    private String repositoryLink;
    
    /**
     * Timestamp when the application record was created
     * Automatically set by Hibernate on insert
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the application record was last updated
     * Automatically updated by Hibernate on any modification
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * One-to-Many relationship with Fix entity
     * An application can have multiple code fixes
     * Cascade: All operations cascade to child fixes
     * Orphan Removal: Fixes are deleted when removed from this collection
     */
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true,fetch=FetchType.EAGER)
    private List<Fix> fixes = new ArrayList<>();
    
    /**
     * One-to-Many relationship with Mail entity
     * An application can receive multiple email incidents
     * Cascade: All operations cascade to child mails
     * Orphan Removal: Mails are deleted when removed from this collection
     */
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Mail> mails = new ArrayList<>();
    
    /**
     * One-to-Many relationship with Jira entity
     * An application can have multiple Jira incidents
     * Cascade: All operations cascade to child jiras
     * Orphan Removal: Jiras are deleted when removed from this collection
     */
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Jira> jiras = new ArrayList<>();
    
    // Helper methods for bidirectional relationship management
    
    /**
     * Add a fix to this application
     * Maintains bidirectional relationship
     * 
     * @param fix The fix to add
     */
    public void addFix(Fix fix) {
        fixes.add(fix);
        fix.setApplication(this);
    }
    
    /**
     * Remove a fix from this application
     * Maintains bidirectional relationship
     * 
     * @param fix The fix to remove
     */
    public void removeFix(Fix fix) {
        fixes.remove(fix);
        fix.setApplication(null);
    }
    
    /**
     * Add a mail incident to this application
     * Maintains bidirectional relationship
     * 
     * @param mail The mail to add
     */
    public void addMail(Mail mail) {
        mails.add(mail);
        mail.setApplication(this);
    }
    
    /**
     * Remove a mail incident from this application
     * Maintains bidirectional relationship
     * 
     * @param mail The mail to remove
     */
    public void removeMail(Mail mail) {
        mails.remove(mail);
        mail.setApplication(null);
    }
    
    /**
     * Add a Jira incident to this application
     * Maintains bidirectional relationship
     * 
     * @param jira The jira to add
     */
    public void addJira(Jira jira) {
        jiras.add(jira);
        jira.setApplication(this);
    }
    
    /**
     * Remove a Jira incident from this application
     * Maintains bidirectional relationship
     * 
     * @param jira The jira to remove
     */
    public void removeJira(Jira jira) {
        jiras.remove(jira);
        jira.setApplication(null);
    }
}

// Made with Bob
