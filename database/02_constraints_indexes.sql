-- ============================================================================
-- FindIt - Campus Lost & Found Management System
-- Script: 02_constraints_indexes.sql
-- Description: Adds additional performance indexes, composite indexes for 
--              smart matching, and domain integrity check constraints.
-- Target DBMS: Oracle Database 19c / 21c / 23c / XE
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. SEARCH & FILTER PERFORMANCE INDEXES
-- ----------------------------------------------------------------------------
-- Accelerate full-text and pattern searches on item attributes
CREATE INDEX IDX_ITEMS_NAME ON ITEMS(UPPER(item_name));
CREATE INDEX IDX_ITEMS_CATEGORY ON ITEMS(category);
CREATE INDEX IDX_ITEMS_COLOR ON ITEMS(UPPER(color));

-- Composite index to optimize smart matching queries on category + color
CREATE INDEX IDX_ITEMS_CAT_COL ON ITEMS(category, UPPER(color));

-- Indexes on reports for fast dashboard and search queries
CREATE INDEX IDX_REPORTS_LOCATION ON REPORTS(UPPER(location));
CREATE INDEX IDX_REPORTS_TYPE_STATUS ON REPORTS(report_type, status);
CREATE INDEX IDX_REPORTS_DATE ON REPORTS(report_date DESC);
CREATE INDEX IDX_REPORTS_USER ON REPORTS(user_id);
CREATE INDEX IDX_REPORTS_ITEM ON REPORTS(item_id);

-- Composite index for fast matching lookups (report_type, status, location)
CREATE INDEX IDX_REPORTS_MATCH_LOOKUP ON REPORTS(report_type, status, location);

-- Indexes on matches
CREATE INDEX IDX_MATCHES_LOST_ID ON MATCHES(lost_report_id);
CREATE INDEX IDX_MATCHES_FOUND_ID ON MATCHES(found_report_id);
CREATE INDEX IDX_MATCHES_SCORE ON MATCHES(match_score DESC);
CREATE INDEX IDX_MATCHES_STATUS ON MATCHES(match_status);

-- Indexes on claims
CREATE INDEX IDX_CLAIMS_MATCH_ID ON CLAIMS(match_id);
CREATE INDEX IDX_CLAIMS_CLAIMANT ON CLAIMS(claimant_id);
CREATE INDEX IDX_CLAIMS_STATUS ON CLAIMS(claim_status);
CREATE INDEX IDX_CLAIMS_DATE ON CLAIMS(claim_date DESC);

-- Indexes on notifications
CREATE INDEX IDX_NOTIF_USER_READ ON NOTIFICATIONS(user_id, read_status);
CREATE INDEX IDX_NOTIF_DATE ON NOTIFICATIONS(notification_date DESC);

-- Indexes on audit and transaction logs
CREATE INDEX IDX_AUDIT_LOG_DATE ON AUDIT_LOG(log_date DESC);
CREATE INDEX IDX_AUDIT_LOG_USER ON AUDIT_LOG(user_id);
CREATE INDEX IDX_AUDIT_LOG_TABLE ON AUDIT_LOG(table_name, record_id);
CREATE INDEX IDX_TXN_LOG_TXN_ID ON TRANSACTION_LOG(transaction_id);

-- ----------------------------------------------------------------------------
-- 2. DOMAIN INTEGRITY CHECK CONSTRAINTS
-- ----------------------------------------------------------------------------
-- Ensure college email structure contains '@' and valid domain
ALTER TABLE USERS ADD CONSTRAINT CK_USERS_EMAIL_FORMAT 
    CHECK (INSTR(email, '@') > 1 AND INSTR(email, '.') > INSTR(email, '@') + 1);

-- Ensure phone numbers have at least 10 digits
ALTER TABLE USERS ADD CONSTRAINT CK_USERS_PHONE_LEN 
    CHECK (LENGTH(phone) >= 10);

-- Ensure item name has minimum length
ALTER TABLE ITEMS ADD CONSTRAINT CK_ITEMS_NAME_LEN 
    CHECK (LENGTH(TRIM(item_name)) >= 2);

-- Ensure verification details are descriptive
ALTER TABLE CLAIMS ADD CONSTRAINT CK_CLAIMS_VERIF_LEN 
    CHECK (LENGTH(TRIM(verification_details)) >= 10);

PROMPT Indexes and additional domain integrity constraints created successfully!

