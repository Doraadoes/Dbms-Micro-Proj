-- ============================================================================
-- FindIt - Campus Lost & Found Management System
-- Script: 04_ddl_dml.sql
-- Description: Demonstrates all core DDL (Data Definition Language) and DML 
--              (Data Manipulation Language) commands as required by the syllabus.
-- Target DBMS: Oracle Database 19c / 21c / 23c / XE
-- ============================================================================

SET SERVEROUTPUT ON;

PROMPT ========================================================================
PROMPT PART 1: DATA DEFINITION LANGUAGE (DDL) DEMONSTRATION
PROMPT ========================================================================

-- 1.1 CREATE TABLE: Create an experimental feedback / survey table
CREATE TABLE ITEM_FEEDBACK (
    feedback_id   NUMBER(10) PRIMARY KEY,
    report_id     NUMBER(10) NOT NULL,
    rating        NUMBER(1)  CHECK (rating BETWEEN 1 AND 5),
    comments      VARCHAR2(250),
    submitted_at  DATE DEFAULT SYSDATE
);
PROMPT DDL: Table ITEM_FEEDBACK created successfully.

-- 1.2 ALTER TABLE - ADD COLUMN: Add student contact consent flag
ALTER TABLE ITEM_FEEDBACK ADD (
    contact_consent CHAR(1) DEFAULT 'Y' CHECK (contact_consent IN ('Y', 'N'))
);
PROMPT DDL: Added column contact_consent to ITEM_FEEDBACK.

-- 1.3 ALTER TABLE - MODIFY COLUMN: Expand comments character length
ALTER TABLE ITEM_FEEDBACK MODIFY (
    comments VARCHAR2(500)
);
PROMPT DDL: Modified comments column width to VARCHAR2(500).

-- 1.4 ALTER TABLE - DROP COLUMN: Remove contact_consent
ALTER TABLE ITEM_FEEDBACK DROP COLUMN contact_consent;
PROMPT DDL: Dropped column contact_consent from ITEM_FEEDBACK.

-- 1.5 RENAME TABLE: Rename ITEM_FEEDBACK to REPORT_SURVEYS
RENAME ITEM_FEEDBACK TO REPORT_SURVEYS;
PROMPT DDL: Renamed table ITEM_FEEDBACK to REPORT_SURVEYS.

-- 1.6 TRUNCATE TABLE: Truncate removes all rows instantly without generating undo
INSERT INTO REPORT_SURVEYS (feedback_id, report_id, rating, comments) 
VALUES (1, 3001, 5, 'Great platform, recovered quickly!');
COMMIT;

TRUNCATE TABLE REPORT_SURVEYS;
PROMPT DDL: Truncated table REPORT_SURVEYS. Row count is now 0.

-- 1.7 DROP TABLE: Drop the demonstration table
DROP TABLE REPORT_SURVEYS PURGE;
PROMPT DDL: Dropped table REPORT_SURVEYS with PURGE option.

PROMPT ========================================================================
PROMPT PART 2: DATA MANIPULATION LANGUAGE (DML) DEMONSTRATION
PROMPT ========================================================================

-- 2.1 INSERT: Single row insert
INSERT INTO ITEMS (item_id, item_name, category, description, color)
VALUES (SEQ_ITEMS.NEXTVAL, 'Dell Latitude 7420 Laptop', 'Electronics', 'Silver corporate laptop with engineering faculty asset tag.', 'Silver');
PROMPT DML: Inserted single laptop record into ITEMS.

-- 2.2 INSERT with Subquery: Create a backup archive table for returned items
CREATE TABLE RETURNED_ITEMS_ARCHIVE AS
SELECT r.report_id, i.item_name, i.category, u.name AS owner_name, r.report_date
FROM REPORTS r
JOIN ITEMS i ON r.item_id = i.item_id
JOIN USERS u ON r.user_id = u.user_id
WHERE r.status = 'RETURNED';

PROMPT DML/DDL: Populated RETURNED_ITEMS_ARCHIVE using CTAS (Create Table As Select).
SELECT * FROM RETURNED_ITEMS_ARCHIVE;

-- Clean up demonstration archive table
DROP TABLE RETURNED_ITEMS_ARCHIVE PURGE;

-- 2.3 UPDATE: Single column update
UPDATE REPORTS
SET status = 'OPEN'
WHERE report_id = 3007 AND status = 'OPEN';
PROMPT DML: Executed UPDATE on REPORTS.

-- 2.4 UPDATE: Multi-column update with subquery
UPDATE CLAIMS
SET admin_comments = 'Verified by security staff',
    review_date = SYSDATE
WHERE claim_id = 5001 AND claim_status = 'PENDING';
PROMPT DML: Executed multi-column conditional UPDATE on CLAIMS.

-- 2.5 DELETE: Conditional DELETE demonstration inside a rollback transaction
SAVEPOINT before_delete_demo;

INSERT INTO NOTIFICATIONS (notification_id, user_id, message)
VALUES (SEQ_NOTIFICATIONS.NEXTVAL, 1003, 'Test ephemeral notification for deletion demo.');

DELETE FROM NOTIFICATIONS
WHERE message LIKE '%Test ephemeral notification%';
PROMPT DML: Deleted ephemeral test row from NOTIFICATIONS.

ROLLBACK TO before_delete_demo;
PROMPT DML/TCL: Rolled back delete demonstration safely.

-- 2.6 SELECT: Projection, Aliasing, Concatenation, and Row Limiting
SELECT 
    r.report_id AS "Report ID",
    UPPER(r.report_type) || ' - ' || i.item_name AS "Item Summary",
    i.category AS "Category",
    r.location AS "Location",
    TO_CHAR(r.report_date, 'DD-Mon-YYYY') AS "Date Logged",
    r.status AS "Current Status"
FROM REPORTS r
JOIN ITEMS i ON r.item_id = i.item_id
ORDER BY r.report_date DESC
FETCH FIRST 5 ROWS ONLY;

PROMPT DDL and DML demonstrations completed successfully!

