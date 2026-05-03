# Agent Baki - Configuration Guide

**Quick Reference for Setup, Configuration, and Debugging**

---

## 📋 Configuration Checklist

- [ ] Microsoft Outlook / Azure AD Setup
- [ ] Atlassian Jira Setup
- [ ] IBM Watsonx AI Setup
- [ ] Environment Variables Configured
- [ ] Application Properties Verified
- [ ] Configuration Validation Passed

---

## 🔗 Official Documentation Links

### Microsoft Outlook / Azure AD
- **Azure Portal**: https://portal.azure.com
- **App Registrations**: https://portal.azure.com/#blade/Microsoft_AAD_IAM/ActiveDirectoryMenuBlade/RegisteredApps
- **Microsoft Graph API Docs**: https://docs.microsoft.com/en-us/graph/overview
- **Graph Explorer (Testing)**: https://developer.microsoft.com/en-us/graph/graph-explorer

### Atlassian Jira
- **Jira Cloud**: https://www.atlassian.com/software/jira
- **API Token Management**: https://id.atlassian.com/manage-profile/security/api-tokens
- **Jira REST API Docs**: https://developer.atlassian.com/cloud/jira/platform/rest/v3/
- **Jira API Explorer**: https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issues/

### IBM Watsonx AI
- **IBM Cloud Console**: https://cloud.ibm.com
- **Watsonx.ai**: https://www.ibm.com/products/watsonx-ai
- **API Documentation**: https://cloud.ibm.com/apidocs/watsonx-ai
- **Model Catalog**: https://www.ibm.com/products/watsonx-ai/foundation-models

---

## 🔧 Configuration Properties Reference

### Application Core Settings

```properties
# Application Name
spring.application.name=agent-baki

# Server Port
server.port=8080

# Database (H2 In-Memory)
spring.datasource.url=jdbc:h2:mem:incidentdb
spring.datasource.username=sa
spring.datasource.password=

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

**Access H2 Console**: http://localhost:8080/h2-console

---

### Microsoft Outlook Configuration

**Environment Variables Required:**
```bash
OUTLOOK_CLIENT_ID=your-azure-ad-client-id
OUTLOOK_CLIENT_SECRET=your-azure-ad-client-secret
OUTLOOK_TENANT_ID=your-azure-ad-tenant-id
```

**Application Properties:**
```properties
outlook.client-id=${OUTLOOK_CLIENT_ID}
outlook.client-secret=${OUTLOOK_CLIENT_SECRET}
outlook.tenant-id=${OUTLOOK_TENANT_ID}
outlook.authority=https://login.microsoftonline.com/${outlook.tenant-id}
outlook.scope=https://graph.microsoft.com/.default
```

**Setup Steps:**
1. Go to Azure Portal → Azure Active Directory → App registrations
2. Click "New registration"
3. Set name: "Agent Baki"
4. Click "Register"
5. Note the **Application (client) ID** → Use as `OUTLOOK_CLIENT_ID`
6. Note the **Directory (tenant) ID** → Use as `OUTLOOK_TENANT_ID`
7. Go to "Certificates & secrets" → "New client secret"
8. Copy the secret value → Use as `OUTLOOK_CLIENT_SECRET`
9. Go to "API permissions" → "Add a permission" → "Microsoft Graph" → "Application permissions"
10. Add: `Mail.Read`, `Mail.Send`, `Mail.ReadWrite`
11. Click "Grant admin consent"

**Permissions Required:**
- `Mail.Read` - Read emails
- `Mail.Send` - Send reply emails
- `Mail.ReadWrite` - Manage emails

**Config Class**: `baki/src/main/java/com/agent/baki/config/OutlookConfig.java`

---

### Atlassian Jira Configuration

**Environment Variables Required:**
```bash
JIRA_BASE_URL=https://your-domain.atlassian.net
JIRA_USERNAME=your-email@company.com
JIRA_API_TOKEN=your-jira-api-token
```

**Application Properties:**
```properties
jira.base-url=${JIRA_BASE_URL}
jira.username=${JIRA_USERNAME}
jira.api-token=${JIRA_API_TOKEN}
jira.project-key=${JIRA_PROJECT_KEY:PROJ}
```

**Setup Steps:**
1. Go to https://id.atlassian.com/manage-profile/security/api-tokens
2. Click "Create API token"
3. Set label: "Agent Baki"
4. Click "Create"
5. Copy the token → Use as `JIRA_API_TOKEN`
6. Your Jira URL format: `https://your-domain.atlassian.net`
7. Username is your Jira account email

**Important Notes:**
- ⚠️ Use API Token, NOT your Jira password
- ⚠️ Username must be an email address
- ⚠️ Base URL should NOT include `/rest/api` path

**Config Class**: `baki/src/main/java/com/agent/baki/config/JiraConfig.java`

---

### IBM Watsonx AI Configuration

**Environment Variables Required:**
```bash
WATSONX_API_KEY=your-ibm-cloud-api-key
WATSONX_PROJECT_ID=your-watsonx-project-id
WATSONX_ENDPOINT=https://us-south.ml.cloud.ibm.com
```

**Application Properties:**
```properties
watsonx.api-key=${WATSONX_API_KEY}
watsonx.project-id=${WATSONX_PROJECT_ID}
watsonx.endpoint=${WATSONX_ENDPOINT}
watsonx.model-id=ibm/granite-13b-chat-v2
watsonx.max-tokens=500
watsonx.temperature=0.1
```

**Setup Steps:**
1. Go to https://cloud.ibm.com
2. Create a Watsonx.ai service instance
3. Create a new project in Watsonx.ai
4. Note the **Project ID** from project settings → Use as `WATSONX_PROJECT_ID`
5. Go to IBM Cloud → Manage → Access (IAM) → API keys
6. Click "Create an IBM Cloud API key"
7. Set name: "Agent Baki"
8. Copy the API key → Use as `WATSONX_API_KEY`

**Available Models:**
- `ibm/granite-13b-chat-v2` (Default - Recommended)
- `ibm/granite-20b-multilingual`
- `meta-llama/llama-2-70b-chat`
- `google/flan-ul2`

**Regional Endpoints:**
- US South: `https://us-south.ml.cloud.ibm.com`
- EU Germany: `https://eu-de.ml.cloud.ibm.com`
- Japan Tokyo: `https://jp-tok.ml.cloud.ibm.com`

**Config Class**: `baki/src/main/java/com/agent/baki/config/WatsonxConfig.java`

---

## 🔍 Configuration Validation

### Automatic Validation
Each config class has a `validateConfiguration()` method that checks:
- Required properties are set
- Values are in correct format
- URLs are valid
- Credentials are not empty

### Manual Validation Commands

**Check Outlook Connection:**
```bash
# Test Graph API access
curl -X GET "https://graph.microsoft.com/v1.0/me" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Check Jira Connection:**
```bash
# Test Jira API access
curl -u "your-email@company.com:YOUR_API_TOKEN" \
  "https://your-domain.atlassian.net/rest/api/3/myself"
```

**Check Watsonx AI Connection:**
```bash
# Test Watsonx API access
curl -X POST "https://us-south.ml.cloud.ibm.com/ml/v1/text/generation" \
  -H "Authorization: Bearer YOUR_IAM_TOKEN" \
  -H "Content-Type: application/json"
```

---

## 🐛 Troubleshooting

### Common Issues

#### Outlook Issues

**Error: "Authentication failed"**
- ✅ Verify Client ID, Secret, and Tenant ID are correct
- ✅ Check API permissions are granted
- ✅ Ensure admin consent is given
- ✅ Verify the app has Mail.Read and Mail.Send permissions

**Error: "Insufficient privileges"**
- ✅ Add required permissions in Azure AD
- ✅ Grant admin consent
- ✅ Wait 5-10 minutes for permissions to propagate

#### Jira Issues

**Error: "Unable to connect to Jira"**
- ✅ Verify base URL format: `https://your-domain.atlassian.net`
- ✅ Check API token is valid (not expired)
- ✅ Ensure username is an email address
- ✅ Test connection with curl command above

**Error: "401 Unauthorized"**
- ✅ Regenerate API token
- ✅ Verify username matches token owner
- ✅ Check for typos in credentials

#### Watsonx AI Issues

**Error: "Failed to analyze logs"**
- ✅ Verify API key is valid
- ✅ Check project ID is correct
- ✅ Ensure endpoint URL matches your region
- ✅ Verify Watsonx service is active in IBM Cloud

**Error: "Model not found"**
- ✅ Check model ID spelling
- ✅ Verify model is available in your region
- ✅ Try default model: `ibm/granite-13b-chat-v2`

---

## 📝 Environment Variables Template

Create a `.env` file (add to .gitignore):

```bash
# Microsoft Outlook / Azure AD
OUTLOOK_CLIENT_ID=your-client-id-here
OUTLOOK_CLIENT_SECRET=your-client-secret-here
OUTLOOK_TENANT_ID=your-tenant-id-here

# Atlassian Jira
JIRA_BASE_URL=https://your-domain.atlassian.net
JIRA_USERNAME=your-email@company.com
JIRA_API_TOKEN=your-api-token-here
JIRA_PROJECT_KEY=PROJ

# IBM Watsonx AI
WATSONX_API_KEY=your-api-key-here
WATSONX_PROJECT_ID=your-project-id-here
WATSONX_ENDPOINT=https://us-south.ml.cloud.ibm.com
```

### Windows PowerShell Setup
```powershell
$env:OUTLOOK_CLIENT_ID="your-client-id"
$env:OUTLOOK_CLIENT_SECRET="your-client-secret"
$env:OUTLOOK_TENANT_ID="your-tenant-id"
$env:JIRA_BASE_URL="https://your-domain.atlassian.net"
$env:JIRA_USERNAME="your-email@company.com"
$env:JIRA_API_TOKEN="your-api-token"
$env:WATSONX_API_KEY="your-api-key"
$env:WATSONX_PROJECT_ID="your-project-id"
$env:WATSONX_ENDPOINT="https://us-south.ml.cloud.ibm.com"
```

### Linux/Mac Bash Setup
```bash
export OUTLOOK_CLIENT_ID="your-client-id"
export OUTLOOK_CLIENT_SECRET="your-client-secret"
export OUTLOOK_TENANT_ID="your-tenant-id"
export JIRA_BASE_URL="https://your-domain.atlassian.net"
export JIRA_USERNAME="your-email@company.com"
export JIRA_API_TOKEN="your-api-token"
export WATSONX_API_KEY="your-api-key"
export WATSONX_PROJECT_ID="your-project-id"
export WATSONX_ENDPOINT="https://us-south.ml.cloud.ibm.com"
```

---

## 🔐 Security Best Practices

1. **Never commit credentials to Git**
   - Add `.env` to `.gitignore`
   - Use environment variables
   - Rotate secrets regularly

2. **Use separate credentials for dev/prod**
   - Development: Use test accounts
   - Production: Use service accounts with minimal permissions

3. **Rotate credentials regularly**
   - Azure AD secrets: Every 6-12 months
   - Jira API tokens: Every 6-12 months
   - IBM Cloud API keys: Every 6-12 months

4. **Monitor API usage**
   - Check Azure AD sign-in logs
   - Monitor Jira API rate limits
   - Track Watsonx AI usage and costs

---

## 📊 Configuration Files Location

| Component | File Path |
|-----------|-----------|
| Outlook Config | `baki/src/main/java/com/agent/baki/config/OutlookConfig.java` |
| Jira Config | `baki/src/main/java/com/agent/baki/config/JiraConfig.java` |
| Watsonx Config | `baki/src/main/java/com/agent/baki/config/WatsonxConfig.java` |
| Application Properties | `baki/src/main/resources/application.properties` |
| Environment Template | `.env` (create from template above) |

---

**Last Updated**: 2026-05-03  
**Version**: 1.0  
**Status**: Ready for Configuration