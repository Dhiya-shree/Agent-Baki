package com.agent.baki.repository;

import com.agent.baki.entity.Application;
import com.agent.baki.entity.Fix;
import com.agent.baki.entity.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Fix entity
 * 
 * Provides data access operations for Fix table
 * Extends JpaRepository for basic CRUD operations
 * 
 * Custom Query Methods:
 * - findByApplication: Get all fixes for an application
 * - findByApplicationAndCodeClassNameAndCodeLine: Find fix by exact code location (for deduplication)
 * - findByIssueStatus: Filter fixes by status
 * - countByApplicationAndIssueStatus: Count fixes by application and status
 * 
 * @author Team Baki
 */
@Repository
public interface FixRepository extends JpaRepository<Fix, Long> {
    
    /**
     * Find all fixes for a specific application
     * 
     * Used when:
     * - Displaying fixes in UI for an application
     * - Generating reports
     * 
     * @param application The application entity
     * @return List of fixes for the application
     */
    List<Fix> findByApplication(Application application);
    
    /**
     * Find a fix by exact code location (for deduplication)
     * 
     * Critical for deduplication logic:
     * - Before creating a new fix, check if one exists for same code location
     * - If exists, link new incident to existing fix
     * - If not, create new fix
     * 
     * @param application The application entity
     * @param codeClassName The fully qualified class name
     * @param codeLine The line number in the code
     * @return Optional containing the fix if found
     */
    Optional<Fix> findByApplicationAndCodeClassNameAndCodeLine(
        Application application, 
        String codeClassName, 
        Integer codeLine
    );
    
    /**
     * Find all fixes with a specific status
     * 
     * Used when:
     * - Filtering fixes by status in UI
     * - Generating status reports
     * - Finding pending fixes for review
     * 
     * @param status The issue status to filter by
     * @return List of fixes with the specified status
     */
    List<Fix> findByIssueStatus(IssueStatus status);
    
    /**
     * Find all fixes for an application with a specific status
     * 
     * Used when:
     * - Displaying filtered fixes in UI
     * - Generating application-specific status reports
     * 
     * @param application The application entity
     * @param status The issue status to filter by
     * @return List of fixes matching both criteria
     */
    List<Fix> findByApplicationAndIssueStatus(Application application, IssueStatus status);
    
    /**
     * Count fixes for an application by status
     * 
     * Used when:
     * - Displaying summary statistics in dashboard
     * - Generating metrics
     * 
     * @param application The application entity
     * @param status The issue status to count
     * @return Number of fixes with the specified status
     */
    long countByApplicationAndIssueStatus(Application application, IssueStatus status);
    
    /**
     * Find all pending fixes (status = PENDING)
     * 
     * Convenience method for finding fixes awaiting developer action
     * 
     * @return List of pending fixes
     */
    @Query("SELECT f FROM Fix f WHERE f.issueStatus = 'PENDING' ORDER BY f.createdAt DESC")
    List<Fix> findAllPendingFixes();
    
    /**
     * Find fixes with linked incidents ready for reply
     * 
     * Used by batch job to find fixes that have unreplied mails or jiras
     * 
     * @return List of fixes with unreplied incidents
     */
    @Query("SELECT DISTINCT f FROM Fix f " +
           "LEFT JOIN f.mails m " +
           "LEFT JOIN f.jiras j " +
           "WHERE (m.replied = 'N' OR j.replied = 'N') " +
           "AND f.issueStatus IN ('IN_PROGRESS', 'DB_FIX', 'RESOLVED', 'IGNORED')")
    List<Fix> findFixesWithUnrepliedIncidents();
}

// Made with Bob
