package com.agent.baki.service.analysis;

import com.agent.baki.dto.IncidentDTO;
import com.agent.baki.dto.LogAnalysisResult;
import com.agent.baki.service.ai.WatsonxAIService;
import com.agent.baki.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for analyzing logs and identifying problematic code
 * 
 * Handles:
 * - Reading log files from file system
 * - Extracting relevant error logs from large files
 * - Chunking logs to avoid sending huge data to AI
 * - Sending logs to Watsonx AI for analysis
 * - Parsing AI response to extract code location
 * - Building LogAnalysisResult
 * 
 * @author Team Baki
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogAnalysisService {
    
    private final WatsonxAIService watsonxAIService;
    private final FileStorageService fileStorageService;
    
    // Maximum characters to send to AI (to avoid token limits)
    private static final int MAX_LOG_SIZE = 50000; // ~50KB
    
    // Regex patterns for parsing AI response
    private static final Pattern CLASS_PATTERN = Pattern.compile("CLASS:\\s*(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINE_PATTERN = Pattern.compile("LINE:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ERROR_PATTERN = Pattern.compile("ERROR:\\s*(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE);
    
    // Java error patterns to identify error sections
    private static final List<Pattern> ERROR_INDICATORS = Arrays.asList(
            Pattern.compile("Exception in thread", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\w+Exception:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\w+Error:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Caused by:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("at\\s+[\\w.$]+\\([\\w.]+:\\d+\\)"), // Stack trace line
            Pattern.compile("ERROR", Pattern.CASE_INSENSITIVE),
            Pattern.compile("SEVERE", Pattern.CASE_INSENSITIVE),
            Pattern.compile("FATAL", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * BobAndMe TODO: Analyze logs for an incident
     * 
     * Implementation Requirements:
     * 1. Check if incident has application name and log files
     * 2. Read log files from file system using fileStorageService
     * 3. Combine all log files into single string
     * 4. Extract error logs using extractErrorLogs() to reduce size
     * 5. Call watsonxAIService.analyzeLog() with extracted logs
     * 6. Parse AI response using parseAIResponse()
     * 7. Return LogAnalysisResult
     * 8. Handle errors and return failed result
     * 
     * @param incident The incident with log files
     * @return LogAnalysisResult with code location
     */
    public LogAnalysisResult analyzeIncidentLogs(IncidentDTO incident) {
        if (incident == null) {
            log.error("Cannot analyze logs: incident is null");
            return buildFailedResult("Incident is null");
        }
        
        if (!incident.isHasApplicationName()) {
            log.error("Cannot analyze logs: application name is missing");
            return buildFailedResult("Application name is missing");
        }
        
        if (!incident.isHasLogFiles()) {
            log.error("Cannot analyze logs: log files are missing");
            return buildFailedResult("Log files are missing");
        }
        
        try {
            log.info("Analyzing logs for incident: {}", incident.getSourceId());
            
            // Read log files from file system
            Map<String, byte[]> logFiles = fileStorageService.readLogFiles(
                    incident.getApplicationName(),
                    incident.getSourceId()
            );
            
            if (logFiles.isEmpty()) {
                log.error("No log files found in file system for incident {}", incident.getSourceId());
                return buildFailedResult("No log files found in file system");
            }
            
            // Combine all log files
            String combinedLogs = combineLogFiles(logFiles);
            
            if (combinedLogs.trim().isEmpty()) {
                log.error("Combined log content is empty for incident {}", incident.getSourceId());
                return buildFailedResult("Log content is empty");
            }
            
            // Extract error logs to reduce size
            String extractedLogs = extractErrorLogs(combinedLogs);
            
            if (extractedLogs.trim().isEmpty()) {
                log.warn("No error patterns found in logs for incident {}, using full logs", 
                        incident.getSourceId());
                extractedLogs = combinedLogs;
            }
            
            // Chunk if still too large
            if (extractedLogs.length() > MAX_LOG_SIZE) {
                log.info("Extracted logs still large ({} chars), chunking to {} chars", 
                        extractedLogs.length(), MAX_LOG_SIZE);
                extractedLogs = extractedLogs.substring(0, MAX_LOG_SIZE);
            }
            
            log.info("Sending {} chars to AI for analysis (original: {} chars)", 
                    extractedLogs.length(), combinedLogs.length());
            
            // Analyze with Watsonx AI
            String aiResponse = watsonxAIService.analyzeLog(extractedLogs);
            
            if (aiResponse == null) {
                log.error("AI analysis failed for incident {}", incident.getSourceId());
                return buildFailedResult("AI analysis failed");
            }
            
            // Parse AI response
            LogAnalysisResult result = parseAIResponse(aiResponse);
            
            if (result.isValid()) {
                log.info("Successfully analyzed logs for incident {}: class={}, line={}", 
                        incident.getSourceId(), result.getClassName(), result.getLineNumber());
            } else {
                log.warn("AI analysis completed but could not extract code location for incident {}", 
                        incident.getSourceId());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Error analyzing logs for incident {}: {}", 
                    incident.getSourceId(), e.getMessage(), e);
            return buildFailedResult("Error: " + e.getMessage());
        }
    }
    
    /**
     * BobAndMe TODO: Extract error-related sections from logs
     * 
     * Implementation Requirements:
     * 1. Split logs into lines
     * 2. Identify lines matching error patterns (Exception, Error, stack traces)
     * 3. Extract context around error lines (before and after)
     * 4. Combine extracted sections
     * 5. Limit total size to avoid huge logs
     * 6. Return extracted error logs
     * 
     * This method intelligently extracts only relevant error sections:
     * - Exception messages and stack traces
     * - ERROR/SEVERE/FATAL log entries
     * - Context lines around errors (5 lines before, 10 lines after)
     * - Deduplicates similar error blocks
     * 
     * @param fullLogs The complete log content
     * @return Extracted error logs with context
     */
    public String extractErrorLogs(String fullLogs) {
        if (fullLogs == null || fullLogs.trim().isEmpty()) {
            return "";
        }
        
        try {
            String[] lines = fullLogs.split("\n");
            log.debug("Extracting errors from {} lines", lines.length);
            
            // Find all error line indices
            Set<Integer> errorLineIndices = new HashSet<>();
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                for (Pattern pattern : ERROR_INDICATORS) {
                    if (pattern.matcher(line).find()) {
                        errorLineIndices.add(i);
                        break;
                    }
                }
            }
            
            if (errorLineIndices.isEmpty()) {
                log.debug("No error patterns found in logs");
                return "";
            }
            
            log.debug("Found {} error lines", errorLineIndices.size());
            
            // Extract context around error lines
            Set<Integer> linesToInclude = new HashSet<>();
            for (Integer errorIndex : errorLineIndices) {
                // Include 5 lines before error
                int startContext = Math.max(0, errorIndex - 5);
                // Include 10 lines after error (to capture stack traces)
                int endContext = Math.min(lines.length - 1, errorIndex + 40);
                
                for (int i = startContext; i <= endContext; i++) {
                    linesToInclude.add(i);
                }
            }
            
            // Build extracted logs maintaining order
            List<Integer> sortedIndices = new ArrayList<>(linesToInclude);
            Collections.sort(sortedIndices);
            
            StringBuilder extracted = new StringBuilder();
            int lastIndex = -2; // Track gaps in line numbers
            
            for (Integer index : sortedIndices) {
                // Add separator if there's a gap
                if (lastIndex >= 0 && index > lastIndex + 1) {
                    extracted.append("\n... [").append(index - lastIndex - 1)
                            .append(" lines omitted] ...\n\n");
                }
                
                extracted.append(lines[index]).append("\n");
                lastIndex = index;
                
                // Stop if we've extracted enough
                if (extracted.length() > MAX_LOG_SIZE) {
                    extracted.append("\n... [truncated - log too large] ...\n");
                    break;
                }
            }
            
            String result = extracted.toString();
            log.info("Extracted {} chars from {} total chars ({} error lines, {} total lines included)", 
                    result.length(), fullLogs.length(), errorLineIndices.size(), linesToInclude.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error extracting error logs: {}", e.getMessage(), e);
            // Return original logs if extraction fails
            return fullLogs;
        }
    }
    
    /**
     * BobAndMe TODO: Combine multiple log files into single string
     * 
     * Implementation Requirements:
     * 1. Iterate through log files map
     * 2. Convert each byte array to string using UTF-8
     * 3. Append filename as header
     * 4. Append file content
     * 5. Add separator between files
     * 6. Return combined string
     * 
     * @param logFiles Map of filename to content bytes
     * @return Combined log content
     */
    public String combineLogFiles(Map<String, byte[]> logFiles) {
        if (logFiles == null || logFiles.isEmpty()) {
            return "";
        }
        
        StringBuilder combined = new StringBuilder();
        
        for (Map.Entry<String, byte[]> entry : logFiles.entrySet()) {
            String filename = entry.getKey();
            byte[] content = entry.getValue();
            
            // Add filename as header
            combined.append("\n=== Log File: ").append(filename).append(" ===\n");
            
            // Convert bytes to string and append
            String logContent = new String(content, StandardCharsets.UTF_8);
            combined.append(logContent);
            
            // Add separator
            combined.append("\n\n");
        }
        
        log.debug("Combined {} log files into single string ({} chars)", 
                logFiles.size(), combined.length());
        
        return combined.toString();
    }
    
    /**
     * BobAndMe TODO: Parse AI response to extract code location
     * 
     * Implementation Requirements:
     * 1. Use regex patterns to extract CLASS, LINE, ERROR from response
     * 2. CLASS_PATTERN: "CLASS: <class name>"
     * 3. LINE_PATTERN: "LINE: <number>"
     * 4. ERROR_PATTERN: "ERROR: <description>"
     * 5. Build LogAnalysisResult with extracted data
     * 6. Set successful=true if all fields extracted
     * 7. Set successful=false if extraction failed
     * 8. Include full AI response in result
     * 
     * @param aiResponse The AI response text
     * @return LogAnalysisResult with parsed data
     */
    public LogAnalysisResult parseAIResponse(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return buildFailedResult("AI response is empty");
        }
        
        try {
            log.debug("Parsing AI response: {}", aiResponse);
            
            // Extract class name
            Matcher classMatcher = CLASS_PATTERN.matcher(aiResponse);
            String className = null;
            if (classMatcher.find()) {
                className = classMatcher.group(1).trim();
            }
            
            // Extract line number
            Matcher lineMatcher = LINE_PATTERN.matcher(aiResponse);
            Integer lineNumber = null;
            if (lineMatcher.find()) {
                try {
                    lineNumber = Integer.parseInt(lineMatcher.group(1).trim());
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse line number: {}", lineMatcher.group(1));
                }
            }
            
            // Extract error description
            Matcher errorMatcher = ERROR_PATTERN.matcher(aiResponse);
            String errorDescription = null;
            if (errorMatcher.find()) {
                errorDescription = errorMatcher.group(1).trim();
            }
            
            // Build result
            LogAnalysisResult result = LogAnalysisResult.builder()
                    .className(className)
                    .lineNumber(lineNumber)
                    .errorDescription(errorDescription)
                    .fullAnalysis(aiResponse)
                    .successful(className != null && lineNumber != null)
                    .build();
            
            if (result.isValid()) {
                log.info("Successfully parsed AI response: class={}, line={}", 
                        className, lineNumber);
            } else {
                log.warn("Could not extract complete code location from AI response");
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Error parsing AI response: {}", e.getMessage(), e);
            return buildFailedResult("Parse error: " + e.getMessage());
        }
    }
    
    /**
     * BobAndMe TODO: Build a failed LogAnalysisResult
     * 
     * Implementation Requirements:
     * 1. Create LogAnalysisResult with successful=false
     * 2. Set errorMessage with provided message
     * 3. Set all other fields to null
     * 4. Return the result
     * 
     * @param errorMessage The error message
     * @return Failed LogAnalysisResult
     */
    public LogAnalysisResult buildFailedResult(String errorMessage) {
        return LogAnalysisResult.builder()
                .successful(false)
                .errorMessage(errorMessage)
                .className(null)
                .lineNumber(null)
                .errorDescription(null)
                .fullAnalysis(null)
                .build();
    }
    
    /**
     * BobAndMe TODO: Validate if log analysis result is usable
     * 
     * Implementation Requirements:
     * 1. Check result.isValid()
     * 2. Log validation result
     * 3. Return boolean
     * 
     * @param result The analysis result to validate
     * @return true if result is valid and usable
     */
    public boolean validateAnalysisResult(LogAnalysisResult result) {
        if (result == null) {
            log.warn("Analysis result is null");
            return false;
        }
        
        boolean valid = result.isValid();
        
        if (valid) {
            log.info("Analysis result is valid: class={}, line={}", 
                    result.getClassName(), result.getLineNumber());
        } else {
            log.warn("Analysis result is invalid: successful={}, className={}, lineNumber={}", 
                    result.isSuccessful(), result.getClassName(), result.getLineNumber());
        }
        
        return valid;
    }
}

// Made with Bob