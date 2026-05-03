package com.agent.baki.service.fix;

import com.agent.baki.dto.LogAnalysisResult;
import com.agent.baki.entity.Application;
import com.agent.baki.entity.Fix;
import com.agent.baki.repository.ApplicationRepository;
import com.agent.baki.repository.FixRepository;
import com.agent.baki.service.integration.GithubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for looking up existing fixes by code location
 * 
 * Implements deduplication logic:
 * - Check if a fix already exists for a specific code location (className + lineNumber)
 * - If exists, return existing fix to link new incident
 * - If not exists, return empty to create new fix
 * 
 * This prevents duplicate fixes for the same code issue and allows
 * multiple incidents to be linked to a single fix.
 * 
 * @author Team Baki
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FixLookupService {
    
    private final FixRepository fixRepository;
    private final ApplicationRepository applicationRepository;
    private final GithubService githubService;
    
    /**
     * BobAndMe TODO: Find existing fix by code location
     * 
     * Implementation Requirements:
     * 1. Validate input parameters (applicationName, className, lineNumber)
     * 2. Find or create Application entity by name
     * 3. Query FixRepository for existing fix with same code location
     * 4. Return Optional<Fix> - present if found, empty if not
     * 5. Log lookup results
     * 
     * This is the core deduplication method. Before creating a new fix,
     * always call this method to check if one already exists.
     * 
     * @param applicationName The application name
     * @param className The fully qualified class name
     * @param lineNumber The line number in the code
     * @return Optional containing existing fix if found
     */
    @Transactional(readOnly = true)
    public Optional<Fix> findExistingFix(String applicationName, String className, Integer lineNumber) {
        // Validate inputs
        if (applicationName == null || applicationName.trim().isEmpty()) {
            log.error("Cannot lookup fix: application name is null or empty");
            return Optional.empty();
        }
        
        if (className == null || className.trim().isEmpty()) {
            log.error("Cannot lookup fix: class name is null or empty");
            return Optional.empty();
        }
        
        if (lineNumber == null || lineNumber <= 0) {
            log.error("Cannot lookup fix: line number is null or invalid ({})", lineNumber);
            return Optional.empty();
        }
        
        try {
            log.debug("Looking up fix for: app={}, class={}, line={}", 
                    applicationName, className, lineNumber);
            
            // Find application
            Optional<Application> applicationOpt = applicationRepository.findByApplicationName(applicationName);
            
            if (applicationOpt.isEmpty()) {
                log.debug("Application '{}' not found, no existing fix possible", applicationName);
                return Optional.empty();
            }
            
            Application application = applicationOpt.get();
            
            // Query for existing fix
            Optional<Fix> existingFix = fixRepository.findByApplicationAndCodeClassNameAndCodeLine(
                    application, className, lineNumber
            );
            
            if (existingFix.isPresent()) {
                Fix fix = existingFix.get();
                log.info("Found existing fix: fixId={}, status={}, class={}, line={}", 
                        fix.getFixId(), fix.getIssueStatus(), className, lineNumber);
            } else {
                log.debug("No existing fix found for: app={}, class={}, line={}", 
                        applicationName, className, lineNumber);
            }
            
            return existingFix;
            
        } catch (Exception e) {
            log.error("Error looking up fix for app={}, class={}, line={}: {}", 
                    applicationName, className, lineNumber, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * BobAndMe TODO: Find existing fix using LogAnalysisResult
     * 
     * Implementation Requirements:
     * 1. Validate LogAnalysisResult is valid
     * 2. Extract className and lineNumber from result
     * 3. Call findExistingFix() with extracted data
     * 4. Return Optional<Fix>
     * 
     * Convenience method that accepts LogAnalysisResult directly
     * 
     * @param applicationName The application name
     * @param analysisResult The log analysis result
     * @return Optional containing existing fix if found
     */
    @Transactional(readOnly = true)
    public Optional<Fix> findExistingFixFromAnalysis(String applicationName, LogAnalysisResult analysisResult) {
        if (analysisResult == null) {
            log.error("Cannot lookup fix: analysis result is null");
            return Optional.empty();
        }
        
        if (!analysisResult.isValid()) {
            log.error("Cannot lookup fix: analysis result is invalid");
            return Optional.empty();
        }
        
        return findExistingFix(applicationName, analysisResult.getClassName(), analysisResult.getLineNumber());
    }
    
    /**
     * BobAndMe TODO: Check if a fix exists for given code location
     * 
     * Implementation Requirements:
     * 1. Call findExistingFix()
     * 2. Return true if Optional is present, false otherwise
     * 3. Log check result
     * 
     * Convenience method for boolean check without retrieving the fix
     * 
     * @param applicationName The application name
     * @param className The fully qualified class name
     * @param lineNumber The line number in the code
     * @return true if fix exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean fixExists(String applicationName, String className, Integer lineNumber) {
        Optional<Fix> fix = findExistingFix(applicationName, className, lineNumber);
        boolean exists = fix.isPresent();
        
        log.debug("Fix exists check for app={}, class={}, line={}: {}", 
                applicationName, className, lineNumber, exists);
        
        return exists;
    }
    
    /**
     * BobAndMe TODO: Check if a fix exists using LogAnalysisResult
     * 
     * Implementation Requirements:
     * 1. Validate LogAnalysisResult
     * 2. Call fixExists() with extracted data
     * 3. Return boolean
     * 
     * Convenience method for boolean check with LogAnalysisResult
     * 
     * @param applicationName The application name
     * @param analysisResult The log analysis result
     * @return true if fix exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean fixExistsFromAnalysis(String applicationName, LogAnalysisResult analysisResult) {
        if (analysisResult == null || !analysisResult.isValid()) {
            return false;
        }
        
        return fixExists(applicationName, analysisResult.getClassName(), analysisResult.getLineNumber());
    }
    
    /**
     * BobAndMe TODO: Get or create Application entity
     * 
     * Implementation Requirements:
     * 1. Validate application name
     * 2. Try to find existing application by name
     * 3. If found, return it
     * 4. If not found, create new Application with name
     * 5. Save and return new application
     * 6. Log creation
     * 
     * Helper method used by other services to ensure application exists
     * 
     * @param applicationName The application name
     * @return Application entity (existing or newly created)
     */
    @Transactional
    public Application getOrCreateApplication(String applicationName) {
        if (applicationName == null || applicationName.trim().isEmpty()) {
            throw new IllegalArgumentException("Application name cannot be null or empty");
        }
        
        String trimmedName = applicationName.trim();
        
        // Try to find existing
        Optional<Application> existingApp = applicationRepository.findByApplicationName(trimmedName);
        
        if (existingApp.isPresent()) {
            log.debug("Found existing application: {}", trimmedName);
            return existingApp.get();
        }
        
        // Create new application
        Application newApp = new Application();
        newApp.setApplicationName(trimmedName);
        
        Application savedApp = applicationRepository.save(newApp);
        log.info("Created new application: id={}, name={}", savedApp.getApplicationId(), trimmedName);
        
        return savedApp;
    }
    
    /**
     * BobAndMe TODO: Validate code location parameters
     * 
     * Implementation Requirements:
     * 1. Check className is not null/empty
     * 2. Check lineNumber is not null and > 0
     * 3. Return true if valid, false otherwise
     * 4. Log validation failures
     * 
     * Helper method for input validation
     * 
     * @param className The class name to validate
     * @param lineNumber The line number to validate
     * @return true if both are valid
     */
    public boolean isValidCodeLocation(String className, Integer lineNumber) {
        if (className == null || className.trim().isEmpty()) {
            log.warn("Invalid code location: class name is null or empty");
            return false;
        }
        
        if (lineNumber == null || lineNumber <= 0) {
            log.warn("Invalid code location: line number is null or invalid ({})", lineNumber);
            return false;
        }
        
        return true;
    }
    
    /**
     * BobAndMe TODO: Validate LogAnalysisResult for fix lookup
     * 
     * Implementation Requirements:
     * 1. Check result is not null
     * 2. Check result.isValid()
     * 3. Check result has className and lineNumber
     * 4. Return true if valid, false otherwise
     * 5. Log validation failures
     * 
     * Helper method for validating analysis results
     * 
     * @param analysisResult The analysis result to validate
     * @return true if valid for fix lookup
     */
    public boolean isValidAnalysisResult(LogAnalysisResult analysisResult) {
        if (analysisResult == null) {
            log.warn("Invalid analysis result: result is null");
            return false;
        }
        
        if (!analysisResult.isValid()) {
            log.warn("Invalid analysis result: result.isValid() returned false");
            return false;
        }
        
        if (!isValidCodeLocation(analysisResult.getClassName(), analysisResult.getLineNumber())) {
            log.warn("Invalid analysis result: code location is invalid");
            return false;
        }
        
        return true;
    }
    
    /**
     * Check GitHub repository for recent updates to the code location
     *
     * Checks the remote repository (from application.repositoryLink) on UAT branch
     * for recent commits/PRs that modified the specified className and line number.
     * If updates are found, adds PR links and commit messages to the Fix object.
     *
     * Implementation:
     * 1. Get repository URL from application.repositoryLink
     * 2. Convert className to file path (e.g., com.example.MyClass -> src/main/java/com/example/MyClass.java)
     * 3. Call GithubService to check for recent updates on UAT branch
     * 4. If updates found, update Fix object with PR/commit information
     * 5. Save updated Fix
     *
     * @param fix The Fix object to check and update
     * @return true if updates were found and added, false otherwise
     */
    @Transactional
    public boolean checkAndUpdateGithubInfo(Fix fix) {
        if (fix == null) {
            log.error("Cannot check GitHub: fix is null");
            return false;
        }
        
        if (fix.getApplication() == null) {
            log.error("Cannot check GitHub: fix has no application");
            return false;
        }
        
        try {
            Application app = fix.getApplication();
            String repositoryUrl = app.getRepositoryLink();
            
            if (repositoryUrl == null || repositoryUrl.trim().isEmpty()) {
                log.debug("No repository URL configured for application: {}", app.getApplicationName());
                return false;
            }
            
            String className = fix.getCodeClassName();
            Integer lineNumber = fix.getCodeLine();
            
            if (!isValidCodeLocation(className, lineNumber)) {
                log.error("Invalid code location in fix: class={}, line={}", className, lineNumber);
                return false;
            }
            
            // Convert class name to file path
            // e.g., com.example.MyClass -> src/main/java/com/example/MyClass.java
            String filePath = convertClassNameToFilePath(className);
            
            log.info("Checking GitHub for updates: repo={}, file={}, line={}",
                    repositoryUrl, filePath, lineNumber);
            
            // Check GitHub for recent updates on UAT branch
            String githubInfo = githubService.checkRecentUpdates(
                    repositoryUrl,
                    "UAT",  // Use UAT branch as specified
                    filePath,
                    lineNumber
            );
            
            if (githubInfo != null && !githubInfo.trim().isEmpty()) {
                // Update fix with GitHub information
                String currentReason = fix.getReason();
                String updatedReason;
                
                if (currentReason != null && !currentReason.trim().isEmpty()) {
                    updatedReason = currentReason + "\n\n--- GitHub Updates ---\n" + githubInfo;
                } else {
                    updatedReason = "--- GitHub Updates ---\n" + githubInfo;
                }
                
                fix.setReason(updatedReason);
                fixRepository.save(fix);
                
                log.info("Updated fix {} with GitHub information", fix.getFixId());
                return true;
            } else {
                log.debug("No recent GitHub updates found for fix {}", fix.getFixId());
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error checking GitHub for fix {}: {}", fix.getFixId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Convert Java class name to file path
     *
     * Converts fully qualified class name to repository file path
     * Example: com.example.MyClass -> src/main/java/com/example/MyClass.java
     *
     * @param className Fully qualified class name
     * @return File path in repository
     */
    private String convertClassNameToFilePath(String className) {
        if (className == null || className.trim().isEmpty()) {
            return "";
        }
        
        // Replace dots with slashes and add .java extension
        String path = className.replace('.', '/') + ".java";
        
        // Add standard Maven source directory prefix
        return "src/main/java/" + path;
    }
}

// Made with Bob