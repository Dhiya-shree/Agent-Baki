package com.agent.baki.service.fix;

import com.agent.baki.dto.LogAnalysisResult;
import com.agent.baki.entity.Application;
import com.agent.baki.entity.Fix;
import com.agent.baki.entity.IssueStatus;
import com.agent.baki.repository.FixRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing Fix entities
 * 
 * Handles:
 * - Creating new fixes from log analysis results
 * - Updating existing fix status and details
 * - Finding or creating fixes (deduplication logic)
 * - Managing fix lifecycle (PENDING -> IN_PROGRESS -> RESOLVED)
 * 
 * Works with FixLookupService for deduplication:
 * - First checks if fix exists for code location
 * - If exists, returns existing fix
 * - If not, creates new fix
 * 
 * @author Team Baki
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FixManagementService {
    
    private final FixRepository fixRepository;
    private final FixLookupService fixLookupService;
    
    /**
     * BobAndMe TODO: Find existing fix or create new one
     * 
     * Implementation Requirements:
     * 1. Validate inputs (applicationName, analysisResult)
     * 2. Get or create Application entity
     * 3. Check if fix exists using FixLookupService
     * 4. If exists, return existing fix
     * 5. If not exists, create new fix with analysis data
     * 6. Save and return new fix
     * 7. Log creation/reuse
     * 
     * This is the main method for fix management. It implements deduplication
     * by checking for existing fixes before creating new ones.
     * 
     * @param applicationName The application name
     * @param analysisResult The log analysis result with code location
     * @return Fix entity (existing or newly created)
     */
    @Transactional
    public Fix findOrCreateFix(String applicationName, LogAnalysisResult analysisResult) {
        // Validate inputs
        if (applicationName == null || applicationName.trim().isEmpty()) {
            throw new IllegalArgumentException("Application name cannot be null or empty");
        }
        
        if (!fixLookupService.isValidAnalysisResult(analysisResult)) {
            throw new IllegalArgumentException("Invalid analysis result for fix creation");
        }
        
        try {
            log.info("Finding or creating fix for: app={}, class={}, line={}", 
                    applicationName, analysisResult.getClassName(), analysisResult.getLineNumber());
            
            // Get or create application
            Application application = fixLookupService.getOrCreateApplication(applicationName);
            
            // Check for existing fix
            Optional<Fix> existingFix = fixLookupService.findExistingFixFromAnalysis(
                    applicationName, analysisResult
            );
            
            if (existingFix.isPresent()) {
                Fix fix = existingFix.get();
                log.info("Reusing existing fix: fixId={}, status={}, incidents={}", 
                        fix.getFixId(), fix.getIssueStatus(), 
                        fix.getMails().size() + fix.getJiras().size());
                return fix;
            }
            
            // Create new fix
            Fix newFix = createFixFromAnalysis(application, analysisResult);
            Fix savedFix = fixRepository.save(newFix);
            
            log.info("Created new fix: fixId={}, class={}, line={}", 
                    savedFix.getFixId(), savedFix.getCodeClassName(), savedFix.getCodeLine());
            
            return savedFix;
            
        } catch (Exception e) {
            log.error("Error finding or creating fix for app={}, class={}, line={}: {}", 
                    applicationName, analysisResult.getClassName(), 
                    analysisResult.getLineNumber(), e.getMessage(), e);
            throw new RuntimeException("Failed to find or create fix", e);
        }
    }
    
    /**
     * BobAndMe TODO: Create a new Fix entity from LogAnalysisResult
     * 
     * Implementation Requirements:
     * 1. Create new Fix instance
     * 2. Set application reference
     * 3. Set codeClassName from analysisResult
     * 4. Set codeLine from analysisResult
     * 5. Set issueSummary from errorDescription (truncate to 250 chars)
     * 6. Set issueStatus to PENDING
     * 7. Return the fix (not saved yet)
     * 
     * Helper method to build Fix entity from analysis result
     * 
     * @param application The application entity
     * @param analysisResult The log analysis result
     * @return New Fix entity (not persisted)
     */
    private Fix createFixFromAnalysis(Application application, LogAnalysisResult analysisResult) {
        Fix fix = new Fix();
        fix.setApplication(application);
        fix.setCodeClassName(analysisResult.getClassName());
        fix.setCodeLine(analysisResult.getLineNumber());
        
        // Set issue summary from error description (truncate if needed)
        String errorDesc = analysisResult.getErrorDescription();
        if (errorDesc != null && !errorDesc.isEmpty()) {
            String summary = errorDesc.length() > 250 
                    ? errorDesc.substring(0, 247) + "..." 
                    : errorDesc;
            fix.setIssueSummary(summary);
        } else {
            fix.setIssueSummary("Error at " + analysisResult.getClassName() + 
                    ":" + analysisResult.getLineNumber());
        }
        
        fix.setIssueStatus(IssueStatus.PENDING);
        
        log.debug("Created fix entity: class={}, line={}, summary={}", 
                fix.getCodeClassName(), fix.getCodeLine(), fix.getIssueSummary());
        
        return fix;
    }
    
    /**
     * BobAndMe TODO: Update fix status
     * 
     * Implementation Requirements:
     * 1. Validate fixId and status
     * 2. Find fix by ID
     * 3. Update issueStatus
     * 4. Save fix
     * 5. Log status change
     * 6. Return updated fix
     * 
     * Used when developer changes fix status in UI
     * 
     * @param fixId The fix ID
     * @param newStatus The new status
     * @return Updated fix
     */
    @Transactional
    public Fix updateFixStatus(Long fixId, IssueStatus newStatus) {
        if (fixId == null) {
            throw new IllegalArgumentException("Fix ID cannot be null");
        }
        
        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        
        Fix fix = fixRepository.findById(fixId)
                .orElseThrow(() -> new IllegalArgumentException("Fix not found: " + fixId));
        
        IssueStatus oldStatus = fix.getIssueStatus();
        fix.setIssueStatus(newStatus);
        
        Fix updatedFix = fixRepository.save(fix);
        
        log.info("Updated fix status: fixId={}, {} -> {}", 
                fixId, oldStatus, newStatus);
        
        return updatedFix;
    }
    
    /**
     * BobAndMe TODO: Update fix with GitHub PR
     * 
     * Implementation Requirements:
     * 1. Validate fixId and githubPr
     * 2. Find fix by ID
     * 3. Update githubPr field
     * 4. Update status to IN_PROGRESS if currently PENDING
     * 5. Save fix
     * 6. Log update
     * 7. Return updated fix
     * 
     * Used when developer marks issue as IN_PROGRESS and provides PR link
     * 
     * @param fixId The fix ID
     * @param githubPr The GitHub PR number or URL
     * @return Updated fix
     */
    @Transactional
    public Fix updateFixWithGitHubPR(Long fixId, String githubPr) {
        if (fixId == null) {
            throw new IllegalArgumentException("Fix ID cannot be null");
        }
        
        if (githubPr == null || githubPr.trim().isEmpty()) {
            throw new IllegalArgumentException("GitHub PR cannot be null or empty");
        }
        
        Fix fix = fixRepository.findById(fixId)
                .orElseThrow(() -> new IllegalArgumentException("Fix not found: " + fixId));
        
        fix.setGithubPr(githubPr.trim());
        
        // Auto-update status to IN_PROGRESS if currently PENDING
        if (fix.getIssueStatus() == IssueStatus.PENDING) {
            fix.setIssueStatus(IssueStatus.IN_PROGRESS);
            log.info("Auto-updated status to IN_PROGRESS for fix: {}", fixId);
        }
        
        Fix updatedFix = fixRepository.save(fix);
        
        log.info("Updated fix with GitHub PR: fixId={}, pr={}, status={}", 
                fixId, githubPr, updatedFix.getIssueStatus());
        
        return updatedFix;
    }
    
    /**
     * BobAndMe TODO: Mark fix as ignored with reason
     * 
     * Implementation Requirements:
     * 1. Validate fixId and reason
     * 2. Find fix by ID
     * 3. Update status to IGNORED
     * 4. Set reason field
     * 5. Save fix
     * 6. Log action
     * 7. Return updated fix
     * 
     * Used when developer marks issue as IGNORED in UI
     * 
     * @param fixId The fix ID
     * @param reason The reason for ignoring
     * @return Updated fix
     */
    @Transactional
    public Fix markFixAsIgnored(Long fixId, String reason) {
        if (fixId == null) {
            throw new IllegalArgumentException("Fix ID cannot be null");
        }
        
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be null or empty");
        }
        
        Fix fix = fixRepository.findById(fixId)
                .orElseThrow(() -> new IllegalArgumentException("Fix not found: " + fixId));
        
        fix.setIssueStatus(IssueStatus.IGNORED);
        fix.setReason(reason.trim());
        
        Fix updatedFix = fixRepository.save(fix);
        
        log.info("Marked fix as ignored: fixId={}, reason={}", fixId, reason);
        
        return updatedFix;
    }
    
    /**
     * BobAndMe TODO: Mark fix as DB fix with change number
     * 
     * Implementation Requirements:
     * 1. Validate fixId and changeNumber
     * 2. Find fix by ID
     * 3. Update status to DB_FIX
     * 4. Set changeNumber field
     * 5. Optionally set reason
     * 6. Save fix
     * 7. Log action
     * 8. Return updated fix
     * 
     * Used when developer marks issue as DB_FIX in UI
     * 
     * @param fixId The fix ID
     * @param changeNumber The change number or migration script reference
     * @param reason Optional reason/description
     * @return Updated fix
     */
    @Transactional
    public Fix markFixAsDBFix(Long fixId, String changeNumber, String reason) {
        if (fixId == null) {
            throw new IllegalArgumentException("Fix ID cannot be null");
        }
        
        if (changeNumber == null || changeNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Change number cannot be null or empty");
        }
        
        Fix fix = fixRepository.findById(fixId)
                .orElseThrow(() -> new IllegalArgumentException("Fix not found: " + fixId));
        
        fix.setIssueStatus(IssueStatus.DB_FIX);
        fix.setChangeNumber(changeNumber.trim());
        
        if (reason != null && !reason.trim().isEmpty()) {
            fix.setReason(reason.trim());
        }
        
        Fix updatedFix = fixRepository.save(fix);
        
        log.info("Marked fix as DB fix: fixId={}, changeNumber={}", fixId, changeNumber);
        
        return updatedFix;
    }
    
    /**
     * BobAndMe TODO: Mark fix as resolved
     * 
     * Implementation Requirements:
     * 1. Validate fixId
     * 2. Find fix by ID
     * 3. Update status to RESOLVED
     * 4. Save fix
     * 5. Log action
     * 6. Return updated fix
     * 
     * Used when developer marks issue as RESOLVED in UI
     * 
     * @param fixId The fix ID
     * @return Updated fix
     */
    @Transactional
    public Fix markFixAsResolved(Long fixId) {
        if (fixId == null) {
            throw new IllegalArgumentException("Fix ID cannot be null");
        }
        
        Fix fix = fixRepository.findById(fixId)
                .orElseThrow(() -> new IllegalArgumentException("Fix not found: " + fixId));
        
        fix.setIssueStatus(IssueStatus.RESOLVED);
        
        Fix updatedFix = fixRepository.save(fix);
        
        log.info("Marked fix as resolved: fixId={}", fixId);
        
        return updatedFix;
    }
    
    /**
     * BobAndMe TODO: Get fix by ID
     * 
     * Implementation Requirements:
     * 1. Validate fixId
     * 2. Find fix by ID
     * 3. Return Optional<Fix>
     * 
     * Helper method to retrieve fix by ID
     * 
     * @param fixId The fix ID
     * @return Optional containing fix if found
     */
    @Transactional(readOnly = true)
    public Optional<Fix> getFixById(Long fixId) {
        if (fixId == null) {
            log.warn("Cannot get fix: ID is null");
            return Optional.empty();
        }
        
        return fixRepository.findById(fixId);
    }
    
    /**
     * BobAndMe TODO: Check if fix can be updated
     * 
     * Implementation Requirements:
     * 1. Check if fix status is not RESOLVED
     * 2. Return true if can be updated, false otherwise
     * 
     * Helper method to check if fix is in a state that allows updates
     * 
     * @param fix The fix to check
     * @return true if fix can be updated
     */
    public boolean canUpdateFix(Fix fix) {
        if (fix == null) {
            return false;
        }
        
        // Can update if not resolved
        return fix.getIssueStatus() != IssueStatus.RESOLVED;
    }
}

// Made with Bob