-- ============================================================================
-- Test Data Insert Scripts for Baki Application
-- ============================================================================
-- This file contains sample insert statements for all entities to test the database
-- Execute these scripts after running the migration V1__Create_Initial_Schema.sql
-- 
-- Entities covered:
-- 1. Application (application_data table)
-- 2. Fix (fix table)
-- 3. Mail (mail table)
-- 4. Jira (jira table)
-- ============================================================================

-- ============================================================================
-- 1. APPLICATION TEST DATA
-- ============================================================================
-- Insert sample applications with different repository configurations

INSERT INTO application_data (application_name, repository_link, created_at, updated_at)
VALUES 
    ('UserService', 'https://github.com/company/user-service.git', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PaymentGateway', 'C:/repos/payment-gateway', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('InventorySystem', 'https://github.com/company/inventory-system.git', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('NotificationService', '/home/dev/projects/notification-service', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('AuthenticationAPI', 'https://gitlab.com/company/auth-api.git', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================================
-- 2. FIX TEST DATA
-- ============================================================================
-- Insert sample fixes with various statuses and scenarios

-- Fixes for UserService (application_id = 1)
INSERT INTO fix (application_id, issue_summary, code_class_name, code_line, github_pr, issue_status, reason, change_number, created_at, updated_at)
VALUES 
    (1, 'NullPointerException in user authentication flow', 'com.company.user.service.AuthService', 145, NULL, 'PENDING', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Database connection timeout in user profile fetch', 'com.company.user.repository.UserRepository', 78, 'PR-1234', 'IN_PROGRESS', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Memory leak in session management', 'com.company.user.service.SessionService', 203, NULL, 'IGNORED', 'Duplicate of FIX-456, already being tracked', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Invalid SQL query in user search', 'com.company.user.dao.UserDao', 92, NULL, 'DB_FIX', 'Database schema needs update', 'CHG-7890', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Fixes for PaymentGateway (application_id = 2)
INSERT INTO fix (application_id, issue_summary, code_class_name, code_line, github_pr, issue_status, reason, change_number, created_at, updated_at)
VALUES 
    (2, 'Payment processing fails for international cards', 'com.company.payment.processor.CardProcessor', 312, 'https://github.com/company/payment-gateway/pull/567', 'RESOLVED', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Race condition in concurrent payment requests', 'com.company.payment.service.TransactionService', 189, NULL, 'PENDING', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Incorrect tax calculation for EU region', 'com.company.payment.calculator.TaxCalculator', 56, NULL, 'DB_FIX', 'Tax rates need to be updated in database', 'CHG-8901', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Fixes for InventorySystem (application_id = 3)
INSERT INTO fix (application_id, issue_summary, code_class_name, code_line, github_pr, issue_status, reason, change_number, created_at, updated_at)
VALUES 
    (3, 'Stock count mismatch in warehouse sync', 'com.company.inventory.sync.WarehouseSync', 421, NULL, 'PENDING', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'ArrayIndexOutOfBoundsException in batch processing', 'com.company.inventory.batch.BatchProcessor', 167, 'PR-2345', 'IN_PROGRESS', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Fixes for NotificationService (application_id = 4)
INSERT INTO fix (application_id, issue_summary, code_class_name, code_line, github_pr, issue_status, reason, change_number, created_at, updated_at)
VALUES 
    (4, 'Email notifications not being sent', 'com.company.notification.email.EmailSender', 234, NULL, 'RESOLVED', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'SMS gateway timeout issues', 'com.company.notification.sms.SmsGateway', 89, NULL, 'IGNORED', 'Third-party gateway issue, not our code', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Fixes for AuthenticationAPI (application_id = 5)
INSERT INTO fix (application_id, issue_summary, code_class_name, code_line, github_pr, issue_status, reason, change_number, created_at, updated_at)
VALUES 
    (5, 'JWT token expiration not being validated', 'com.company.auth.security.JwtValidator', 112, 'PR-3456', 'IN_PROGRESS', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, 'OAuth2 callback URL mismatch', 'com.company.auth.oauth.OAuth2Handler', 78, NULL, 'PENDING', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================================
-- 3. MAIL TEST DATA
-- ============================================================================
-- Insert sample email incidents linked to applications and fixes

-- Mails for UserService (application_id = 1)
INSERT INTO mail (fix_id, application_id, replied, log_location, email_subject, email_from, email_message_id, created_time, replied_time)
VALUES 
    (1, 1, 'N', 'C:/baki/logs/mail_001/logs', 'URGENT: Production error in user login', 'ops.team@company.com', 'MSG-001-2026-05-03-001', CURRENT_TIMESTAMP, NULL),
    (2, 1, 'Y', 'C:/baki/logs/mail_002/logs', 'Database timeout in user profile service', 'dev.team@company.com', 'MSG-002-2026-05-03-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 1, 'N', 'C:/baki/logs/mail_003/logs', 'Another user authentication failure', 'support@company.com', 'MSG-003-2026-05-03-003', CURRENT_TIMESTAMP, NULL);

-- Mails for PaymentGateway (application_id = 2)
INSERT INTO mail (fix_id, application_id, replied, log_location, email_subject, email_from, email_message_id, created_time, replied_time)
VALUES 
    (5, 2, 'Y', 'C:/baki/logs/mail_004/logs', 'Payment processing error for customer XYZ', 'finance@company.com', 'MSG-004-2026-05-03-004', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (6, 2, 'N', 'C:/baki/logs/mail_005/logs', 'Concurrent payment issue reported', 'qa.team@company.com', 'MSG-005-2026-05-03-005', CURRENT_TIMESTAMP, NULL);

-- Mails for InventorySystem (application_id = 3)
INSERT INTO mail (fix_id, application_id, replied, log_location, email_subject, email_from, email_message_id, created_time, replied_time)
VALUES 
    (8, 3, 'N', 'C:/baki/logs/mail_006/logs', 'Stock discrepancy in warehouse A', 'warehouse@company.com', 'MSG-006-2026-05-03-006', CURRENT_TIMESTAMP, NULL),
    (9, 3, 'Y', 'C:/baki/logs/mail_007/logs', 'Batch processing failure overnight', 'ops.team@company.com', 'MSG-007-2026-05-03-007', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Mails for NotificationService (application_id = 4)
INSERT INTO mail (fix_id, application_id, replied, log_location, email_subject, email_from, email_message_id, created_time, replied_time)
VALUES 
    (10, 4, 'Y', 'C:/baki/logs/mail_008/logs', 'Emails not reaching customers', 'customer.service@company.com', 'MSG-008-2026-05-03-008', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Mails for AuthenticationAPI (application_id = 5)
INSERT INTO mail (fix_id, application_id, replied, log_location, email_subject, email_from, email_message_id, created_time, replied_time)
VALUES 
    (12, 5, 'N', 'C:/baki/logs/mail_009/logs', 'JWT token validation issue', 'security@company.com', 'MSG-009-2026-05-03-009', CURRENT_TIMESTAMP, NULL),
    (13, 5, 'N', 'C:/baki/logs/mail_010/logs', 'OAuth2 integration broken', 'dev.team@company.com', 'MSG-010-2026-05-03-010', CURRENT_TIMESTAMP, NULL);

-- ============================================================================
-- 4. JIRA TEST DATA
-- ============================================================================
-- Insert sample Jira incidents linked to applications and fixes

-- Jira tickets for UserService (application_id = 1)
INSERT INTO jira (fix_id, application_id, replied, log_location, jira_ticket_key, jira_summary, jira_issue_type, created_time, replied_time)
VALUES 
    (1, 1, 'N', 'C:/baki/logs/jira_USER-101/logs', 'USER-101', 'NullPointerException in authentication service', 'Bug', CURRENT_TIMESTAMP, NULL),
    (2, 1, 'Y', 'C:/baki/logs/jira_USER-102/logs', 'USER-102', 'Database connection timeout in profile fetch', 'Incident', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 1, 'N', 'C:/baki/logs/jira_USER-103/logs', 'USER-103', 'User search query performance issue', 'Task', CURRENT_TIMESTAMP, NULL);

-- Jira tickets for PaymentGateway (application_id = 2)
INSERT INTO jira (fix_id, application_id, replied, log_location, jira_ticket_key, jira_summary, jira_issue_type, created_time, replied_time)
VALUES 
    (5, 2, 'Y', 'C:/baki/logs/jira_PAY-201/logs', 'PAY-201', 'International card payment failures', 'Bug', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (6, 2, 'N', 'C:/baki/logs/jira_PAY-202/logs', 'PAY-202', 'Race condition in payment processing', 'Bug', CURRENT_TIMESTAMP, NULL),
    (7, 2, 'Y', 'C:/baki/logs/jira_PAY-203/logs', 'PAY-203', 'Incorrect tax calculation for EU customers', 'Bug', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Jira tickets for InventorySystem (application_id = 3)
INSERT INTO jira (fix_id, application_id, replied, log_location, jira_ticket_key, jira_summary, jira_issue_type, created_time, replied_time)
VALUES 
    (8, 3, 'N', 'C:/baki/logs/jira_INV-301/logs', 'INV-301', 'Stock count mismatch between systems', 'Incident', CURRENT_TIMESTAMP, NULL),
    (9, 3, 'Y', 'C:/baki/logs/jira_INV-302/logs', 'INV-302', 'Batch processing crash with large datasets', 'Bug', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Jira tickets for NotificationService (application_id = 4)
INSERT INTO jira (fix_id, application_id, replied, log_location, jira_ticket_key, jira_summary, jira_issue_type, created_time, replied_time)
VALUES 
    (10, 4, 'Y', 'C:/baki/logs/jira_NOTIF-401/logs', 'NOTIF-401', 'Email delivery failure in production', 'Incident', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (11, 4, 'N', 'C:/baki/logs/jira_NOTIF-402/logs', 'NOTIF-402', 'SMS gateway timeout issues', 'Bug', CURRENT_TIMESTAMP, NULL);

-- Jira tickets for AuthenticationAPI (application_id = 5)
INSERT INTO jira (fix_id, application_id, replied, log_location, jira_ticket_key, jira_summary, jira_issue_type, created_time, replied_time)
VALUES 
    (12, 5, 'N', 'C:/baki/logs/jira_AUTH-501/logs', 'AUTH-501', 'JWT token expiration not validated properly', 'Bug', CURRENT_TIMESTAMP, NULL),
    (13, 5, 'N', 'C:/baki/logs/jira_AUTH-502/logs', 'AUTH-502', 'OAuth2 callback URL configuration issue', 'Task', CURRENT_TIMESTAMP, NULL);

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================
-- Use these queries to verify the test data was inserted correctly

-- Count records in each table
-- SELECT 'Applications' as entity, COUNT(*) as count FROM application_data
-- UNION ALL
-- SELECT 'Fixes', COUNT(*) FROM fix
-- UNION ALL
-- SELECT 'Mails', COUNT(*) FROM mail
-- UNION ALL
-- SELECT 'Jiras', COUNT(*) FROM jira;

-- View applications with their fix counts
-- SELECT 
--     a.application_id,
--     a.application_name,
--     COUNT(f.fix_id) as fix_count
-- FROM application_data a
-- LEFT JOIN fix f ON a.application_id = f.application_id
-- GROUP BY a.application_id, a.application_name
-- ORDER BY a.application_id;

-- View fixes with their incident counts
-- SELECT 
--     f.fix_id,
--     f.issue_summary,
--     f.issue_status,
--     COUNT(DISTINCT m.mail_id) as mail_count,
--     COUNT(DISTINCT j.jira_id) as jira_count
-- FROM fix f
-- LEFT JOIN mail m ON f.fix_id = m.fix_id
-- LEFT JOIN jira j ON f.fix_id = j.fix_id
-- GROUP BY f.fix_id, f.issue_summary, f.issue_status
-- ORDER BY f.fix_id;

-- View unreplied incidents (ready for batch processing)
-- SELECT 
--     'Mail' as type,
--     m.mail_id as incident_id,
--     m.email_subject as summary,
--     a.application_name,
--     f.issue_summary as fix_summary
-- FROM mail m
-- JOIN application_data a ON m.application_id = a.application_id
-- LEFT JOIN fix f ON m.fix_id = f.fix_id
-- WHERE m.replied = 'N'
-- UNION ALL
-- SELECT 
--     'Jira' as type,
--     j.jira_id as incident_id,
--     j.jira_summary as summary,
--     a.application_name,
--     f.issue_summary as fix_summary
-- FROM jira j
-- JOIN application_data a ON j.application_id = a.application_id
-- LEFT JOIN fix f ON j.fix_id = f.fix_id
-- WHERE j.replied = 'N'
-- ORDER BY type, incident_id;

-- ============================================================================
-- END OF TEST DATA INSERTS
-- ============================================================================
-- Summary:
-- - 5 Applications inserted
-- - 13 Fixes inserted (covering all IssueStatus values)
-- - 10 Mail incidents inserted (mix of replied and unreplied)
-- - 13 Jira incidents inserted (mix of replied and unreplied)
-- 
-- Total: 41 test records across 4 tables
-- ============================================================================

-- Made with Bob
