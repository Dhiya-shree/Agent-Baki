# Setup and Deployment Guide - Incident Management Automation System

## Prerequisites

### Required Software
- **Java Development Kit (JDK)**: Version 17 or higher
- **Maven**: Version 3.8 or higher
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions
- **Git**: For version control

### Required Accounts & Access
1. **Microsoft Azure AD** (for Outlook integration)
   - Azure AD tenant
   - App registration with Mail.Read and Mail.Send permissions
   
2. **Atlassian Jira** (for Jira integration)
   - Jira Cloud account
   - API token with read/write permissions
   
3. **IBM Watsonx AI** (for log analysis)
   - IBM Cloud account
   - Watsonx AI service instance
   - API key and project ID

## Step 1: Project Setup

### 1.1 Create Spring Boot Project

Using Spring Initializr (https://start.spring.io/):

```
Project: Maven
Language: Java
Spring Boot: 3.2.x
Packaging: Jar
Java: 17

Dependencies:
- Spring Web
- Spring Data JPA
- Thymeleaf
- H2 Database
- Lombok
- Validation
```

Or use the provided [`pom.xml`](TECHNICAL_SPECIFICATION.md:1-77) from the technical specification.

### 1.2 Clone or Initialize Repository

```bash
git clone <your-repository-url>
cd incident-management-system
```

### 1.3 Project Structure

Create the following directory structure:

```
incident-management-system/
├── src/
│   ├── main/
│   │   ├── java/com/incident/management/
│   │   │   ├── IncidentManagementApplication.java
│   │   │   ├── config/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   └── exception/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── templates/
│   │       └── static/
│   └── test/
├── pom.xml
└── README.md
```

## Step 2: Configure External Services

### 2.1 Microsoft Outlook Setup

#### Register Azure AD Application

1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to **Azure Active Directory** > **App registrations**
3. Click **New registration**
   - Name: `Incident Management System`
   - Supported account types: `Accounts in this organizational directory only`
   - Redirect URI: Leave blank for now
4. Click **Register**

#### Configure API Permissions

1. In your app registration, go to **API permissions**
2. Click **Add a permission** > **Microsoft Graph** > **Application permissions**
3. Add the following permissions:
   - `Mail.Read`
   - `Mail.Send`
   - `Mail.ReadWrite`
4. Click **Grant admin consent**

#### Create Client Secret

1. Go to **Certificates & secrets**
2. Click **New client secret**
3. Add description: `Incident Management Secret`
4. Set expiration: 24 months
5. Click **Add**
6. **Copy the secret value immediately** (you won't see it again)

#### Note Down Credentials

```
Client ID: <your-client-id>
Client Secret: <your-client-secret>
Tenant ID: <your-tenant-id>
```

### 2.2 Jira Setup

#### Generate API Token

1. Go to [Atlassian Account Settings](https://id.atlassian.com/manage-profile/security/api-tokens)
2. Click **Create API token**
3. Label: `Incident Management System`
4. Click **Create**
5. **Copy the token** (you won't see it again)

#### Note Down Credentials

```
Jira Base URL: https://your-domain.atlassian.net
Username: your-email@example.com
API Token: <your-api-token>
```

### 2.3 Watsonx AI Setup

#### Create Watsonx AI Service

1. Go to [IBM Cloud](https://cloud.ibm.com)
2. Navigate to **Catalog** > **AI / Machine Learning**
3. Select **Watsonx.ai**
4. Create a new instance
5. Create a project in Watsonx.ai

#### Get API Credentials

1. In your Watsonx project, go to **Manage** > **Access (IAM)**
2. Create an API key
3. Note down:
   - API Key
   - Project ID
   - Endpoint URL (usually: `https://us-south.ml.cloud.ibm.com`)

## Step 3: Environment Configuration

### 3.1 Create Environment Variables

Create a `.env` file in the project root (add to `.gitignore`):

```bash
# Outlook Configuration
OUTLOOK_CLIENT_ID=your-client-id-here
OUTLOOK_CLIENT_SECRET=your-client-secret-here
OUTLOOK_TENANT_ID=your-tenant-id-here

# Jira Configuration
JIRA_BASE_URL=https://your-domain.atlassian.net
JIRA_USERNAME=your-email@example.com
JIRA_API_TOKEN=your-jira-api-token-here

# Watsonx AI Configuration
WATSONX_API_KEY=your-watsonx-api-key-here
WATSONX_PROJECT_ID=your-project-id-here
WATSONX_ENDPOINT=https://us-south.ml.cloud.ibm.com
```

### 3.2 Update application.properties

Update [`src/main/resources/application.properties`](TECHNICAL_SPECIFICATION.md:1040-1077):

```properties
# Server
spring.application.name=incident-management-system
server.port=8080

# H2 Database
spring.datasource.url=jdbc:h2:mem:incidentdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# File Storage
app.storage.base-path=C:/Users

# Outlook
outlook.client-id=${OUTLOOK_CLIENT_ID}
outlook.client-secret=${OUTLOOK_CLIENT_SECRET}
outlook.tenant-id=${OUTLOOK_TENANT_ID}

# Jira
jira.base-url=${JIRA_BASE_URL}
jira.username=${JIRA_USERNAME}
jira.api-token=${JIRA_API_TOKEN}

# Watsonx AI
watsonx.api-key=${WATSONX_API_KEY}
watsonx.project-id=${WATSONX_PROJECT_ID}
watsonx.endpoint=${WATSONX_ENDPOINT}

# Batch Job (runs every 2 hours)
batch.reply.cron=0 0 */2 * * ?

# Logging
logging.level.root=INFO
logging.level.com.incident.management=DEBUG
```

### 3.3 Set Environment Variables (Windows)

For Windows development:

```powershell
# PowerShell
$env:OUTLOOK_CLIENT_ID="your-client-id"
$env:OUTLOOK_CLIENT_SECRET="your-client-secret"
$env:OUTLOOK_TENANT_ID="your-tenant-id"
$env:JIRA_BASE_URL="https://your-domain.atlassian.net"
$env:JIRA_USERNAME="your-email@example.com"
$env:JIRA_API_TOKEN="your-api-token"
$env:WATSONX_API_KEY="your-api-key"
$env:WATSONX_PROJECT_ID="your-project-id"
$env:WATSONX_ENDPOINT="https://us-south.ml.cloud.ibm.com"
```

Or create a `setenv.bat` file:

```batch
@echo off
set OUTLOOK_CLIENT_ID=your-client-id
set OUTLOOK_CLIENT_SECRET=your-client-secret
set OUTLOOK_TENANT_ID=your-tenant-id
set JIRA_BASE_URL=https://your-domain.atlassian.net
set JIRA_USERNAME=your-email@example.com
set JIRA_API_TOKEN=your-api-token
set WATSONX_API_KEY=your-api-key
set WATSONX_PROJECT_ID=your-project-id
set WATSONX_ENDPOINT=https://us-south.ml.cloud.ibm.com
```

Run before starting the application:
```batch
setenv.bat
```

## Step 4: Build and Run

### 4.1 Build the Project

```bash
mvn clean install
```

### 4.2 Run the Application

```bash
mvn spring-boot:run
```

Or run the JAR directly:

```bash
java -jar target/incident-management-system-0.0.1-SNAPSHOT.jar
```

### 4.3 Verify Application is Running

1. Open browser and navigate to: `http://localhost:8080`
2. Check H2 Console: `http://localhost:8080/h2-console`
   - JDBC URL: `jdbc:h2:mem:incidentdb`
   - Username: `sa`
   - Password: (leave blank)

## Step 5: Initial Data Setup

### 5.1 Add Application Entries

You can add applications via the H2 console or create a data initialization script:

```sql
INSERT INTO application_data (application_name, repository_link) 
VALUES ('MyApp', 'C:/Users/Developer/repos/myapp');

INSERT INTO application_data (application_name, repository_link) 
VALUES ('WebService', 'C:/Users/Developer/repos/webservice');
```

### 5.2 Test Email Collection

The system will automatically start collecting emails on startup. Check logs:

```
INFO  c.i.m.s.c.OutlookCollectorService - Fetching emails from Outlook
INFO  c.i.m.s.p.IncidentParserService - Parsing email: [Subject]
INFO  c.i.m.s.a.LogAnalysisService - Processing incident for application: MyApp
```

## Step 6: Testing

### 6.1 Manual Testing Checklist

- [ ] Application starts without errors
- [ ] H2 console is accessible
- [ ] Database tables are created correctly
- [ ] Outlook integration fetches emails
- [ ] Jira integration fetches issues
- [ ] Logs are saved to file system
- [ ] Watsonx AI analysis works
- [ ] UI displays applications and fixes
- [ ] Action buttons (Ignore, In Progress, DB Fix) work
- [ ] Batch job runs on schedule
- [ ] Replies are sent successfully

### 6.2 Run Unit Tests

```bash
mvn test
```

### 6.3 Run Integration Tests

```bash
mvn verify
```

## Step 7: Accessing the Application

### 7.1 Web Interface

- **Dashboard**: `http://localhost:8080/dashboard`
- **Applications List**: `http://localhost:8080/applications`
- **H2 Console**: `http://localhost:8080/h2-console`

### 7.2 REST API Endpoints

Test using curl or Postman:

```bash
# Get all applications
curl http://localhost:8080/api/applications

# Get fixes for an application
curl http://localhost:8080/api/applications/1/fixes

# Mark fix as ignored
curl -X POST http://localhost:8080/api/fixes/1/ignore \
  -H "Content-Type: application/json" \
  -d '{"reason": "Duplicate issue"}'

# Mark fix as in progress
curl -X POST http://localhost:8080/api/fixes/1/in-progress \
  -H "Content-Type: application/json" \
  -d '{"prNumber": "PR-123"}'

# Mark as DB fix
curl -X POST http://localhost:8080/api/fixes/1/db-fix \
  -H "Content-Type: application/json" \
  -d '{"fixNumber": "FIX-456"}'
```

## Step 8: Monitoring and Logs

### 8.1 Application Logs

Logs are written to console and can be configured to write to files:

```properties
# Add to application.properties
logging.file.name=logs/incident-management.log
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

### 8.2 Monitor Batch Jobs

Check logs for batch job execution:

```
INFO  c.i.m.s.b.BatchReplyService - Starting batch reply process
INFO  c.i.m.s.b.BatchReplyService - Processed mail ID: 1
INFO  c.i.m.s.b.BatchReplyService - Processed Jira ticket: PROJ-123
INFO  c.i.m.s.b.BatchReplyService - Batch reply process completed
```

### 8.3 Database Monitoring

Use H2 Console to monitor:
- Number of applications
- Number of fixes by status
- Unreplied mails/jiras

```sql
-- Check unreplied incidents
SELECT COUNT(*) FROM mail WHERE replied = 'N';
SELECT COUNT(*) FROM jira WHERE replied = 'N';

-- Check fix status distribution
SELECT issue_status, COUNT(*) FROM fix GROUP BY issue_status;
```

## Step 9: Troubleshooting

### Common Issues

#### 1. Outlook Authentication Fails

**Error**: `Authentication failed`

**Solution**:
- Verify client ID, secret, and tenant ID
- Check API permissions are granted
- Ensure admin consent is given
- Verify the app has Mail.Read and Mail.Send permissions

#### 2. Jira Connection Fails

**Error**: `Unable to connect to Jira`

**Solution**:
- Verify Jira base URL is correct
- Check API token is valid
- Ensure username matches the token owner
- Test connection manually: `curl -u email:token https://your-domain.atlassian.net/rest/api/3/myself`

#### 3. Watsonx AI Analysis Fails

**Error**: `Failed to analyze logs`

**Solution**:
- Verify API key is valid
- Check project ID is correct
- Ensure endpoint URL is correct
- Verify Watsonx service is active in IBM Cloud

#### 4. File System Errors

**Error**: `Failed to create log directory`

**Solution**:
- Check `app.storage.base-path` is correct
- Verify write permissions on the directory
- Ensure the path exists or can be created

#### 5. Batch Job Not Running

**Error**: Batch job doesn't execute

**Solution**:
- Check cron expression is valid
- Verify `@EnableScheduling` is present in configuration
- Check application logs for scheduler errors

## Step 10: Production Deployment

### 10.1 Database Migration

For production, replace H2 with a persistent database:

```properties
# PostgreSQL example
spring.datasource.url=jdbc:postgresql://localhost:5432/incidentdb
spring.datasource.username=postgres
spring.datasource.password=your-password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
```

### 10.2 Security Hardening

1. **Disable H2 Console**:
   ```properties
   spring.h2.console.enabled=false
   ```

2. **Use HTTPS**:
   ```properties
   server.ssl.enabled=true
   server.ssl.key-store=classpath:keystore.p12
   server.ssl.key-store-password=your-password
   server.ssl.key-store-type=PKCS12
   ```

3. **Add Spring Security**:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-security</artifactId>
   </dependency>
   ```

### 10.3 Build Production JAR

```bash
mvn clean package -DskipTests
```

The JAR will be in `target/incident-management-system-0.0.1-SNAPSHOT.jar`

### 10.4 Run as Windows Service

Use [WinSW](https://github.com/winsw/winsw) to run as a Windows service:

1. Download `WinSW.exe`
2. Create `incident-management-service.xml`:

```xml
<service>
  <id>incident-management</id>
  <name>Incident Management System</name>
  <description>Automated incident management and code fix tracking</description>
  <executable>java</executable>
  <arguments>-jar "C:\path\to\incident-management-system.jar"</arguments>
  <logpath>C:\path\to\logs</logpath>
  <log mode="roll-by-size">
    <sizeThreshold>10240</sizeThreshold>
    <keepFiles>8</keepFiles>
  </log>
  <env name="OUTLOOK_CLIENT_ID" value="your-client-id"/>
  <env name="OUTLOOK_CLIENT_SECRET" value="your-client-secret"/>
  <!-- Add other environment variables -->
</service>
```

3. Install service:
```batch
WinSW.exe install incident-management-service.xml
WinSW.exe start
```

## Step 11: Maintenance

### 11.1 Regular Tasks

- **Weekly**: Review unreplied incidents
- **Monthly**: Clean up old log files
- **Quarterly**: Review and update API credentials
- **Annually**: Renew Azure AD client secrets

### 11.2 Backup Strategy

1. **Database Backup** (if using persistent DB):
   ```bash
   pg_dump incidentdb > backup_$(date +%Y%m%d).sql
   ```

2. **Log Files Backup**:
   - Archive logs older than 30 days
   - Store in separate backup location

### 11.3 Monitoring Metrics

Track these metrics:
- Incident processing time
- AI analysis accuracy
- Reply success rate
- System uptime
- Error rates

## Support and Documentation

### Additional Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Microsoft Graph API](https://docs.microsoft.com/en-us/graph/overview)
- [Jira REST API](https://developer.atlassian.com/cloud/jira/platform/rest/v3/)
- [IBM Watsonx Documentation](https://www.ibm.com/docs/en/watsonx-as-a-service)

### Getting Help

For issues or questions:
1. Check the troubleshooting section
2. Review application logs
3. Consult the technical specification document
4. Contact the development team

---

**Document Version**: 1.0  
**Last Updated**: 2026-05-03  
**Status**: Production Ready