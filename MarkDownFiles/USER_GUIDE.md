# Agent Baki - User Guide

## Table of Contents
1. [Introduction](#introduction)
2. [System Overview](#system-overview)
3. [Prerequisites](#prerequisites)
4. [Installation & Setup](#installation--setup)
5. [Configuration](#configuration)
6. [Using the System](#using-the-system)
7. [Web Interface Guide](#web-interface-guide)
8. [Batch Processing](#batch-processing)
9. [Troubleshooting](#troubleshooting)
10. [FAQ](#faq)

---

## Introduction

**Agent Baki** is an automated incident management system designed for developers. It automatically collects incidents from Outlook emails and Jira tickets, analyzes logs using AI to identify problematic code locations, tracks fixes with deduplication, and automates responses back to incident sources.

### Key Features
- ✅ Automated incident collection from Outlook and Jira
- ✅ AI-powered log analysis using IBM Watsonx
- ✅ Code fix tracking with deduplication
- ✅ Batch processing for automated responses
- ✅ GitHub integration for recent code updates
- ✅ Web UI for developer interaction
- ✅ Comprehensive logging and audit trail

---

## System Overview

### Architecture
```
┌─────────────────┐     ┌─────────────────┐
│  Outlook Email  │────▶│                 │
└─────────────────┘     │   Agent Baki    │
                        │                 │
┌─────────────────┐     │  - Log Analysis │
│  Jira Tickets   │────▶│  - Fix Tracking │
└─────────────────┘     │  - Automation   │
                        │                 │
┌─────────────────┐     │                 │
│  GitHub Repo    │◀───▶│                 │
└─────────────────┘     └─────────────────┘
                               │
                               ▼
                        ┌─────────────────┐
                        │   Web UI        │
                        │   Dashboard     │
                        └─────────────────┘
```

### Workflow
1. **Incident Collection**: System monitors Outlook inbox and Jira projects for new incidents
2. **Log Analysis**: AI analyzes logs to identify problematic code (class name + line number)
3. **Fix Tracking**: Creates or links to existing fix records (deduplication)
4. **Developer Action**: Developers review and update fix status via Web UI
5. **Automated Response**: Batch job sends replies/comments every 2 hours
6. **Cleanup**: Log files are deleted after successful replies

---

## Prerequisites

### Required Software
- **Java 17** or higher
- **Maven 3.8+** for building
- **Git** for version control

### Required Accounts & Credentials
1. **Microsoft Azure AD** (for Outlook integration)
   - Azure AD App Registration
   - Client ID, Client Secret, Tenant ID

2. **Jira Cloud/Server**
   - Jira account with API access
   - API Token

3. **IBM Watsonx AI**
   - IBM Cloud account
   - Watsonx API Key and Project ID

4. **GitHub** (optional, for code update tracking)
   - GitHub account
   - Personal Access Token with `repo` scope

---

## Installation & Setup

### Step 1: Clone the Repository
```bash
git clone <repository-url>
cd baki
```

### Step 2: Build the Project
```bash
mvn clean install
```

### Step 3: Set Environment Variables

Create a `.env` file or set system environment variables:

```bash
# Outlook Configuration
export OUTLOOK_CLIENT_ID=your-azure-client-id
export OUTLOOK_CLIENT_SECRET=your-azure-client-secret
export OUTLOOK_TENANT_ID=your-azure-tenant-id

# Jira Configuration
export JIRA_BASE_URL=https://your-domain.atlassian.net
export JIRA_USERNAME=your-email@example.com
export JIRA_API_TOKEN=your-jira-api-token
export JIRA_PROJECT_KEY=PROJ

# Watsonx AI Configuration
export WATSONX_API_KEY=your-watsonx-api-key
export WATSONX_PROJECT_ID=your-watsonx-project-id
export WATSONX_ENDPOINT=https://us-south.ml.cloud.ibm.com

# GitHub Configuration (Optional)
export GITHUB_USERNAME=your-github-username
export GITHUB_TOKEN=your-github-personal-access-token
```

### Step 4: Run the Application
```bash
mvn spring-boot:run
```

Or run the JAR file:
```bash
java -jar target/baki-1.0.0.jar
```

### Step 5: Access the Application
Open your browser and navigate to:
```
http://localhost:8080
```

---

## Configuration

### application.properties

Key configuration properties in `src/main/resources/application.properties`:

#### Database Configuration
```properties
spring.datasource.url=jdbc:h2:mem:incidentdb
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

#### Batch Job Configuration
```properties
# Runs every 2 hours
batch.reply.cron=0 0 */2 * * ?
batch.reply.enabled=true
```

#### File Storage
```properties
app.storage.base-path=C:/Users
```
Logs are stored at: `C:/Users/Baki/{appName}/issues/{sourceId}/logs/`

#### Logging
```properties
logging.level.com.agent.baki=DEBUG
logging.file.name=logs/agent-baki.log
```

Log files are created in the `logs/` directory:
- `baki-application.log` - Main application logs
- `baki-batch.log` - Batch processing logs
- `baki-error.log` - Error logs only
- `baki-integration.log` - Integration service logs

---

## Using the System

### 1. Incident Collection

#### Email Format
Emails must have subjects containing: **"Issues"**, **"Ticket"**, or **"Incident"**

Example email:
```
Subject: Production Issue - Application Crash
Body:
Application Name: MyApp
Logs:
2024-01-15 10:30:45 ERROR [main] com.example.MyClass - NullPointerException
    at com.example.MyClass.processData(MyClass.java:45)
    at com.example.Service.execute(Service.java:120)
```

#### Jira Ticket Format
Jira tickets should include:
- **Application Name** in description or custom field
- **Log content** in description or comments

### 2. Automated Log Analysis

The system automatically:
1. Extracts application name and logs
2. Sends logs to Watsonx AI for analysis
3. Identifies problematic code location (class + line number)
4. Creates or links to existing fix record

### 3. Fix Management

Developers can manage fixes through the Web UI:

#### Fix Statuses
- **PENDING** - Newly identified, awaiting review
- **IN_PROGRESS** - Developer is working on it
- **DB_FIX** - Fixed via database change
- **RESOLVED** - Code fix deployed
- **IGNORED** - Not a real issue or won't fix

---

## Web Interface Guide

### Dashboard (`/`)
- View all applications with incident counts
- See fix status distribution
- Quick access to application details
- Delete applications (with confirmation)

### Application Details (`/application/{id}`)
- View all fixes for an application
- Fixes grouped by status
- See linked incidents (emails and Jira tickets)
- Delete application with all related data

### Fix Details (`/fix/{id}`)
- View detailed fix information
- Code location (class name + line number)
- GitHub updates (if available)
- Linked incidents list
- Action buttons:
  - **Mark as Ignored** - Close without fixing
  - **Mark as In Progress** - Start working on it
  - **Mark as DB Fix** - Fixed via database
  - **Mark as Resolved** - Code fix deployed
  - **Delete Fix** - Remove fix and unlink incidents

### Action Modals

#### Mark as Ignored
- Provide reason for ignoring
- Automatically sends replies to all linked incidents

#### Mark as In Progress
- Optionally add GitHub PR link
- Notifies stakeholders

#### Mark as DB Fix
- Provide change number
- Add reason/details
- Sends automated responses

#### Mark as Resolved
- Add GitHub PR link (required)
- Automatically notifies all stakeholders

---

## Batch Processing

### Scheduled Job
Runs automatically every 2 hours (00:00, 02:00, 04:00, etc.)

### What It Does
1. **Find Unreplied Incidents**: Queries database for `replied='N'` with fix assigned
2. **Send Email Replies**: Replies to all unreplied emails with fix details
3. **Add Jira Comments**: Adds comments to unreplied Jira tickets
4. **Cleanup Logs**: Deletes log files for successfully replied incidents

### Reply Message Format

#### Email Reply
```
Hello,

Thank you for reporting the incident. Our automated system has analyzed the issue.

Issue Location:
- Class: com.example.MyClass
- Line: 45
- Summary: NullPointerException in processData method

Status: RESOLVED ✓

The issue has been fixed and deployed to production.
GitHub PR: https://github.com/owner/repo/pull/123

The fix should now be live. Please verify and let us know if the issue persists.

---
This is an automated message from Agent Baki Incident Management System.
```

#### Jira Comment
```
*Automated Update from Agent Baki*

Our automated system has analyzed this incident.

h4. Issue Location
* *Class:* {{com.example.MyClass}}
* *Line:* {{45}}
* *Summary:* NullPointerException in processData method

h4. Status: {color:green}RESOLVED{color} (/)

The issue has been fixed and deployed to production.
*GitHub PR:* https://github.com/owner/repo/pull/123

The fix should now be live. Please verify and update this ticket if the issue persists.

----
_This is an automated comment from Agent Baki Incident Management System._
```

### Manual Trigger
For testing or immediate processing, you can manually trigger the batch job:
```bash
curl -X POST http://localhost:8080/api/batch/trigger
```

---

## Troubleshooting

### Common Issues

#### 1. Emails Not Being Collected
**Problem**: No emails appearing in the system

**Solutions**:
- Verify Outlook credentials are correct
- Check email subject contains "Issues", "Ticket", or "Incident"
- Ensure emails are unread
- Check logs: `logs/baki-integration.log`

#### 2. Jira Tickets Not Syncing
**Problem**: Jira tickets not being imported

**Solutions**:
- Verify Jira API token is valid
- Check Jira base URL is correct
- Ensure project key exists
- Review logs: `logs/baki-integration.log`

#### 3. AI Analysis Failing
**Problem**: Logs not being analyzed

**Solutions**:
- Verify Watsonx API key and project ID
- Check log content is valid (not empty)
- Ensure logs contain error information
- Review logs: `logs/baki-application.log`

#### 4. Batch Job Not Running
**Problem**: Automated replies not being sent

**Solutions**:
- Check `batch.reply.enabled=true` in application.properties
- Verify cron expression is correct
- Check logs: `logs/baki-batch.log`
- Manually trigger to test: `/api/batch/trigger`

#### 5. GitHub Integration Not Working
**Problem**: No GitHub updates appearing

**Solutions**:
- Verify GitHub token has `repo` scope
- Check repository URL in application settings
- Ensure UAT branch exists
- Review logs: `logs/baki-integration.log`

### Log Files Location
```
logs/
├── baki-application.log      # Main application logs
├── baki-batch.log            # Batch processing logs
├── baki-error.log            # Error logs only
├── baki-integration.log      # Integration service logs
└── archived/                 # Compressed old logs
    ├── baki-application-2024-01-15.log.gz
    └── ...
```

### Database Console
Access H2 database console for debugging:
```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:incidentdb
Username: sa
Password: (leave empty)
```

---

## FAQ

### Q: How does deduplication work?
**A**: The system checks if a fix already exists for the same code location (className + lineNumber). If found, new incidents are linked to the existing fix instead of creating duplicates.

### Q: Can I change the batch job schedule?
**A**: Yes, modify the `batch.reply.cron` property in `application.properties`. Use standard cron expression format.

### Q: What happens to log files after replies are sent?
**A**: Log files are automatically deleted after successful replies to save disk space. The cleanup happens as part of the batch job.

### Q: Can I manually send replies without waiting for the batch job?
**A**: Yes, update the fix status via the Web UI, and the batch job will process it in the next run. Or manually trigger the batch job via API.

### Q: How do I add a new application?
**A**: Applications are automatically created when the first incident is received. Just ensure the email/Jira ticket includes the application name.

### Q: Can I use this with on-premise Jira?
**A**: Yes, just update the `jira.base-url` to point to your on-premise Jira server.

### Q: What AI model is used for log analysis?
**A**: IBM Watsonx AI with the Granite 13B Chat v2 model. You can change this in `application.properties` (`watsonx.model-id`).

### Q: How do I backup the database?
**A**: The system uses H2 in-memory database by default. For production, configure a persistent database (PostgreSQL, MySQL) in `application.properties`.

### Q: Can I customize the reply message format?
**A**: Yes, modify the `buildReplyMessage()` method in `BatchEmailReplyService.java` and `buildCommentMessage()` in `BatchJiraCommentService.java`.

### Q: How do I monitor system health?
**A**: Use Spring Boot Actuator endpoints:
- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Info: `http://localhost:8080/actuator/info`

---

## Support

For issues, questions, or feature requests:
- Check the logs in `logs/` directory
- Review this user guide
- Contact the development team

---

**Agent Baki** - Automated Incident Management for Developers
Version 2.0 | Last Updated: 2024