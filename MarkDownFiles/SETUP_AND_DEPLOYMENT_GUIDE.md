# Baki Application Management - Setup and Deployment Guide

This guide covers the complete setup for the REST API backend and React frontend.

## Overview

The system has been upgraded with:
- ✅ REST API with ResponseEntity
- ✅ Validation on all entities and DTOs
- ✅ Global exception handler for user-friendly error messages
- ✅ CORS configuration for React frontend
- ✅ Modern React UI with issue management

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     React Frontend                          │
│                   (http://localhost:3000)                   │
│  - Application List                                         │
│  - Application Details                                      │
│  - Issue Management (Ignore, DB Fix, Resolved, In Progress)│
└─────────────────────────────────────────────────────────────┘
                            │
                            │ REST API Calls
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  Spring Boot Backend                        │
│                   (http://localhost:8080)                   │
│  - REST Controller (ApplicationRestController)              │
│  - DTOs with Validation                                     │
│  - Global Exception Handler                                 │
│  - CORS Configuration                                       │
│  - Original Thymeleaf Controller (still available)          │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      Database (H2/PostgreSQL)               │
└─────────────────────────────────────────────────────────────┘
```

## Backend Setup

### 1. New Files Created

**DTOs:**
- `baki/src/main/java/com/agent/baki/dto/ApplicationDTO.java`
- `baki/src/main/java/com/agent/baki/dto/FixDTO.java`
- `baki/src/main/java/com/agent/baki/dto/FixUpdateRequest.java`

**Exception Handling:**
- `baki/src/main/java/com/agent/baki/exception/GlobalExceptionHandler.java`
- `baki/src/main/java/com/agent/baki/exception/ErrorResponse.java`
- `baki/src/main/java/com/agent/baki/exception/ResourceNotFoundException.java`

**Mapper:**
- `baki/src/main/java/com/agent/baki/mapper/ApplicationMapper.java`

**REST Controller:**
- `baki/src/main/java/com/agent/baki/controller/ApplicationRestController.java`

**Configuration:**
- `baki/src/main/java/com/agent/baki/config/CorsConfig.java`

### 2. Backend Dependencies

Ensure these dependencies are in `baki/pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### 3. Running the Backend

```bash
cd baki
mvn clean install
mvn spring-boot:run
```

The backend will start on http://localhost:8080

### 4. REST API Endpoints

**Applications:**
- `GET /api/applications` - Get all applications with statistics
- `GET /api/applications/{id}` - Get application details with all fixes

**Fixes:**
- `GET /api/fixes/{id}` - Get fix details
- `PUT /api/fixes/{id}/ignore` - Mark as ignored (requires reason)
- `PUT /api/fixes/{id}/in-progress` - Mark as in progress (requires githubPr)
- `PUT /api/fixes/{id}/db-fix` - Mark as DB fix (requires changeNumber)
- `PUT /api/fixes/{id}/resolved` - Mark as resolved

**Health:**
- `GET /api/health` - API health check

### 5. Testing Backend API

Using curl:

```bash
# Get all applications
curl http://localhost:8080/api/applications

# Get application by ID
curl http://localhost:8080/api/applications/1

# Mark fix as ignored
curl -X PUT http://localhost:8080/api/fixes/1/ignore \
  -H "Content-Type: application/json" \
  -d '{"reason": "Duplicate issue"}'

# Mark fix as in progress
curl -X PUT http://localhost:8080/api/fixes/1/in-progress \
  -H "Content-Type: application/json" \
  -d '{"githubPr": "PR-123"}'

# Mark fix as DB fix
curl -X PUT http://localhost:8080/api/fixes/1/db-fix \
  -H "Content-Type: application/json" \
  -d '{"changeNumber": "CHG-456", "reason": "Schema update"}'

# Mark fix as resolved
curl -X PUT http://localhost:8080/api/fixes/1/resolved
```

## Frontend Setup

### 1. Install Node.js

Download and install Node.js from https://nodejs.org/ (v14 or higher)

### 2. Install Dependencies

```bash
cd frontend
npm install
```

### 3. Start Development Server

```bash
npm start
```

The frontend will open at http://localhost:3000

### 4. Frontend Structure

```
frontend/
├── public/
│   └── index.html
├── src/
│   ├── components/
│   │   ├── ApplicationList.js       # Grid of application cards
│   │   ├── ApplicationList.css
│   │   ├── ApplicationDetails.js    # Detailed view with grouped issues
│   │   ├── ApplicationDetails.css
│   │   ├── FixItem.js               # Individual issue with 4 action buttons
│   │   └── FixItem.css
│   ├── services/
│   │   └── api.js                   # Axios API service
│   ├── App.js                       # Main component
│   ├── App.css
│   ├── index.js
│   └── index.css
├── package.json
└── README.md
```

## User Guide

### Viewing Applications

1. Open http://localhost:3000
2. You'll see a grid of application cards showing:
   - Application name
   - Repository link
   - Issue counts by status (Pending, In Progress, Resolved, Ignored, DB Fix)

### Managing Issues

1. Click on any application card
2. Issues are grouped by status
3. Each issue shows:
   - Summary
   - Code location (class and line number)
   - Email and Jira counts
   - Current status badge

### Issue Actions

Each issue (except resolved ones) has 4 action buttons:

**1. Ignore Button (🚫)**
- Click to mark issue as ignored
- Enter a reason in the text field
- Click Submit

**2. DB Fix Button (💾)**
- Click to mark as database fix
- Enter change number (required)
- Optionally enter a reason
- Click Submit

**3. Resolved Button (✅)**
- Click to mark issue as resolved
- No additional input required
- Confirms immediately

**4. In Progress Button (🔄)**
- Click to mark as in progress
- Enter GitHub PR number or URL
- Click Submit

## Validation Rules

### Application
- `applicationName`: Required, max 255 characters, must be unique
- `repositoryLink`: Optional, max 500 characters

### Fix
- `applicationId`: Required
- `issueSummary`: Max 250 characters
- `codeClassName`: Max 255 characters
- `githubPr`: Max 255 characters
- `changeNumber`: Max 100 characters
- `issueStatus`: Required, must be valid enum value

### Fix Update Request
- `reason`: Optional for ignore and DB fix
- `githubPr`: Required for in-progress
- `changeNumber`: Required for DB fix

## Error Handling

The system provides user-friendly error messages for:
- Validation errors (400 Bad Request)
- Resource not found (404 Not Found)
- Illegal state (409 Conflict) - e.g., updating resolved fix
- Server errors (500 Internal Server Error)

Example error response:
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "reason": "Reason is required"
  },
  "timestamp": "2026-05-03T14:30:00"
}
```

## Deployment

### Backend Deployment

1. Build the JAR:
```bash
cd baki
mvn clean package
```

2. Run the JAR:
```bash
java -jar target/baki-0.0.1-SNAPSHOT.jar
```

### Frontend Deployment

1. Build for production:
```bash
cd frontend
npm run build
```

2. Deploy the `build` folder to:
   - Static hosting (Netlify, Vercel, GitHub Pages)
   - Nginx/Apache web server
   - AWS S3 + CloudFront
   - Azure Static Web Apps

3. Update API URL in production:
   - Edit `frontend/src/services/api.js`
   - Change `API_BASE_URL` to production backend URL

## Troubleshooting

### CORS Issues
- Ensure `CorsConfig.java` includes your frontend URL
- For production, update allowed origins in `CorsConfig`

### Port Conflicts
- Backend: Change port in `application.properties`: `server.port=8081`
- Frontend: Set PORT environment variable: `PORT=3001 npm start`

### Database Connection
- Check `application.properties` for database configuration
- Ensure database is running and accessible

### Build Errors
- Clear Maven cache: `mvn clean`
- Clear npm cache: `npm cache clean --force`
- Delete `node_modules` and reinstall: `rm -rf node_modules && npm install`

## Monitoring

### Backend Logs
- Application logs: `baki/logs/baki-application.log`
- Error logs: `baki/logs/baki-error.log`

### Frontend Console
- Open browser DevTools (F12)
- Check Console tab for errors
- Check Network tab for API calls

## Security Considerations

1. **CORS**: Update allowed origins for production
2. **Validation**: All inputs are validated on backend
3. **Error Messages**: Sensitive information is not exposed
4. **Authentication**: Consider adding Spring Security for production
5. **HTTPS**: Use HTTPS in production for both frontend and backend

## Next Steps

1. Add authentication and authorization
2. Implement pagination for large datasets
3. Add search and filter functionality
4. Implement real-time updates with WebSocket
5. Add unit and integration tests
6. Set up CI/CD pipeline
7. Add monitoring and logging (ELK stack, Prometheus)

## Support

For issues or questions:
- Check logs in `baki/logs/`
- Review browser console for frontend errors
- Verify API endpoints with curl or Postman
- Ensure both backend and frontend are running

---

© 2026 Baki Application Management System