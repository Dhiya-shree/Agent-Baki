# Technical Specification - Incident Management Automation System

## 1. Maven Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- H2 Database -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Microsoft Graph API for Outlook -->
    <dependency>
        <groupId>com.microsoft.graph</groupId>
        <artifactId>microsoft-graph</artifactId>
        <version>5.80.0</version>
    </dependency>
    <dependency>
        <groupId>com.azure</groupId>
        <artifactId>azure-identity</artifactId>
        <version>1.11.0</version>
    </dependency>
    
    <!-- Jira REST Client -->
    <dependency>
        <groupId>com.atlassian.jira</groupId>
        <artifactId>jira-rest-java-client-core</artifactId>
        <version>5.2.6</version>
    </dependency>
    
    <!-- HTTP Client -->
    <dependency>
        <groupId>org.apache.httpcomponents.client5</groupId>
        <artifactId>httpclient5</artifactId>
    </dependency>
    
    <!-- JSON Processing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 2. Entity Models

### Application.java
```java
@Entity
@Table(name = "application_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicationId;
    
    @Column(nullable = false, unique = true)
    private String applicationName;
    
    @Column(length = 500)
    private String repositoryLink;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    private List<Fix> fixes = new ArrayList<>();
    
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    private List<Mail> mails = new ArrayList<>();
    
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    private List<Jira> jiras = new ArrayList<>();
}
```

### Fix.java
```java
@Entity
@Table(name = "fix", indexes = {
    @Index(name = "idx_code_line", columnList = "applicationId,codeClassName,codeLine")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fix {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fixId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;
    
    @Column(length = 250)
    private String issueSummary;
    
    private String codeClassName;
    
    private Integer codeLine;
    
    private String githubPr;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private IssueStatus issueStatus = IssueStatus.PENDING;
    
    @Column(columnDefinition = "TEXT")
    private String ignoreReason;
    
    @Column(length = 100)
    private String dbFixNumber;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "fix", cascade = CascadeType.ALL)
    private List<Mail> mails = new ArrayList<>();
    
    @OneToMany(mappedBy = "fix", cascade = CascadeType.ALL)
    private List<Jira> jiras = new ArrayList<>();
}

enum IssueStatus {
    PENDING, IGNORED, IN_PROGRESS, DB_FIX, RESOLVED
}
```

### Mail.java
```java
@Entity
@Table(name = "mail", indexes = {
    @Index(name = "idx_replied", columnList = "replied,fixId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mailId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fix_id")
    private Fix fix;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;
    
    @Column(length = 1)
    private Character replied = 'N';
    
    @Column(length = 500)
    private String logLocation;
    
    @Column(length = 500)
    private String emailSubject;
    
    @Column(length = 255)
    private String emailFrom;
    
    @Column(unique = true)
    private String emailMessageId;
    
    @CreationTimestamp
    private LocalDateTime createdTime;
    
    private LocalDateTime repliedTime;
}
```

### Jira.java
```java
@Entity
@Table(name = "jira", indexes = {
    @Index(name = "idx_replied", columnList = "replied,fixId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Jira {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long jiraId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fix_id")
    private Fix fix;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;
    
    @Column(length = 1)
    private Character replied = 'N';
    
    @Column(length = 500)
    private String logLocation;
    
    @Column(unique = true, length = 50)
    private String jiraTicketKey;
    
    @Column(length = 500)
    private String jiraSummary;
    
    @Column(length = 50)
    private String jiraIssueType;
    
    @CreationTimestamp
    private LocalDateTime createdTime;
    
    private LocalDateTime repliedTime;
}
```

## 3. Repository Interfaces

```java
@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Optional<Application> findByApplicationName(String applicationName);
    boolean existsByApplicationName(String applicationName);
}

@Repository
public interface FixRepository extends JpaRepository<Fix, Long> {
    List<Fix> findByApplication(Application application);
    
    Optional<Fix> findByApplicationAndCodeClassNameAndCodeLine(
        Application application, String codeClassName, Integer codeLine);
    
    List<Fix> findByIssueStatus(IssueStatus status);
}

@Repository
public interface MailRepository extends JpaRepository<Mail, Long> {
    List<Mail> findByRepliedAndFixIsNotNull(Character replied);
    Optional<Mail> findByEmailMessageId(String messageId);
}

@Repository
public interface JiraRepository extends JpaRepository<Jira, Long> {
    List<Jira> findByRepliedAndFixIsNotNull(Character replied);
    Optional<Jira> findByJiraTicketKey(String ticketKey);
}
```

## 4. Service Layer Implementation

### OutlookCollectorService.java
```java
@Service
@Slf4j
public class OutlookCollectorService {
    
    @Value("${outlook.client-id}")
    private String clientId;
    
    @Value("${outlook.client-secret}")
    private String clientSecret;
    
    @Value("${outlook.tenant-id}")
    private String tenantId;
    
    private GraphServiceClient<Request> graphClient;
    
    @PostConstruct
    public void init() {
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
            .clientId(clientId)
            .clientSecret(clientSecret)
            .tenantId(tenantId)
            .build();
            
        graphClient = GraphServiceClient.builder()
            .authenticationProvider(new TokenCredentialAuthProvider(credential))
            .buildClient();
    }
    
    public List<Message> fetchIncidentEmails() {
        try {
            MessageCollectionPage messages = graphClient.me()
                .messages()
                .buildRequest()
                .filter("contains(subject, 'Issue') or contains(subject, 'Ticket') or contains(subject, 'Incident')")
                .select("id,subject,from,body,receivedDateTime")
                .top(50)
                .get();
                
            return messages.getCurrentPage();
        } catch (Exception e) {
            log.error("Error fetching emails from Outlook", e);
            throw new RuntimeException("Failed to fetch emails", e);
        }
    }
    
    public void sendReply(String messageId, String replyContent) {
        try {
            Message reply = new Message();
            ItemBody body = new ItemBody();
            body.contentType = BodyType.TEXT;
            body.content = replyContent;
            reply.body = body;
            
            graphClient.me()
                .messages(messageId)
                .reply(reply)
                .buildRequest()
                .post();
                
            log.info("Reply sent successfully for message: {}", messageId);
        } catch (Exception e) {
            log.error("Error sending reply to email", e);
            throw new RuntimeException("Failed to send reply", e);
        }
    }
}
```

### JiraCollectorService.java
```java
@Service
@Slf4j
public class JiraCollectorService {
    
    @Value("${jira.base-url}")
    private String jiraBaseUrl;
    
    @Value("${jira.username}")
    private String username;
    
    @Value("${jira.api-token}")
    private String apiToken;
    
    private JiraRestClient jiraClient;
    
    @PostConstruct
    public void init() {
        URI jiraUri = URI.create(jiraBaseUrl);
        jiraClient = new AsynchronousJiraRestClientFactory()
            .createWithBasicHttpAuthentication(jiraUri, username, apiToken);
    }
    
    public List<Issue> fetchIncidentIssues() {
        try {
            SearchResult searchResult = jiraClient.getSearchClient()
                .searchJql("type = Incident OR type = Bug ORDER BY created DESC", 50, 0, null)
                .claim();
                
            return StreamSupport.stream(searchResult.getIssues().spliterator(), false)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching issues from Jira", e);
            throw new RuntimeException("Failed to fetch Jira issues", e);
        }
    }
    
    public void addComment(String issueKey, String comment) {
        try {
            Issue issue = jiraClient.getIssueClient()
                .getIssue(issueKey)
                .claim();
                
            jiraClient.getIssueClient()
                .addComment(issue.getCommentsUri(), Comment.valueOf(comment))
                .claim();
                
            log.info("Comment added successfully to issue: {}", issueKey);
        } catch (Exception e) {
            log.error("Error adding comment to Jira issue", e);
            throw new RuntimeException("Failed to add comment", e);
        }
    }
}
```

### IncidentParserService.java
```java
@Service
@Slf4j
public class IncidentParserService {
    
    private static final Pattern APP_NAME_PATTERN = 
        Pattern.compile("(?i)application[:\\s]+([a-zA-Z0-9_-]+)");
    
    public ParsedIncident parseEmail(Message message) {
        String subject = message.subject;
        String body = message.body.content;
        String from = message.from.emailAddress.address;
        
        ParsedIncident incident = new ParsedIncident();
        incident.setSource("EMAIL");
        incident.setSubject(subject);
        incident.setContent(body);
        incident.setFrom(from);
        incident.setMessageId(message.id);
        
        // Extract application name
        String appName = extractApplicationName(body);
        incident.setApplicationName(appName);
        
        // Extract logs
        String logs = extractLogs(body);
        incident.setLogs(logs);
        
        return incident;
    }
    
    public ParsedIncident parseJiraIssue(Issue issue) {
        ParsedIncident incident = new ParsedIncident();
        incident.setSource("JIRA");
        incident.setSubject(issue.getSummary());
        incident.setContent(issue.getDescription());
        incident.setTicketKey(issue.getKey());
        incident.setIssueType(issue.getIssueType().getName());
        
        // Extract application name
        String appName = extractApplicationName(issue.getDescription());
        incident.setApplicationName(appName);
        
        // Extract logs
        String logs = extractLogs(issue.getDescription());
        incident.setLogs(logs);
        
        return incident;
    }
    
    private String extractApplicationName(String content) {
        Matcher matcher = APP_NAME_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private String extractLogs(String content) {
        // Look for log patterns (stack traces, error messages, etc.)
        if (content.contains("Exception") || content.contains("Error") || 
            content.contains("at ")) {
            return content;
        }
        return null;
    }
    
    public boolean isValidIncident(ParsedIncident incident) {
        return incident.getApplicationName() != null && 
               incident.getLogs() != null;
    }
}
```

### FileSystemService.java
```java
@Service
@Slf4j
public class FileSystemService {
    
    @Value("${app.storage.base-path}")
    private String basePath;
    
    public String createLogDirectory(String applicationName, String incidentId) {
        Path logPath = Paths.get(basePath, applicationName, "issues", incidentId, "logs");
        
        try {
            Files.createDirectories(logPath);
            log.info("Created log directory: {}", logPath);
            return logPath.toString();
        } catch (IOException e) {
            log.error("Error creating log directory", e);
            throw new RuntimeException("Failed to create log directory", e);
        }
    }
    
    public void saveLogs(String logLocation, String logs) {
        Path logFile = Paths.get(logLocation, "incident.log");
        
        try {
            Files.writeString(logFile, logs, StandardCharsets.UTF_8);
            log.info("Saved logs to: {}", logFile);
        } catch (IOException e) {
            log.error("Error saving logs", e);
            throw new RuntimeException("Failed to save logs", e);
        }
    }
    
    public String readLogs(String logLocation) {
        Path logFile = Paths.get(logLocation, "incident.log");
        
        try {
            return Files.readString(logFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error reading logs", e);
            throw new RuntimeException("Failed to read logs", e);
        }
    }
    
    public void deleteLogDirectory(String logLocation) {
        Path logPath = Paths.get(logLocation).getParent();
        
        try {
            Files.walk(logPath)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.warn("Failed to delete: {}", path, e);
                    }
                });
            log.info("Deleted log directory: {}", logPath);
        } catch (IOException e) {
            log.error("Error deleting log directory", e);
        }
    }
}
```

### WatsonxAIService.java
```java
@Service
@Slf4j
public class WatsonxAIService {
    
    @Value("${watsonx.api-key}")
    private String apiKey;
    
    @Value("${watsonx.project-id}")
    private String projectId;
    
    @Value("${watsonx.endpoint}")
    private String endpoint;
    
    private final RestTemplate restTemplate;
    
    public WatsonxAIService() {
        this.restTemplate = new RestTemplate();
    }
    
    public AnalysisResult analyzeLogs(String logs, String repositoryPath) {
        try {
            String prompt = buildAnalysisPrompt(logs, repositoryPath);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            Map<String, Object> request = new HashMap<>();
            request.put("project_id", projectId);
            request.put("input", prompt);
            request.put("parameters", Map.of(
                "max_new_tokens", 500,
                "temperature", 0.1
            ));
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                endpoint + "/ml/v1/text/generation",
                entity,
                Map.class
            );
            
            return parseAnalysisResponse(response.getBody());
            
        } catch (Exception e) {
            log.error("Error analyzing logs with Watsonx AI", e);
            throw new RuntimeException("Failed to analyze logs", e);
        }
    }
    
    private String buildAnalysisPrompt(String logs, String repositoryPath) {
        return String.format("""
            Analyze the following error logs and identify the exact code location causing the issue.
            
            Repository Path: %s
            
            Error Logs:
            %s
            
            Please provide:
            1. The exact class name where the error occurs
            2. The line number in that class
            3. A brief summary of the issue (max 250 characters)
            
            Format your response as JSON:
            {
                "className": "com.example.ClassName",
                "lineNumber": 123,
                "summary": "Brief description of the issue"
            }
            """, repositoryPath, logs);
    }
    
    private AnalysisResult parseAnalysisResponse(Map<String, Object> response) {
        // Parse the AI response and extract structured data
        String generatedText = (String) ((Map) ((List) response.get("results")).get(0))
            .get("generated_text");
            
        // Parse JSON from generated text
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(generatedText, AnalysisResult.class);
        } catch (Exception e) {
            log.error("Error parsing AI response", e);
            throw new RuntimeException("Failed to parse AI response", e);
        }
    }
}

@Data
class AnalysisResult {
    private String className;
    private Integer lineNumber;
    private String summary;
}
```

### LogAnalysisService.java
```java
@Service
@Slf4j
@Transactional
public class LogAnalysisService {
    
    @Autowired
    private FileSystemService fileSystemService;
    
    @Autowired
    private WatsonxAIService watsonxAIService;
    
    @Autowired
    private FixLookupService fixLookupService;
    
    @Autowired
    private ApplicationRepository applicationRepository;
    
    @Autowired
    private MailRepository mailRepository;
    
    @Autowired
    private JiraRepository jiraRepository;
    
    public void processIncident(ParsedIncident incident) {
        // Get or create application
        Application application = getOrCreateApplication(incident.getApplicationName());
        
        // Create log directory
        String incidentId = incident.getSource().equals("EMAIL") ? 
            "mail_" + incident.getMessageId() : 
            "jira_" + incident.getTicketKey();
            
        String logLocation = fileSystemService.createLogDirectory(
            application.getApplicationName(), incidentId);
        
        // Save logs
        fileSystemService.saveLogs(logLocation, incident.getLogs());
        
        // Analyze logs with AI
        AnalysisResult analysis = watsonxAIService.analyzeLogs(
            incident.getLogs(), application.getRepositoryLink());
        
        // Lookup or create fix
        Fix fix = fixLookupService.findOrCreateFix(
            application, analysis.getClassName(), 
            analysis.getLineNumber(), analysis.getSummary());
        
        // Create mail or jira entry
        if (incident.getSource().equals("EMAIL")) {
            createMailEntry(incident, application, fix, logLocation);
        } else {
            createJiraEntry(incident, application, fix, logLocation);
        }
    }
    
    private Application getOrCreateApplication(String appName) {
        return applicationRepository.findByApplicationName(appName)
            .orElseGet(() -> {
                Application app = new Application();
                app.setApplicationName(appName);
                return applicationRepository.save(app);
            });
    }
    
    private void createMailEntry(ParsedIncident incident, Application app, 
                                  Fix fix, String logLocation) {
        Mail mail = new Mail();
        mail.setApplication(app);
        mail.setFix(fix);
        mail.setLogLocation(logLocation);
        mail.setEmailSubject(incident.getSubject());
        mail.setEmailFrom(incident.getFrom());
        mail.setEmailMessageId(incident.getMessageId());
        mailRepository.save(mail);
    }
    
    private void createJiraEntry(ParsedIncident incident, Application app, 
                                  Fix fix, String logLocation) {
        Jira jira = new Jira();
        jira.setApplication(app);
        jira.setFix(fix);
        jira.setLogLocation(logLocation);
        jira.setJiraTicketKey(incident.getTicketKey());
        jira.setJiraSummary(incident.getSubject());
        jira.setJiraIssueType(incident.getIssueType());
        jiraRepository.save(jira);
    }
}
```

### FixLookupService.java
```java
@Service
@Slf4j
public class FixLookupService {
    
    @Autowired
    private FixRepository fixRepository;
    
    public Fix findOrCreateFix(Application application, String className, 
                                Integer lineNumber, String summary) {
        // Check if fix already exists for this code location
        Optional<Fix> existingFix = fixRepository
            .findByApplicationAndCodeClassNameAndCodeLine(
                application, className, lineNumber);
        
        if (existingFix.isPresent()) {
            log.info("Found existing fix for {}:{}", className, lineNumber);
            return existingFix.get();
        }
        
        // Create new fix
        Fix newFix = new Fix();
        newFix.setApplication(application);
        newFix.setCodeClassName(className);
        newFix.setCodeLine(lineNumber);
        newFix.setIssueSummary(summary);
        newFix.setIssueStatus(IssueStatus.PENDING);
        
        Fix savedFix = fixRepository.save(newFix);
        log.info("Created new fix with ID: {}", savedFix.getFixId());
        
        return savedFix;
    }
}
```

### BatchReplyService.java
```java
@Service
@Slf4j
public class BatchReplyService {
    
    @Autowired
    private MailRepository mailRepository;
    
    @Autowired
    private JiraRepository jiraRepository;
    
    @Autowired
    private OutlookCollectorService outlookService;
    
    @Autowired
    private JiraCollectorService jiraService;
    
    @Autowired
    private FileSystemService fileSystemService;
    
    @Scheduled(cron = "${batch.reply.cron}")
    @Transactional
    public void processUnrepliedIncidents() {
        log.info("Starting batch reply process");
        
        processUnrepliedMails();
        processUnrepliedJiras();
        
        log.info("Batch reply process completed");
    }
    
    private void processUnrepliedMails() {
        List<Mail> unrepliedMails = mailRepository.findByRepliedAndFixIsNotNull('N');
        
        for (Mail mail : unrepliedMails) {
            try {
                String replyContent = buildReplyContent(mail.getFix());
                outlookService.sendReply(mail.getEmailMessageId(), replyContent);
                
                // Update mail status
                mail.setReplied('Y');
                mail.setRepliedTime(LocalDateTime.now());
                mailRepository.save(mail);
                
                // Delete logs
                fileSystemService.deleteLogDirectory(mail.getLogLocation());
                
                log.info("Processed mail ID: {}", mail.getMailId());
            } catch (Exception e) {
                log.error("Error processing mail ID: {}", mail.getMailId(), e);
            }
        }
    }
    
    private void processUnrepliedJiras() {
        List<Jira> unrepliedJiras = jiraRepository.findByRepliedAndFixIsNotNull('N');
        
        for (Jira jira : unrepliedJiras) {
            try {
                String comment = buildReplyContent(jira.getFix());
                jiraService.addComment(jira.getJiraTicketKey(), comment);
                
                // Update jira status
                jira.setReplied('Y');
                jira.setRepliedTime(LocalDateTime.now());
                jiraRepository.save(jira);
                
                // Delete logs
                fileSystemService.deleteLogDirectory(jira.getLogLocation());
                
                log.info("Processed Jira ticket: {}", jira.getJiraTicketKey());
            } catch (Exception e) {
                log.error("Error processing Jira ticket: {}", jira.getJiraTicketKey(), e);
            }
        }
    }
    
    private String buildReplyContent(Fix fix) {
        StringBuilder content = new StringBuilder();
        content.append("Issue Analysis Complete\n\n");
        content.append("Application: ").append(fix.getApplication().getApplicationName()).append("\n");
        content.append("Issue Location: ").append(fix.getCodeClassName())
               .append(" (Line ").append(fix.getCodeLine()).append(")\n");
        content.append("Summary: ").append(fix.getIssueSummary()).append("\n\n");
        
        switch (fix.getIssueStatus()) {
            case IN_PROGRESS:
                content.append("Status: Fix in progress\n");
                if (fix.getGithubPr() != null) {
                    content.append("PR: ").append(fix.getGithubPr()).append("\n");
                }
                break;
            case DB_FIX:
                content.append("Status: Database fix applied\n");
                if (fix.getDbFixNumber() != null) {
                    content.append("Fix Number: ").append(fix.getDbFixNumber()).append("\n");
                }
                break;
            case IGNORED:
                content.append("Status: Issue ignored\n");
                if (fix.getIgnoreReason() != null) {
                    content.append("Reason: ").append(fix.getIgnoreReason()).append("\n");
                }
                break;
            default:
                content.append("Status: Pending review\n");
        }
        
        return content.toString();
    }
}
```

## 5. Controller Layer

### ApplicationController.java
```java
@Controller
@RequestMapping("/applications")
public class ApplicationController {
    
    @Autowired
    private ApplicationRepository applicationRepository;
    
    @Autowired
    private FixRepository fixRepository;
    
    @GetMapping
    public String listApplications(Model model) {
        List<Application> applications = applicationRepository.findAll();
        model.addAttribute("applications", applications);
        return "applications";
    }
    
    @GetMapping("/{id}/fixes")
    public String viewFixes(@PathVariable Long id, Model model) {
        Application application = applicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        
        List<Fix> fixes = fixRepository.findByApplication(application);
        
        model.addAttribute("application", application);
        model.addAttribute("fixes", fixes);
        return "fix-details";
    }
}
```

### FixController.java
```java
@RestController
@RequestMapping("/api/fixes")
public class FixController {
    
    @Autowired
    private FixRepository fixRepository;
    
    @PostMapping("/{id}/ignore")
    public ResponseEntity<Fix> ignoreFix(@PathVariable Long id, 
                                         @RequestBody ActionRequest request) {
        Fix fix = fixRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fix not found"));
        
        fix.setIssueStatus(IssueStatus.IGNORED);
        fix.setIgnoreReason(request.getReason());
        
        Fix updated = fixRepository.save(fix);
        return ResponseEntity.ok(updated);
    }
    
    @PostMapping("/{id}/in-progress")
    public ResponseEntity<Fix> markInProgress(@PathVariable Long id, 
                                              @RequestBody ActionRequest request) {
        Fix fix = fixRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fix not found"));
        
        fix.setIssueStatus(IssueStatus.IN_PROGRESS);
        fix.setGithubPr(request.getPrNumber());
        
        Fix updated = fixRepository.save(fix);
        return ResponseEntity.ok(updated);
    }
    
    @PostMapping("/{id}/db-fix")
    public ResponseEntity<Fix> markDbFix(@PathVariable Long id, 
                                         @RequestBody ActionRequest request) {
        Fix fix = fixRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fix not found"));
        
        fix.setIssueStatus(IssueStatus.DB_FIX);
        fix.setDbFixNumber(request.getFixNumber());
        
        Fix updated = fixRepository.save(fix);
        return ResponseEntity.ok(updated);
    }
}

@Data
class ActionRequest {
    private String reason;
    private String prNumber;
    private String fixNumber;
}
```

## 6. Thymeleaf Templates

### applications.html
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Applications - Incident Management</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div class="container mt-5">
        <h1>Applications</h1>
        
        <table class="table table-striped mt-4">
            <thead>
                <tr>
                    <th>Application Name</th>
                    <th>Repository</th>
                    <th>Open Issues</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="app : ${applications}">
                    <td th:text="${app.applicationName}"></td>
                    <td th:text="${app.repositoryLink}"></td>
                    <td th:text="${#lists.size(app.fixes)}"></td>
                    <td>
                        <a th:href="@{/applications/{id}/fixes(id=${app.applicationId})}" 
                           class="btn btn-primary btn-sm">View Fixes</a>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</body>
</html>
```

### fix-details.html
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Fix Details - Incident Management</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div class="container mt-5">
        <h1 th:text="${application.applicationName}"></h1>
        
        <div class="mt-4">
            <div class="card mb-3" th:each="fix : ${fixes}">
                <div class="card-header">
                    <strong th:text="${fix.codeClassName}"></strong> 
                    (Line <span th:text="${fix.codeLine}"></span>)
                </div>
                <div class="card-body">
                    <p th:text="${fix.issueSummary}"></p>
                    <p><strong>Status:</strong> <span th:text="${fix.issueStatus}"></span></p>
                    
                    <div class="btn-group" role="group">
                        <button type="button" class="btn btn-warning" 
                                th:onclick="'showIgnoreModal(' + ${fix.fixId} + ')'">
                            Ignore
                        </button>
                        <button type="button" class="btn btn-primary" 
                                th:onclick="'showInProgressModal(' + ${fix.fixId} + ')'">
                            Issue in Progress
                        </button>
                        <button type="button" class="btn btn-success" 
                                th:onclick="'showDbFixModal(' + ${fix.fixId} + ')'">
                            DB Fix
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script th:src="@{/js/app.js}"></script>
</body>
</html>
```

## 7. Configuration

### application.properties
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
outlook.client-id=${OUTLOOK_CLIENT_ID:your-client-id}
outlook.client-secret=${OUTLOOK_CLIENT_SECRET:your-client-secret}
outlook.tenant-id=${OUTLOOK_TENANT_ID:your-tenant-id}

# Jira
jira.base-url=${JIRA_BASE_URL:https://your-domain.atlassian.net}
jira.username=${JIRA_USERNAME:your-email}
jira.api-token=${JIRA_API_TOKEN:your-api-token}

# Watsonx AI
watsonx.api-key=${WATSONX_API_KEY:your-api-key}
watsonx.project-id=${WATSONX_PROJECT_ID:your-project-id}
watsonx.endpoint=${WATSONX_ENDPOINT:https://us-south.ml.cloud.ibm.com}

# Batch Job
batch.reply.cron=0 0 */2 * * ?

# Logging
logging.level.root=INFO
logging.level.com.incident.management=DEBUG
```

---

**Document Version**: 1.0  
**Last Updated**: 2026-05-03  
**Status**: Ready for Implementation