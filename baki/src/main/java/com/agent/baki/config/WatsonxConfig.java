package com.agent.baki.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for IBM Watsonx AI integration
 * 
 * This class configures the Watsonx AI client for log analysis.
 * It uses IBM Cloud IAM authentication with API key.
 * 
 * Required Environment Variables:
 * - WATSONX_API_KEY: IBM Cloud API key
 * - WATSONX_PROJECT_ID: Watsonx.ai project ID
 * - WATSONX_ENDPOINT: Watsonx.ai service endpoint URL
 * 
 * Setup Instructions:
 * 1. Create a Watsonx.ai service instance in IBM Cloud
 * 2. Create a project in Watsonx.ai
 * 3. Generate an API key from IBM Cloud IAM
 * 4. Set environment variables with the credentials
 * 
 * @author Team Baki
 */
@Configuration
@Getter
public class WatsonxConfig {
    
    /**
     * IBM Cloud API Key
     * Used for authentication with Watsonx.ai service
     * Generated from IBM Cloud > Manage > Access (IAM) > API keys
     * Keep this key secure and rotate regularly
     */
    @Value("${watsonx.api-key}")
    private String apiKey;
    
    /**
     * Watsonx.ai Project ID
     * Unique identifier for your Watsonx.ai project
     * Found in Watsonx.ai project settings
     */
    @Value("${watsonx.project-id}")
    private String projectId;
    
    /**
     * Watsonx.ai Service Endpoint
     * Base URL for Watsonx.ai API calls
     * Default: https://us-south.ml.cloud.ibm.com
     * Other regions: eu-de, jp-tok, etc.
     */
    @Value("${watsonx.endpoint}")
    private String endpoint;
    
    /**
     * Watsonx.ai Model ID
     * The foundation model to use for text generation
     * Default: ibm/granite-13b-chat-v2
     * Other options: ibm/granite-20b-multilingual, meta-llama/llama-2-70b-chat, etc.
     */
    @Value("${watsonx.model-id:ibm/granite-13b-chat-v2}")
    private String modelId;
    
    /**
     * Maximum number of tokens to generate
     * Controls the length of AI response
     * Default: 500 tokens
     * Range: 1-4096 (depends on model)
     */
    @Value("${watsonx.max-tokens:500}")
    private Integer maxTokens;
    
    /**
     * Temperature for text generation
     * Controls randomness in responses
     * Default: 0.1 (more deterministic)
     * Range: 0.0-2.0 (higher = more creative/random)
     */
    @Value("${watsonx.temperature:0.1}")
    private Double temperature;
    
    /**
     * Creates and configures RestTemplate for Watsonx AI API calls
     * 
     * RestTemplate is used for making HTTP requests to Watsonx.ai API.
     * It handles:
     * - HTTP request/response serialization
     * - Error handling
     * - Connection pooling
     * 
     * This bean can be used by WatsonxAIService to make API calls.
     * 
     * @return RestTemplate configured for HTTP calls
     */
    @Bean
    public RestTemplate watsonxRestTemplate() {
        return new RestTemplate();
    }
    
    /**
     * Gets the full Watsonx.ai API endpoint URL
     * 
     * Constructs the complete URL for text generation API
     * Format: {endpoint}/ml/v1/text/generation
     * 
     * @return Full API endpoint URL
     */
    public String getTextGenerationEndpoint() {
        return endpoint + "/ml/v1/text/generation";
    }
    
    /**
     * Gets the IAM token endpoint for authentication
     * 
     * IBM Cloud uses IAM tokens for API authentication
     * Format: https://iam.cloud.ibm.com/identity/token
     * 
     * @return IAM token endpoint URL
     */
    public String getIamTokenEndpoint() {
        return "https://iam.cloud.ibm.com/identity/token";
    }
    
    /**
     * Validates that all required Watsonx configuration properties are set
     * 
     * This method is called after bean initialization to ensure
     * all required properties are configured before the application starts.
     * 
     * Validates:
     * - API Key is not empty
     * - Project ID is not empty
     * - Endpoint is not empty and is a valid URL
     * - Model ID is not empty
     * - Max Tokens is positive
     * - Temperature is within valid range
     * 
     * @throws IllegalStateException if any required property is missing or invalid
     */
    public void validateConfiguration() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "Watsonx API Key is not configured. Set WATSONX_API_KEY environment variable.");
        }
        
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalStateException(
                "Watsonx Project ID is not configured. Set WATSONX_PROJECT_ID environment variable.");
        }
        
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalStateException(
                "Watsonx Endpoint is not configured. Set WATSONX_ENDPOINT environment variable.");
        }
        
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            throw new IllegalStateException(
                "Watsonx Endpoint must start with http:// or https://. Current value: " + endpoint);
        }
        
        if (modelId == null || modelId.trim().isEmpty()) {
            throw new IllegalStateException(
                "Watsonx Model ID is not configured. Check watsonx.model-id property.");
        }
        
        if (maxTokens == null || maxTokens <= 0) {
            throw new IllegalStateException(
                "Watsonx Max Tokens must be positive. Current value: " + maxTokens);
        }
        
        if (temperature == null || temperature < 0.0 || temperature > 2.0) {
            throw new IllegalStateException(
                "Watsonx Temperature must be between 0.0 and 2.0. Current value: " + temperature);
        }
    }
    
    /**
     * Gets the model configuration as a formatted string
     * 
     * Useful for logging and debugging
     * 
     * @return Model configuration summary
     */
    public String getModelConfigSummary() {
        return String.format(
            "Model: %s, MaxTokens: %d, Temperature: %.2f",
            modelId, maxTokens, temperature
        );
    }
}

// Made with Bob
