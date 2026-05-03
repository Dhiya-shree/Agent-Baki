-- ============================================
-- Agent Baki - Initial Database Schema
-- Version: 1.0
-- Description: Creates tables for application_data, fix, mail, and jira
-- ============================================

-- ============================================
-- Table: application_data
-- Description: Stores application information and repository links
-- ============================================
CREATE TABLE application_data (
    application_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_name VARCHAR(255) NOT NULL UNIQUE,
    repository_link VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_application_name UNIQUE (application_name)
);

-- Index for faster lookups by application name
CREATE INDEX idx_application_name ON application_data(application_name);

-- ============================================
-- Table: fix
-- Description: Tracks code issues with class name, line number, and status
-- ============================================
CREATE TABLE fix (
    fix_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    issue_summary VARCHAR(250),
    code_class_name VARCHAR(255),
    code_line INTEGER,
    github_pr VARCHAR(255),
    issue_status VARCHAR(50) DEFAULT 'PENDING',
    reason TEXT,
    change_number VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_fix_application FOREIGN KEY (application_id) 
        REFERENCES application_data(application_id) ON DELETE CASCADE,
    CONSTRAINT chk_issue_status CHECK (issue_status IN ('PENDING', 'IGNORED', 'IN_PROGRESS', 'DB_FIX', 'RESOLVED'))
);

-- Composite index for finding existing fixes by code location
CREATE INDEX idx_fix_code_location ON fix(application_id, code_class_name, code_line);

-- Index for filtering by status
CREATE INDEX idx_fix_status ON fix(issue_status);

-- ============================================
-- Table: mail
-- Description: Links email incidents to fixes and applications
-- ============================================
CREATE TABLE mail (
    mail_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fix_id BIGINT,
    application_id BIGINT,
    replied CHAR(1) DEFAULT 'N',
    log_location VARCHAR(500),
    email_subject VARCHAR(500),
    email_from VARCHAR(255),
    email_message_id VARCHAR(255) UNIQUE,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    replied_time TIMESTAMP,
    CONSTRAINT fk_mail_fix FOREIGN KEY (fix_id) 
        REFERENCES fix(fix_id) ON DELETE SET NULL,
    CONSTRAINT fk_mail_application FOREIGN KEY (application_id) 
        REFERENCES application_data(application_id) ON DELETE CASCADE,
    CONSTRAINT uk_email_message_id UNIQUE (email_message_id),
    CONSTRAINT chk_replied CHECK (replied IN ('Y', 'N'))
);

-- Index for batch job to find unreplied mails with fixes
CREATE INDEX idx_mail_replied_fix ON mail(replied, fix_id);

-- Index for faster lookups by application
CREATE INDEX idx_mail_application ON mail(application_id);

-- ============================================
-- Table: jira
-- Description: Links Jira incidents to fixes and applications
-- ============================================
CREATE TABLE jira (
    jira_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fix_id BIGINT,
    application_id BIGINT,
    replied CHAR(1) DEFAULT 'N',
    log_location VARCHAR(500),
    jira_ticket_key VARCHAR(50) UNIQUE,
    jira_summary VARCHAR(500),
    jira_issue_type VARCHAR(50),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    replied_time TIMESTAMP,
    CONSTRAINT fk_jira_fix FOREIGN KEY (fix_id) 
        REFERENCES fix(fix_id) ON DELETE SET NULL,
    CONSTRAINT fk_jira_application FOREIGN KEY (application_id) 
        REFERENCES application_data(application_id) ON DELETE CASCADE,
    CONSTRAINT uk_jira_ticket_key UNIQUE (jira_ticket_key),
    CONSTRAINT chk_jira_replied CHECK (replied IN ('Y', 'N'))
);

-- Index for batch job to find unreplied jira tickets with fixes
CREATE INDEX idx_jira_replied_fix ON jira(replied, fix_id);

-- Index for faster lookups by application
CREATE INDEX idx_jira_application ON jira(application_id);

-- ============================================
-- Comments for Documentation
-- ============================================
COMMENT ON TABLE application_data IS 'Stores application metadata and repository information';
COMMENT ON TABLE fix IS 'Tracks code issues identified from log analysis';
COMMENT ON TABLE mail IS 'Links email incidents to fixes with many-to-one relationship';
COMMENT ON TABLE jira IS 'Links Jira incidents to fixes with many-to-one relationship';

-- ============================================
-- Initial Data (Optional - for testing)
-- ============================================
-- Uncomment below to insert sample data for development/testing

-- INSERT INTO application_data (application_name, repository_link) 
-- VALUES ('SampleApp', 'C:/Users/Developer/repos/sampleapp');

-- INSERT INTO application_data (application_name, repository_link) 
-- VALUES ('WebService', 'C:/Users/Developer/repos/webservice');

-- Made with Bob
