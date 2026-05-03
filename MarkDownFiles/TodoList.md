# Agent Baki - Implementation Todo List

**Project**: Incident Management Automation System for Developers  
**Team**: Team Baki  
**Last Updated**: 2026-05-03  
**Progress**: 4/27 Tasks Complete (15%)

---

## Phase 1: Foundation & Data Layer ✅ COMPLETE

- [x] **Task 1**: Set up Spring Boot project with dependencies
- [x] **Task 2**: Design database schema with Flyway migration
- [x] **Task 3**: Create JPA entities (Application, Fix, Mail, Jira, IssueStatus)
- [x] **Task 4**: Implement repository interfaces

**Files Created**: `pom.xml`, `BakiApplication.java`, `application.properties`, `V1__Create_Initial_Schema.sql`, 4 entities, 4 repositories

---

## Phase 2: Configuration & Integration Services ⏳ PENDING

- [ ] **Task 5**: Create configuration service for Outlook and Jira credentials
- [ ] **Task 6**: Build Outlook integration service using Microsoft Graph API
- [ ] **Task 7**: Implement email fetching logic for Issues/Ticket/Incident subjects
- [ ] **Task 8**: Build Jira REST API integration service

**Target Files**: `config/OutlookConfig.java`, `config/JiraConfig.java`, `service/integration/OutlookService.java`, `service/integration/JiraService.java`

---

## Phase 3: Data Processing Services ⏳ PENDING

- [ ] **Task 9**: Create incident parser to extract application name and logs
- [ ] **Task 10**: Implement automated reply for missing data
- [ ] **Task 11**: Build file system service for log folder structure
- [ ] **Task 12**: Create service to save logs to file system
- [ ] **Task 13**: Integrate Watsonx AI API client
- [ ] **Task 14**: Build log analysis service for code location identification
- [ ] **Task 15**: Implement fix lookup logic (deduplication)
- [ ] **Task 16**: Create fix management service
- [ ] **Task 17**: Build incident linking service

**Target Files**: `service/parser/`, `service/storage/`, `service/ai/`, `service/analysis/`, `service/fix/`, `service/incident/`

---

## Phase 4: User Interface ⏳ PENDING

- [ ] **Task 18**: Design Thymeleaf UI (dashboard, applications, fix-details)
- [ ] **Task 19**: Create REST controllers for application and fix management
- [ ] **Task 20**: Implement UI action handlers (Ignore, In Progress, DB Fix)

**Target Files**: `templates/*.html`, `controller/ApplicationController.java`, `controller/FixController.java`, `controller/DashboardController.java`

---

## Phase 5: Batch Processing & Automation ⏳ PENDING

- [ ] **Task 21**: Build batch job scheduler (@Scheduled, cron: every 2 hours)
- [ ] **Task 22**: Implement batch reply service for Outlook emails
- [ ] **Task 23**: Implement batch comment service for Jira incidents
- [ ] **Task 24**: Create file cleanup service

**Target Files**: `service/batch/BatchJobScheduler.java`, `service/batch/EmailReplyService.java`, `service/batch/JiraCommentService.java`, `service/batch/FileCleanupService.java`

---

## Phase 6: Quality & Documentation ⏳ PENDING

- [ ] **Task 25**: Add transaction management and error handling
- [ ] **Task 26**: Implement logging and audit trail
- [ ] **Task 27**: Create API documentation and user setup guide

**Target Files**: `config/TransactionConfig.java`, `exception/GlobalExceptionHandler.java`, `service/audit/AuditService.java`, `API_DOCUMENTATION.md`, `USER_GUIDE.md`

---

## Key Implementation Notes

### Completed Work
1. Database schema with proper indexes and foreign keys
2. Entity relationships with bidirectional helpers
3. Deduplication support via code location lookup
4. Batch job queries for unreplied incidents

### Design Decisions
- Field naming: `changeNumber` (not dbFixNumber), `reason` (not ignoreReason)
- Cron schedule: Every 2 hours (`0 0 */2 * * ?`)
- Storage: File system for logs, database for metadata
- AI: Watsonx AI primary, Spring AI for MCP support

### Next Session Priority
**Phase 2: Configuration & Integration Services**
- Create config classes for Outlook, Jira, Watsonx
- Implement integration services
- Follow Write.md rules: Services use method names with "BobAndMe TODO" comments

---

## Related Documents

- `IMPLEMENTATION_PLAN.md` - Architecture and design
- `TECHNICAL_SPECIFICATION.md` - Code specifications
- `SETUP_GUIDE.md` - Installation guide
- `README.md` - Project overview
- `PlanDraft.md` - Original requirements
- `Write.md` - Coding rules

---

## Project Structure

```
baki/
├── pom.xml ✅
├── src/main/
│   ├── java/com/agent/baki/
│   │   ├── BakiApplication.java ✅
│   │   ├── entity/ ✅ (5 files)
│   │   ├── repository/ ✅ (4 files)
│   │   ├── config/ ⏳
│   │   ├── service/ ⏳
│   │   ├── controller/ ⏳
│   │   ├── dto/ ⏳
│   │   └── exception/ ⏳
│   └── resources/
│       ├── application.properties ✅
│       ├── db/migration/ ✅ (V1 migration)
│       ├── templates/ ⏳
│       └── static/ (Bootstrap CSS ready)
└── src/test/ ⏳
```

---

**Status**: Ready for Phase 2 Implementation  
**Estimated Time**: 12 weeks total (as per IMPLEMENTATION_PLAN.md)