package com.agent.baki.entity;

/**
 * Enum representing the status of a code fix issue
 * 
 * Used to track the lifecycle of a fix from identification to resolution
 * 
 * Status Flow:
 * PENDING -> IGNORED (if developer decides to ignore)
 * PENDING -> IN_PROGRESS (when developer starts working on it)
 * PENDING -> DB_FIX (if it's a database-only fix)
 * IN_PROGRESS -> RESOLVED (when fix is completed)
 * DB_FIX -> RESOLVED (when database fix is applied)
 * 
 * @author Team Baki
 */
public enum IssueStatus {
    
    /**
     * Initial status when a fix is first identified
     * Waiting for developer action
     */
    PENDING,
    
    /**
     * Developer has chosen to ignore this issue
     * Reason should be provided in Fix.ignoreReason field
     */
    IGNORED,
    
    /**
     * Developer is actively working on this fix
     * PR number should be provided in Fix.githubPr field
     */
    IN_PROGRESS,
    
    /**
     * Issue requires only database changes (no code changes)
     * Fix number should be provided in Fix.dbFixNumber field
     */
    DB_FIX,
    
    /**
     * Fix has been completed and deployed
     * Final status for a fix
     */
    RESOLVED
}

// Made with Bob
