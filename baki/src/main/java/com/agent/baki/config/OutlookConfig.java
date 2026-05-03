package com.agent.baki.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration class for Microsoft Outlook integration
 *
 * This class configures the Microsoft Graph API client for accessing Outlook emails.
 * It uses Azure AD authentication with client credentials flow (service principal).
 *
 * Required Environment Variables:
 * - OUTLOOK_CLIENT_ID: Azure AD application (client) ID
 * - OUTLOOK_CLIENT_SECRET: Azure AD application client secret
 * - OUTLOOK_TENANT_ID: Azure AD tenant ID
 *
 * Setup Instructions:
 * 1. Register an application in Azure AD
 * 2. Grant Mail.Read and Mail.Send permissions
 * 3. Create a client secret
 * 4. Set environment variables with the credentials
 *
 * @author Team Baki
 */
@Configuration
@ConditionalOnProperty(name = "outlook.config.enabled", havingValue = "true", matchIfMissing = false)
@Getter
public class OutlookConfig {
    
    /**
     * Azure AD Application (Client) ID
     * Obtained from Azure Portal > App Registrations
     */
    @Value("${outlook.client-id}")
    private String clientId;
    
    /**
     * Azure AD Application Client Secret
     * Created in Azure Portal > App Registrations > Certificates & secrets
     * Note: Keep this secret secure and rotate regularly
     */
    @Value("${outlook.client-secret}")
    private String clientSecret;
    
    /**
     * Azure AD Tenant ID
     * Obtained from Azure Portal > Azure Active Directory > Overview
     */
    @Value("${outlook.tenant-id}")
    private String tenantId;
    
    /**
     * Azure AD Authority URL
     * Format: https://login.microsoftonline.com/{tenant-id}
     * Used for authentication endpoint
     */
    @Value("${outlook.authority}")
    private String authority;
    
    /**
     * Microsoft Graph API Scope
     * Default: https://graph.microsoft.com/.default
     * Grants all permissions configured in Azure AD
     */
    @Value("${outlook.scope}")
    private String scope;
    
    /**
     * Creates and configures Azure AD Client Secret Credential
     * 
     * This credential is used for service-to-service authentication
     * using the OAuth 2.0 client credentials flow.
     * 
     * Flow:
     * 1. Application authenticates with Azure AD using client ID and secret
     * 2. Azure AD returns an access token
     * 3. Token is used to call Microsoft Graph API
     * 
     * @return ClientSecretCredential configured with Azure AD credentials
     */
    @Bean
    public ClientSecretCredential clientSecretCredential() {
        return new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();
    }
    
    /**
     * Creates and configures Token Credential Auth Provider
     * 
     * This provider manages token acquisition and refresh for Graph API calls.
     * It automatically handles:
     * - Token acquisition from Azure AD
     * - Token caching
     * - Token refresh when expired
     * 
     * @param credential The Azure AD client secret credential
     * @return TokenCredentialAuthProvider for Graph API authentication
     */
    @Bean
    public TokenCredentialAuthProvider tokenCredentialAuthProvider(
            ClientSecretCredential credential) {
        
        // Define the scopes (permissions) required for Graph API
        List<String> scopes = Arrays.asList(scope);
        
        return new TokenCredentialAuthProvider(scopes, credential);
    }
    
    /**
     * Creates and configures Microsoft Graph Service Client
     * 
     * This is the main client for interacting with Microsoft Graph API.
     * It provides access to:
     * - Outlook Mail (reading and sending emails)
     * - Calendar, Contacts, OneDrive, etc. (if needed in future)
     * 
     * Usage Example:
     * <pre>
     * {@code
     * graphServiceClient.me()
     *     .messages()
     *     .buildRequest()
     *     .filter("contains(subject, 'Issue')")
     *     .get();
     * }
     * </pre>
     * 
     * @param authProvider The token credential auth provider
     * @return GraphServiceClient configured for API calls
     */
    @Bean
    public GraphServiceClient<?> graphServiceClient(
            TokenCredentialAuthProvider authProvider) {
        
        return GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();
    }
    
    /**
     * Validates that all required Outlook configuration properties are set
     * 
     * This method is called after bean initialization to ensure
     * all required properties are configured before the application starts.
     * 
     * Validates:
     * - Client ID is not empty
     * - Client Secret is not empty
     * - Tenant ID is not empty
     * 
     * @throws IllegalStateException if any required property is missing
     */
    public void validateConfiguration() {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalStateException(
                "Outlook Client ID is not configured. Set OUTLOOK_CLIENT_ID environment variable.");
        }
        
        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new IllegalStateException(
                "Outlook Client Secret is not configured. Set OUTLOOK_CLIENT_SECRET environment variable.");
        }
        
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalStateException(
                "Outlook Tenant ID is not configured. Set OUTLOOK_TENANT_ID environment variable.");
        }
    }
}

// Made with Bob
