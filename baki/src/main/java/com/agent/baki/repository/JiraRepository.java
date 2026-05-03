package com.agent.baki.repository;

import com.agent.baki.entity.Jira;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Jira entity
 * 
 * Provides data access operations for Jira table
 * Extends JpaRepository for basic CRUD operations
 * 
 * Custom Query Methods:
 * - findByRepliedAndFixIsNotNull: Find unreplied jiras with assigned fixes (for batch job)
 * - findByJiraTicketKey: Find jira by unique ticket key
 * - findByApplicationApplicationId: Get all jiras for an application
 * 
 * @author Team Baki
 */
@Repository
public interface JiraRepository extends JpaRepository<Jira, Long> {
    
    /**
     * Find all unreplied Jira tickets that have a fix assigned
     * 
     * Critical for batch job:
     * - Finds Jira tickets ready for automated comment
     * - Only includes tickets with fix_id (analysis complete)
     * - Only includes tickets with replied = 'N'
     * 
     * @param replied The replied status ('N' for unreplied)
     * @return List of Jira tickets ready for comment
     */
    List<Jira> findByRepliedAndFixIsNotNull(Character replied);
    
    /**
     * Find a Jira ticket by its unique ticket key
     * 
     * Used when:
     * - Checking if ticket already processed (avoid duplicates)
     * - Adding comment to specific ticket
     * 
     * @param ticketKey The unique Jira ticket key (e.g., "PROJ-123")
     * @return Optional containing the jira if found
     */
    Optional<Jira> findByJiraTicketKey(String ticketKey);
    
    /**
     * Check if a Jira ticket exists by ticket key
     * 
     * More efficient than findByJiraTicketKey when only checking existence
     * 
     * @param ticketKey The Jira ticket key to check
     * @return true if ticket exists, false otherwise
     */
    boolean existsByJiraTicketKey(String ticketKey);
    
    /**
     * Find all Jira tickets for a specific application
     * 
     * Used when:
     * - Displaying incident history for an application
     * - Generating reports
     * 
     * @param applicationId The application ID
     * @return List of Jira tickets for the application
     */
    List<Jira> findByApplicationApplicationId(Long applicationId);
    
    /**
     * Find all unreplied Jira tickets (regardless of fix assignment)
     * 
     * Used when:
     * - Monitoring pending incidents
     * - Dashboard statistics
     * 
     * @param replied The replied status ('N' for unreplied)
     * @return List of all unreplied Jira tickets
     */
    List<Jira> findByReplied(Character replied);
    
    /**
     * Count unreplied Jira tickets
     * 
     * Used for dashboard metrics
     * 
     * @param replied The replied status ('N' for unreplied)
     * @return Number of unreplied Jira tickets
     */
    long countByReplied(Character replied);
    
    /**
     * Find Jira tickets without assigned fix (analysis pending or failed)
     * 
     * Used when:
     * - Monitoring incidents awaiting analysis
     * - Troubleshooting analysis failures
     * 
     * @return List of Jira tickets without fix assignment
     */
    @Query("SELECT j FROM Jira j WHERE j.fix IS NULL AND j.replied = 'N'")
    List<Jira> findJirasWithoutFix();
    
    /**
     * Find Jira tickets by issue type
     * 
     * Used when:
     * - Filtering tickets by type (Bug, Incident, Task)
     * - Generating type-specific reports
     * 
     * @param issueType The Jira issue type
     * @return List of Jira tickets with the specified type
     */
    List<Jira> findByJiraIssueType(String issueType);
}

// Made with Bob
