package com.agent.baki.controller;

import com.agent.baki.entity.Application;
import com.agent.baki.entity.Fix;
import com.agent.baki.entity.IssueStatus;
import com.agent.baki.repository.ApplicationRepository;
import com.agent.baki.repository.FixRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//Todo: delete methods

/**
 * Controller for Application and Fix Management UI
 * 
 * Handles all web requests for the Thymeleaf-based user interface
 * 
 * Endpoints:
 * - GET  /                    : Dashboard - list all applications
 * - GET  /application/{id}    : Application details with fixes
 * - GET  /fix/{id}            : Fix details with incidents
 * - POST /fix/{id}/status     : Update fix status
 * - POST /fix/{id}/ignore     : Mark fix as ignored
 * - POST /fix/{id}/in-progress: Mark fix as in progress with GitHub PR
 * - POST /fix/{id}/db-fix     : Mark fix as DB fix with change number
 * - POST /fix/{id}/resolved   : Mark fix as resolved
 * 
 * @author Team Baki
 */
@RequiredArgsConstructor
@Slf4j
public class ApplicationController {
    
    private final ApplicationRepository applicationRepository;
    private final FixRepository fixRepository;
    
    /**
     * Dashboard - Display all applications with summary statistics
     *
     * Purpose:
     * - Show list of all applications
     * - Display count of fixes by status for each application
     * - Provide navigation to application details
     *
     * Model Attributes:
     * - applications: List<Application> - All applications with their fixes
     *
     * View: dashboard.html
     *
     * Flow:
     * 1. Fetch all applications from database
     * 2. For each application, load associated fixes (eager loading)
     * 3. Add applications to model
     * 4. Return dashboard view
     *
     * @param model Spring MVC model for passing data to view
     * @return View name "dashboard"
     */
    @GetMapping("/")
    public String dashboard(Model model) {
        log.info("Loading dashboard with all applications");
        
        try {
            List<Application> applications = applicationRepository.findAll();
            model.addAttribute("applications", applications);
            
            log.info("Dashboard loaded successfully with {} applications", applications.size());
            return "dashboard";
        } catch (Exception e) {
            log.error("Error loading dashboard", e);
            model.addAttribute("error", "Failed to load applications: " + e.getMessage());
            return "dashboard";
        }
    }
    
    /**
     * Application Details - Display specific application with all its fixes
     *
     * Purpose:
     * - Show application information (name, repository link)
     * - List all fixes for this application
     * - Group fixes by status (PENDING, IN_PROGRESS, RESOLVED, etc.)
     * - Show incident count for each fix
     *
     * Model Attributes:
     * - application: Application - The application entity
     * - pendingFixes: List<Fix> - Fixes with PENDING status
     * - inProgressFixes: List<Fix> - Fixes with IN_PROGRESS status
     * - resolvedFixes: List<Fix> - Fixes with RESOLVED status
     * - ignoredFixes: List<Fix> - Fixes with IGNORED status
     * - dbFixes: List<Fix> - Fixes with DB_FIX status
     *
     * View: application-details.html
     *
     * Flow:
     * 1. Find application by ID (throw 404 if not found)
     * 2. Get all fixes for application
     * 3. Group fixes by status
     * 4. Add data to model
     * 5. Return application details view
     *
     * @param id Application ID from URL path
     * @param model Spring MVC model for passing data to view
     * @return View name "application-details"
     */
    @GetMapping("/application/{id}")
    public String applicationDetails(@PathVariable Long id, Model model) {
        log.info("Loading application details for application ID: {}", id);
        
        try {
            Application application = applicationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Application not found with ID: " + id));
            
            // Get all fixes for this application
            List<Fix> allFixes = fixRepository.findByApplication(application);
            
            // Group fixes by status
            List<Fix> pendingFixes = allFixes.stream()
                    .filter(fix -> fix.getIssueStatus() == IssueStatus.PENDING)
                    .toList();
            
            List<Fix> inProgressFixes = allFixes.stream()
                    .filter(fix -> fix.getIssueStatus() == IssueStatus.IN_PROGRESS)
                    .toList();
            
            List<Fix> resolvedFixes = allFixes.stream()
                    .filter(fix -> fix.getIssueStatus() == IssueStatus.RESOLVED)
                    .toList();
            
            List<Fix> ignoredFixes = allFixes.stream()
                    .filter(fix -> fix.getIssueStatus() == IssueStatus.IGNORED)
                    .toList();
            
            List<Fix> dbFixes = allFixes.stream()
                    .filter(fix -> fix.getIssueStatus() == IssueStatus.DB_FIX)
                    .toList();
            
            // Add to model
            model.addAttribute("application", application);
            model.addAttribute("pendingFixes", pendingFixes);
            model.addAttribute("inProgressFixes", inProgressFixes);
            model.addAttribute("resolvedFixes", resolvedFixes);
            model.addAttribute("ignoredFixes", ignoredFixes);
            model.addAttribute("dbFixes", dbFixes);
            
            log.info("Application details loaded: {} with {} total fixes",
                    application.getApplicationName(), allFixes.size());
            
            return "application-details";
        } catch (Exception e) {
            log.error("Error loading application details for ID: {}", id, e);
            model.addAttribute("error", "Failed to load application: " + e.getMessage());
            return "dashboard";
        }
    }
    
    /**
     * Fix Details - Display specific fix with all linked incidents
     *
     * Purpose:
     * - Show fix information (class, line, summary, status)
     * - List all linked email incidents (Mail entities)
     * - List all linked Jira incidents (Jira entities)
     * - Show GitHub PR if available
     * - Show change number if DB fix
     * - Show reason if ignored
     * - Provide action buttons (Ignore, In Progress, DB Fix, Resolved)
     *
     * Model Attributes:
     * - fix: Fix - The fix entity with all relationships loaded
     * - mails: List<Mail> - Email incidents linked to this fix
     * - jiras: List<Jira> - Jira incidents linked to this fix
     * - canUpdate: boolean - Whether fix can be updated (not RESOLVED)
     *
     * View: fix-details.html
     *
     * Flow:
     * 1. Find fix by ID (throw 404 if not found)
     * 2. Load associated mails and jiras (eager loading)
     * 3. Determine if fix can be updated
     * 4. Add data to model
     * 5. Return fix details view
     *
     * @param id Fix ID from URL path
     * @param model Spring MVC model for passing data to view
     * @return View name "fix-details"
     */
    @GetMapping("/fix/{id}")
    public String fixDetails(@PathVariable Long id, Model model) {
        log.info("Loading fix details for fix ID: {}", id);
        
        try {
            Fix fix = fixRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Fix not found with ID: " + id));
            
            // Determine if fix can be updated (not RESOLVED)
            boolean canUpdate = fix.getIssueStatus() != IssueStatus.RESOLVED;
            
            // Add to model
            model.addAttribute("fix", fix);
            model.addAttribute("mails", fix.getMails());
            model.addAttribute("jiras", fix.getJiras());
            model.addAttribute("canUpdate", canUpdate);
            
            log.info("Fix details loaded: {} with {} mails and {} jiras",
                    fix.getIssueSummary(), fix.getMails().size(), fix.getJiras().size());
            
            return "fix-details";
        } catch (Exception e) {
            log.error("Error loading fix details for ID: {}", id, e);
            model.addAttribute("error", "Failed to load fix: " + e.getMessage());
            return "dashboard";
        }
    }
    
    /**
     * Mark Fix as Ignored - Handle "Ignore" button action
     *
     * Purpose:
     * - Update fix status to IGNORED
     * - Save reason for ignoring
     * - Redirect back to fix details page
     *
     * Form Parameters:
     * - reason: String (required) - Reason for ignoring the fix
     *
     * Flow:
     * 1. Find fix by ID
     * 2. Validate fix can be updated (not RESOLVED)
     * 3. Update status to IGNORED
     * 4. Set reason field
     * 5. Save fix
     * 6. Log action
     * 7. Redirect to fix details page with success message
     *
     * Success: Redirect to /fix/{id} with success message
     * Error: Redirect to /fix/{id} with error message
     *
     * @param id Fix ID from URL path
     * @param reason Reason for ignoring (from form)
     * @return Redirect to fix details page
     */
    @PostMapping("/fix/{id}/ignore")
    public String markFixAsIgnored(@PathVariable Long id, @RequestParam String reason) {
        log.info("Marking fix {} as IGNORED with reason: {}", id, reason);
        
        try {
            Fix fix = fixRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Fix not found with ID: " + id));
            
            if (fix.getIssueStatus() == IssueStatus.RESOLVED) {
                log.warn("Cannot update fix {} - already RESOLVED", id);
                return "redirect:/fix/" + id + "?error=Cannot update resolved fix";
            }
            
            fix.setIssueStatus(IssueStatus.IGNORED);
            fix.setReason(reason);
            fixRepository.save(fix);
            
            log.info("Fix {} marked as IGNORED successfully", id);
            return "redirect:/fix/" + id + "?success=Fix marked as ignored";
        } catch (Exception e) {
            log.error("Error marking fix {} as ignored", id, e);
            return "redirect:/fix/" + id + "?error=" + e.getMessage();
        }
    }
    
    /**
     * Mark Fix as In Progress - Handle "Issue in Progress" button action
     *
     * Purpose:
     * - Update fix status to IN_PROGRESS
     * - Save GitHub PR link
     * - Redirect back to fix details page
     *
     * Form Parameters:
     * - githubPr: String (required) - GitHub PR number or URL
     *
     * Flow:
     * 1. Find fix by ID
     * 2. Validate fix can be updated
     * 3. Update status to IN_PROGRESS
     * 4. Set githubPr field
     * 5. Save fix
     * 6. Log action
     * 7. Redirect to fix details page with success message
     *
     * Success: Redirect to /fix/{id} with success message
     * Error: Redirect to /fix/{id} with error message
     *
     * @param id Fix ID from URL path
     * @param githubPr GitHub PR link (from form)
     * @return Redirect to fix details page
     */
    @PostMapping("/fix/{id}/in-progress")
    public String markFixAsInProgress(@PathVariable Long id, @RequestParam String githubPr) {
        log.info("Marking fix {} as IN_PROGRESS with GitHub PR: {}", id, githubPr);
        
        try {
            Fix fix = fixRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Fix not found with ID: " + id));
            
            if (fix.getIssueStatus() == IssueStatus.RESOLVED) {
                log.warn("Cannot update fix {} - already RESOLVED", id);
                return "redirect:/fix/" + id + "?error=Cannot update resolved fix";
            }
            
            fix.setIssueStatus(IssueStatus.IN_PROGRESS);
            fix.setGithubPr(githubPr);
            fixRepository.save(fix);
            
            log.info("Fix {} marked as IN_PROGRESS successfully", id);
            return "redirect:/fix/" + id + "?success=Fix marked as in progress";
        } catch (Exception e) {
            log.error("Error marking fix {} as in progress", id, e);
            return "redirect:/fix/" + id + "?error=" + e.getMessage();
        }
    }
    
    /**
     * Mark Fix as DB Fix - Handle "DB Fix" button action
     *
     * Purpose:
     * - Update fix status to DB_FIX
     * - Save change number (migration script reference)
     * - Optionally save reason/description
     * - Redirect back to fix details page
     *
     * Form Parameters:
     * - changeNumber: String (required) - Change number or migration script name
     * - reason: String (optional) - Additional description
     *
     * Flow:
     * 1. Find fix by ID
     * 2. Validate fix can be updated
     * 3. Update status to DB_FIX
     * 4. Set changeNumber field
     * 5. Set reason field if provided
     * 6. Save fix
     * 7. Log action
     * 8. Redirect to fix details page with success message
     *
     * Success: Redirect to /fix/{id} with success message
     * Error: Redirect to /fix/{id} with error message
     *
     * @param id Fix ID from URL path
     * @param changeNumber Change number (from form)
     * @param reason Optional reason (from form)
     * @return Redirect to fix details page
     */
    @PostMapping("/fix/{id}/db-fix")
    public String markFixAsDBFix(@PathVariable Long id,
                                  @RequestParam String changeNumber,
                                  @RequestParam(required = false) String reason) {
        log.info("Marking fix {} as DB_FIX with change number: {}", id, changeNumber);
        
        try {
            Fix fix = fixRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Fix not found with ID: " + id));
            
            if (fix.getIssueStatus() == IssueStatus.RESOLVED) {
                log.warn("Cannot update fix {} - already RESOLVED", id);
                return "redirect:/fix/" + id + "?error=Cannot update resolved fix";
            }
            
            fix.setIssueStatus(IssueStatus.DB_FIX);
            fix.setChangeNumber(changeNumber);
            if (reason != null && !reason.trim().isEmpty()) {
                fix.setReason(reason);
            }
            fixRepository.save(fix);
            
            log.info("Fix {} marked as DB_FIX successfully", id);
            return "redirect:/fix/" + id + "?success=Fix marked as DB fix";
        } catch (Exception e) {
            log.error("Error marking fix {} as DB fix", id, e);
            return "redirect:/fix/" + id + "?error=" + e.getMessage();
        }
    }
    
    /**
     * Mark Fix as Resolved - Handle "Resolved" button action
     *
     * Purpose:
     * - Update fix status to RESOLVED
     * - Redirect back to fix details page
     *
     * Flow:
     * 1. Find fix by ID
     * 2. Validate fix can be updated
     * 3. Update status to RESOLVED
     * 4. Save fix
     * 5. Log action
     * 6. Redirect to fix details page with success message
     *
     * Success: Redirect to /fix/{id} with success message
     * Error: Redirect to /fix/{id} with error message
     *
     * @param id Fix ID from URL path
     * @return Redirect to fix details page
     */
    @PostMapping("/fix/{id}/resolved")
    public String markFixAsResolved(@PathVariable Long id) {
        log.info("Marking fix {} as RESOLVED", id);
        
        try {
            Fix fix = fixRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Fix not found with ID: " + id));
            
            fix.setIssueStatus(IssueStatus.RESOLVED);
            fixRepository.save(fix);
            
            log.info("Fix {} marked as RESOLVED successfully", id);
            return "redirect:/fix/" + id + "?success=Fix marked as resolved";
        } catch (Exception e) {
            log.error("Error marking fix {} as resolved", id, e);
            return "redirect:/fix/" + id + "?error=" + e.getMessage();
        }
    }
    
    /**
     * Update Fix Status - Generic status update endpoint
     *
     * Purpose:
     * - Allow direct status updates via dropdown or other UI elements
     * - Update fix status to any valid IssueStatus value
     * - Redirect back to referring page
     *
     * Form Parameters:
     * - status: String (required) - New status (PENDING, IN_PROGRESS, RESOLVED, IGNORED, DB_FIX)
     *
     * Flow:
     * 1. Find fix by ID
     * 2. Validate status value
     * 3. Update fix status
     * 4. Save fix
     * 5. Log action
     * 6. Redirect to referring page or fix details
     *
     * Success: Redirect with success message
     * Error: Redirect with error message
     *
     * @param id Fix ID from URL path
     * @param status New status value (from form)
     * @return Redirect to referring page
     */
    @PostMapping("/fix/{id}/status")
    public String updateFixStatus(@PathVariable Long id, @RequestParam String status) {
        log.info("Updating fix {} status to: {}", id, status);
        
        try {
            Fix fix = fixRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Fix not found with ID: " + id));
            
            // Validate and convert status string to enum
            IssueStatus newStatus;
            try {
                newStatus = IssueStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("Invalid status value: {}", status);
                return "redirect:/fix/" + id + "?error=Invalid status value";
            }
            
            fix.setIssueStatus(newStatus);
            fixRepository.save(fix);
            
            log.info("Fix {} status updated to {} successfully", id, newStatus);
            return "redirect:/fix/" + id + "?success=Status updated successfully";
        } catch (Exception e) {
            log.error("Error updating fix {} status", id, e);
            return "redirect:/fix/" + id + "?error=" + e.getMessage();
        }
    }
    
    /**
     * Delete Application - Handle application deletion
     *
     * Purpose:
     * - Delete application and all associated data (fixes, mails, jiras)
     * - Cascade delete due to entity relationships
     * - Redirect to dashboard
     *
     * Flow:
     * 1. Find application by ID
     * 2. Check if application exists
     * 3. Delete application (cascade deletes fixes, mails, jiras)
     * 4. Log deletion
     * 5. Redirect to dashboard with success message
     *
     * Warning: This is a destructive operation that cannot be undone
     *
     * Success: Redirect to / with success message
     * Error: Redirect to /application/{id} with error message
     *
     * @param id Application ID from URL path
     * @return Redirect to dashboard
     */
    @PostMapping("/application/{id}/delete")
    public String deleteApplication(@PathVariable Long id) {
        log.warn("Deleting application with ID: {}", id);
        
        try {
            Application application = applicationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Application not found with ID: " + id));
            
            String appName = application.getApplicationName();
            applicationRepository.delete(application);
            
            log.info("Application '{}' (ID: {}) deleted successfully with all associated data", appName, id);
            return "redirect:/?success=Application deleted successfully";
        } catch (Exception e) {
            log.error("Error deleting application with ID: {}", id, e);
            return "redirect:/application/" + id + "?error=" + e.getMessage();
        }
    }
    
    /**
     * Delete Fix - Handle fix deletion
     *
     * Purpose:
     * - Delete fix and unlink from incidents
     * - Set fix_id to NULL in mail and jira tables (orphan removal)
     * - Redirect to application details
     *
     * Flow:
     * 1. Find fix by ID
     * 2. Get application ID for redirect
     * 3. Unlink all mails and jiras (set fix_id to NULL)
     * 4. Delete fix
     * 5. Log deletion
     * 6. Redirect to application details with success message
     *
     * Warning: This is a destructive operation that cannot be undone
     * Incidents will remain but will need to be re-analyzed
     *
     * Success: Redirect to /application/{appId} with success message
     * Error: Redirect to /fix/{id} with error message
     *
     * @param id Fix ID from URL path
     * @return Redirect to application details
     */
    @PostMapping("/fix/{id}/delete")
    public String deleteFix(@PathVariable Long id) {
        log.warn("Deleting fix with ID: {}", id);
        
        try {
            Fix fix = fixRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Fix not found with ID: " + id));
            
            Long applicationId = fix.getApplication().getApplicationId();
            String fixSummary = fix.getIssueSummary();
            
            // Unlink all mails and jiras (set fix to null)
            fix.getMails().forEach(mail -> mail.setFix(null));
            fix.getJiras().forEach(jira -> jira.setFix(null));
            
            // Delete the fix
            fixRepository.delete(fix);
            
            log.info("Fix '{}' (ID: {}) deleted successfully. Incidents unlinked.", fixSummary, id);
            return "redirect:/application/" + applicationId + "?success=Fix deleted successfully";
        } catch (Exception e) {
            log.error("Error deleting fix with ID: {}", id, e);
            return "redirect:/fix/" + id + "?error=" + e.getMessage();
        }
    }
}

// Made with Bob