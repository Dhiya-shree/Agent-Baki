package com.agent.baki.service.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Service for file system operations
 * 
 * Handles:
 * - Creating folder structure for log storage
 * - Saving log files to file system
 * - Reading log files from file system
 * - Deleting log files after processing
 * 
 * Storage Path Format: C:/Users/Baki/{application_name}/issues/{source_id}/logs/
 * 
 * @author Team Baki
 */
@Service
@Slf4j
public class FileStorageService {
    
    /**
     * Base directory for log storage
     * Format: C:/Users/Baki/
     */
    private static final String BASE_DIRECTORY = "C:/Users/Baki/";
    
    /**
     * Initialize base directory on application startup
     * Creates C:/Users/Baki/ if it doesn't exist
     */
    @PostConstruct
    public void initializeBaseDirectory() {
        createBaseDirectory();
    }
    
    /**
     * Create base directory C:/Users/Baki/
     * 
     * Static method to ensure base directory exists
     * Called on service initialization
     * 
     * @return true if directory exists or was created successfully
     */
    public static boolean createBaseDirectory() {
        try {
            Path basePath = Paths.get(BASE_DIRECTORY);
            
            if (Files.exists(basePath)) {
                log.info("Base directory already exists: {}", BASE_DIRECTORY);
                return true;
            }
            
            Files.createDirectories(basePath);
            log.info("Created base directory: {}", BASE_DIRECTORY);
            return true;
            
        } catch (IOException e) {
            log.error("Failed to create base directory {}: {}", BASE_DIRECTORY, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Create folder structure for storing logs
     * 
     * Path format: C:/Users/Baki/{applicationName}/issues/{sourceId}/logs/
     * Creates all parent directories if they don't exist
     * 
     * @param applicationName The application name
     * @param sourceId The incident source ID (email ID or Jira key)
     * @return Path to the logs directory, or null if creation failed
     */
    public Path createLogDirectory(String applicationName, String sourceId) {
        try {
            if (applicationName == null || applicationName.trim().isEmpty()) {
                log.error("Cannot create log directory: application name is null or empty");
                return null;
            }
            
            if (sourceId == null || sourceId.trim().isEmpty()) {
                log.error("Cannot create log directory: source ID is null or empty");
                return null;
            }
            
            // Sanitize names to remove invalid path characters
            String sanitizedAppName = sanitizePathComponent(applicationName);
            String sanitizedSourceId = sanitizePathComponent(sourceId);
            
            // Build path: C:/Users/Baki/{applicationName}/issues/{sourceId}/logs/
            Path logPath = Paths.get(BASE_DIRECTORY, sanitizedAppName, "issues", sanitizedSourceId, "logs");
            
            // Create all directories
            Files.createDirectories(logPath);
            
            log.info("Created log directory: {}", logPath);
            return logPath;
            
        } catch (IOException e) {
            log.error("Failed to create log directory for app={}, sourceId={}: {}", 
                     applicationName, sourceId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Save log files to file system
     * 
     * Saves all log files to: C:/Users/Baki/{applicationName}/issues/{sourceId}/logs/
     * Creates directory if it doesn't exist
     * 
     * @param applicationName The application name
     * @param sourceId The incident source ID
     * @param logFiles Map of filename to content bytes
     * @return Number of files successfully saved
     */
    public int saveLogFiles(String applicationName, String sourceId, Map<String, byte[]> logFiles) {
        if (logFiles == null || logFiles.isEmpty()) {
            log.warn("No log files to save for app={}, sourceId={}", applicationName, sourceId);
            return 0;
        }
        
        // Create log directory
        Path logDirectory = createLogDirectory(applicationName, sourceId);
        if (logDirectory == null) {
            log.error("Cannot save log files: failed to create directory");
            return 0;
        }
        
        int savedCount = 0;
        
        // Save each log file
        for (Map.Entry<String, byte[]> entry : logFiles.entrySet()) {
            String filename = entry.getKey();
            byte[] content = entry.getValue();
            
            try {
                Path filePath = logDirectory.resolve(filename);
                Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                
                log.info("Saved log file: {} ({} bytes)", filePath, content.length);
                savedCount++;
                
            } catch (IOException e) {
                log.error("Failed to save log file {}: {}", filename, e.getMessage(), e);
            }
        }
        
        log.info("Saved {}/{} log files for app={}, sourceId={}", 
                savedCount, logFiles.size(), applicationName, sourceId);
        return savedCount;
    }
    
    /**
     * Read all log files from directory
     * 
     * Reads all files from: C:/Users/Baki/{applicationName}/issues/{sourceId}/logs/
     * 
     * @param applicationName The application name
     * @param sourceId The incident source ID
     * @return Map of filename to content bytes (empty if directory doesn't exist)
     */
    public Map<String, byte[]> readLogFiles(String applicationName, String sourceId) {
        Map<String, byte[]> logFiles = new HashMap<>();
        
        Path logDirectory = getLogDirectoryPath(applicationName, sourceId);
        
        if (!Files.exists(logDirectory)) {
            log.warn("Log directory does not exist: {}", logDirectory);
            return logFiles;
        }
        
        try (Stream<Path> files = Files.list(logDirectory)) {
            files.filter(Files::isRegularFile)
                 .forEach(file -> {
                     try {
                         String filename = file.getFileName().toString();
                         byte[] content = Files.readAllBytes(file);
                         logFiles.put(filename, content);
                         log.debug("Read log file: {} ({} bytes)", filename, content.length);
                     } catch (IOException e) {
                         log.error("Failed to read log file {}: {}", file, e.getMessage());
                     }
                 });
            
            log.info("Read {} log files from {}", logFiles.size(), logDirectory);
            
        } catch (IOException e) {
            log.error("Failed to list log files in {}: {}", logDirectory, e.getMessage(), e);
        }
        
        return logFiles;
    }
    
    /**
     * Delete log directory and all files
     * 
     * Deletes: C:/Users/Baki/{applicationName}/issues/{sourceId}/logs/
     * Called after successful reply to clean up storage
     * 
     * @param applicationName The application name
     * @param sourceId The incident source ID
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteLogDirectory(String applicationName, String sourceId) {
        Path logDirectory = getLogDirectoryPath(applicationName, sourceId);
        
        if (!Files.exists(logDirectory)) {
            log.info("Log directory does not exist, nothing to delete: {}", logDirectory);
            return true;
        }
        
        try {
            // Delete all files and subdirectories recursively
            try (Stream<Path> paths = Files.walk(logDirectory)) {
                paths.sorted(Comparator.reverseOrder())
                     .forEach(path -> {
                         try {
                             Files.delete(path);
                             log.debug("Deleted: {}", path);
                         } catch (IOException e) {
                             log.error("Failed to delete {}: {}", path, e.getMessage());
                         }
                     });
            }
            
            log.info("Deleted log directory: {}", logDirectory);
            return true;
            
        } catch (IOException e) {
            log.error("Failed to delete log directory {}: {}", logDirectory, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get the full path to logs directory
     * 
     * Path format: C:/Users/Baki/{applicationName}/issues/{sourceId}/logs/
     * 
     * @param applicationName The application name
     * @param sourceId The incident source ID
     * @return Path to the logs directory
     */
    public Path getLogDirectoryPath(String applicationName, String sourceId) {
        String sanitizedAppName = sanitizePathComponent(applicationName);
        String sanitizedSourceId = sanitizePathComponent(sourceId);
        
        return Paths.get(BASE_DIRECTORY, sanitizedAppName, "issues", sanitizedSourceId, "logs");
    }
    
    /**
     * Check if log directory exists
     * 
     * @param applicationName The application name
     * @param sourceId The incident source ID
     * @return true if directory exists, false otherwise
     */
    public boolean logDirectoryExists(String applicationName, String sourceId) {
        Path logDirectory = getLogDirectoryPath(applicationName, sourceId);
        return Files.exists(logDirectory);
    }
    
    /**
     * Get total size of all log files in directory
     * 
     * @param applicationName The application name
     * @param sourceId The incident source ID
     * @return Total size in bytes (0 if directory doesn't exist)
     */
    public long getLogDirectorySize(String applicationName, String sourceId) {
        Path logDirectory = getLogDirectoryPath(applicationName, sourceId);
        
        if (!Files.exists(logDirectory)) {
            return 0;
        }
        
        try (Stream<Path> files = Files.walk(logDirectory)) {
            return files.filter(Files::isRegularFile)
                       .mapToLong(file -> {
                           try {
                               return Files.size(file);
                           } catch (IOException e) {
                               log.warn("Failed to get size of {}: {}", file, e.getMessage());
                               return 0;
                           }
                       })
                       .sum();
        } catch (IOException e) {
            log.error("Failed to calculate directory size for {}: {}", logDirectory, e.getMessage());
            return 0;
        }
    }
    
    /**
     * Sanitize path component to remove invalid characters
     * 
     * Removes or replaces characters that are invalid in file paths:
     * - Replaces spaces with underscores
     * - Removes: < > : " / \ | ? *
     * 
     * @param component The path component to sanitize
     * @return Sanitized path component
     */
    private String sanitizePathComponent(String component) {
        if (component == null) {
            return "unknown";
        }
        
        return component.trim()
                       .replaceAll("[<>:\"/\\\\|?*]", "")
                       .replaceAll("\\s+", "_");
    }
}

// Made with Bob