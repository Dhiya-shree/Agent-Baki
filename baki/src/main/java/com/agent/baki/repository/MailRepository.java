package com.agent.baki.repository;

import com.agent.baki.entity.Mail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Mail entity
 * 
 * Provides data access operations for Mail table
 * Extends JpaRepository for basic CRUD operations
 * 
 * Custom Query Methods:
 * - findByRepliedAndFixIsNotNull: Find unreplied mails with assigned fixes (for batch job)
 * - findByEmailMessageId: Find mail by unique Outlook message ID
 * - findByApplicationApplicationId: Get all mails for an application
 * 
 * @author Team Baki
 */
@Repository
public interface MailRepository extends JpaRepository<Mail, Long> {
    
    /**
     * Find all unreplied mails that have a fix assigned
     * 
     * Critical for batch job:
     * - Finds mails ready for automated reply
     * - Only includes mails with fix_id (analysis complete)
     * - Only includes mails with replied = 'N'
     * 
     * @param replied The replied status ('N' for unreplied)
     * @return List of mails ready for reply
     */
    List<Mail> findByRepliedAndFixIsNotNull(Character replied);
    
    /**
     * Find a mail by its unique Outlook message ID
     * 
     * Used when:
     * - Checking if email already processed (avoid duplicates)
     * - Replying to specific email
     * 
     * @param messageId The unique Outlook message ID
     * @return Optional containing the mail if found
     */
    Optional<Mail> findByEmailMessageId(String messageId);
    
    /**
     * Check if a mail exists by message ID
     * 
     * More efficient than findByEmailMessageId when only checking existence
     * 
     * @param messageId The Outlook message ID to check
     * @return true if mail exists, false otherwise
     */
    boolean existsByEmailMessageId(String messageId);
    
    /**
     * Find all mails for a specific application
     * 
     * Used when:
     * - Displaying incident history for an application
     * - Generating reports
     * 
     * @param applicationId The application ID
     * @return List of mails for the application
     */
    List<Mail> findByApplicationApplicationId(Long applicationId);
    
    /**
     * Find all unreplied mails (regardless of fix assignment)
     * 
     * Used when:
     * - Monitoring pending incidents
     * - Dashboard statistics
     * 
     * @param replied The replied status ('N' for unreplied)
     * @return List of all unreplied mails
     */
    List<Mail> findByReplied(Character replied);
    
    /**
     * Count unreplied mails
     * 
     * Used for dashboard metrics
     * 
     * @param replied The replied status ('N' for unreplied)
     * @return Number of unreplied mails
     */
    long countByReplied(Character replied);
    
    /**
     * Find mails without assigned fix (analysis pending or failed)
     * 
     * Used when:
     * - Monitoring incidents awaiting analysis
     * - Troubleshooting analysis failures
     * 
     * @return List of mails without fix assignment
     */
    @Query("SELECT m FROM Mail m WHERE m.fix IS NULL AND m.replied = 'N'")
    List<Mail> findMailsWithoutFix();
}

// Made with Bob
