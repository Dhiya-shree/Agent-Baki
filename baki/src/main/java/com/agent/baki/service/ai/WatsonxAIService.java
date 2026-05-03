package com.agent.baki.service.ai;

import com.agent.baki.config.WatsonxConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for IBM Watsonx AI integration
 * 
 * Handles:
 * - IAM token generation
 * - Text generation API calls
 * - Log analysis prompts
 * 
 * @author Team Baki
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WatsonxAIService {
    
    private final WatsonxConfig watsonxConfig;
    private final RestTemplate watsonxRestTemplate;
    
    // Cache IAM token
    private String cachedIamToken;
    private long tokenExpiryTime;
    
    /**
     * BobAndMe TODO: Get IAM access token for Watsonx AI
     * 
     * Implementation Requirements:
     * 1. Check if cached token is still valid (not expired)
     * 2. If valid, return cached token
     * 3. If expired or null, request new token from IBM Cloud IAM
     * 4. POST to watsonxConfig.getIamTokenEndpoint()
     * 5. Request body: grant_type=urn:ibm:params:oauth:grant-type:apikey, apikey={apiKey}
     * 6. Parse response JSON to extract access_token and expires_in
     * 7. Cache token and calculate expiry time
     * 8. Return access token
     * 9. Handle exceptions and return null on error
     * 
     * @return IAM access token, or null if failed
     */
    public String getIamToken() {
        // Check if cached token is still valid
        long currentTime = System.currentTimeMillis();
        if (cachedIamToken != null && currentTime < tokenExpiryTime) {
            log.debug("Using cached IAM token");
            return cachedIamToken;
        }
        
        try {
            log.info("Requesting new IAM token from IBM Cloud");
            
            // Build request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            // Build request body
            String requestBody = "grant_type=urn:ibm:params:oauth:grant-type:apikey&apikey=" + 
                               watsonxConfig.getApiKey();
            
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
            
            // Make POST request to IAM endpoint
            ResponseEntity<Map> response = watsonxRestTemplate.postForEntity(
                    watsonxConfig.getIamTokenEndpoint(),
                    request,
                    Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // Extract access token
                String accessToken = (String) responseBody.get("access_token");
                
                // Extract expiry time (in seconds)
                Integer expiresIn = (Integer) responseBody.get("expires_in");
                
                if (accessToken != null && expiresIn != null) {
                    // Cache token and calculate expiry time (subtract 5 minutes for safety)
                    cachedIamToken = accessToken;
                    tokenExpiryTime = currentTime + ((expiresIn - 300) * 1000L);
                    
                    log.info("Successfully obtained IAM token, expires in {} seconds", expiresIn);
                    return accessToken;
                }
            }
            
            log.error("Failed to obtain IAM token: invalid response");
            return null;
            
        } catch (Exception e) {
            log.error("Error obtaining IAM token: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * BobAndMe TODO: Analyze log content using Watsonx AI
     * 
     * Implementation Requirements:
     * 1. Get IAM token using getIamToken()
     * 2. Build prompt for log analysis
     * 3. Create request body with model_id, input (prompt + log content), parameters
     * 4. Set headers: Authorization (Bearer token), Content-Type (application/json)
     * 5. POST to watsonxConfig.getTextGenerationEndpoint()
     * 6. Parse response JSON to extract generated_text
     * 7. Return AI response text
     * 8. Handle exceptions and return null on error
     * 
     * Request Body Format:
     * {
     *   "model_id": "ibm/granite-13b-chat-v2",
     *   "input": "prompt + log content",
     *   "parameters": {
     *     "max_new_tokens": 500,
     *     "temperature": 0.1
     *   },
     *   "project_id": "project-id"
     * }
     * 
     * @param logContent The log content to analyze
     * @return AI analysis response, or null if failed
     */
    public String analyzeLog(String logContent) {
        if (logContent == null || logContent.trim().isEmpty()) {
            log.error("Cannot analyze empty log content");
            return null;
        }
        
        try {
            // Get IAM token
            String iamToken = getIamToken();
            if (iamToken == null) {
                log.error("Cannot analyze log: failed to obtain IAM token");
                return null;
            }
            
            log.info("Analyzing log content with Watsonx AI (length: {} chars)", logContent.length());
            
            // Build prompt
            String prompt = buildLogAnalysisPrompt(logContent);
            
            // Build request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(iamToken);
            
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model_id", watsonxConfig.getModelId());
            requestBody.put("input", prompt);
            requestBody.put("project_id", watsonxConfig.getProjectId());
            
            // Add parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("max_new_tokens", watsonxConfig.getMaxTokens());
            parameters.put("temperature", watsonxConfig.getTemperature());
            requestBody.put("parameters", parameters);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Make POST request to Watsonx AI
            ResponseEntity<Map> response = watsonxRestTemplate.postForEntity(
                    watsonxConfig.getTextGenerationEndpoint(),
                    request,
                    Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // Extract generated text from results array
                if (responseBody.containsKey("results")) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> results = 
                        (java.util.List<Map<String, Object>>) responseBody.get("results");
                    
                    if (!results.isEmpty()) {
                        String generatedText = (String) results.get(0).get("generated_text");
                        
                        if (generatedText != null) {
                            log.info("Successfully analyzed log with Watsonx AI");
                            return generatedText;
                        }
                    }
                }
            }
            
            log.error("Failed to analyze log: invalid response from Watsonx AI");
            return null;
            
        } catch (Exception e) {
            log.error("Error analyzing log with Watsonx AI: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * BobAndMe TODO: Build prompt for log analysis
     * 
     * Implementation Requirements:
     * 1. Create structured prompt asking AI to identify:
     *    - Problematic code class name
     *    - Problematic code line number
     *    - Error description
     * 2. Include instructions for response format
     * 3. Append log content to prompt
     * 4. Return complete prompt string
     * 
     * Prompt Template:
     * "Analyze the following application log and identify the problematic code location.
     * 
     * Provide the response in this exact format:
     * CLASS: <fully qualified class name>
     * LINE: <line number>
     * ERROR: <brief error description>
     * 
     * Log content:
     * {logContent}
     * 
     * Analysis:"
     * 
     * @param logContent The log content
     * @return Complete prompt string
     */
    public String buildLogAnalysisPrompt(String logContent) {
        // Truncate log content if too long (keep last 3000 chars for context)
        String truncatedLog = logContent;
        if (logContent.length() > 3000) {
            truncatedLog = "..." + logContent.substring(logContent.length() - 3000);
            log.debug("Truncated log content from {} to 3000 chars", logContent.length());
        }
        
        return "Analyze the following application log and identify the problematic code location.\n\n" +
               "Provide the response in this EXACT format:\n" +
               "CLASS: <fully qualified class name>\n" +
               "LINE: <line number>\n" +
               "ERROR: <brief error description>\n\n" +
               "Important:\n" +
               "- Extract the exact class name from the stack trace\n" +
               "- Extract the exact line number from the stack trace\n" +
               "- Provide a brief description of the error\n" +
               "- Do not include any additional text or explanation\n\n" +
               "Log content:\n" +
               truncatedLog + "\n\n" +
               "Analysis:";
    }
}

// Made with Bob