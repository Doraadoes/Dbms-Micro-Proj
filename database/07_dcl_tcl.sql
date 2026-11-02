-- ============================================================================
-- FindIt - Campus Lost & Found Management System
-- Script: 07_dcl_tcl.sql
-- Description: Demonstrates Data Control Language (DCL: Roles, Grants, Revokes)
--              and Transaction Control Language (TCL: COMMIT, ROLLBACK, SAVEPOINT,
--              SET TRANSACTION).
-- Target DBMS: Oracle Database 19c / 21c / 23c / XE
-- ============================================================================

SET SERVEROUTPUT ON;

PROMPT ========================================================================
PROMPT PART 1: DATA CONTROL LANGUAGE (DCL)
PROMPT ========================================================================

-- 1.1 Create Custom Security Roles for Role-Based Access Control (RBAC)
BEGIN
    EXECUTE IMMEDIATE 'CREATE ROLE RL_FINDIT_STUDENT';
    DBMS_OUTPUT.PUT_LINE('Role RL_FINDIT_STUDENT created.');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Role RL_FINDIT_STUDENT already exists or permission denied.');
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE ROLE RL_FINDIT_ADMIN';
    DBMS_OUTPUT.PUT_LINE('Role RL_FINDIT_ADMIN created.');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Role RL_FINDIT_ADMIN already exists or permission denied.');
END;
/

-- 1.2 Principle of Least Privilege: Grant restricted student privileges
-- Students can query public views, insert their own reports and claims
GRANT SELECT ON V_LOST_ITEMS TO RL_FINDIT_STUDENT;
GRANT SELECT ON V_FOUND_ITEMS TO RL_FINDIT_STUDENT;
GRANT INSERT, SELECT ON ITEMS TO RL_FINDIT_STUDENT;
GRANT INSERT, SELECT ON REPORTS TO RL_FINDIT_STUDENT;
GRANT INSERT, SELECT ON CLAIMS TO RL_FINDIT_STUDENT;
GRANT SELECT, UPDATE(read_status) ON NOTIFICATIONS TO RL_FINDIT_STUDENT;

PROMPT Granted fine-grained student privileges to RL_FINDIT_STUDENT.

-- 1.3 Administrator Privileges: Full administrative maintenance access
GRANT SELECT, INSERT, UPDATE, DELETE ON USERS TO RL_FINDIT_ADMIN;
GRANT SELECT, INSERT, UPDATE, DELETE ON ITEMS TO RL_FINDIT_ADMIN;
GRANT SELECT, INSERT, UPDATE, DELETE ON REPORTS TO RL_FINDIT_ADMIN;
GRANT SELECT, INSERT, UPDATE, DELETE ON MATCHES TO RL_FINDIT_ADMIN;
GRANT SELECT, INSERT, UPDATE, DELETE ON CLAIMS TO RL_FINDIT_ADMIN;
GRANT SELECT, INSERT, UPDATE, DELETE ON NOTIFICATIONS TO RL_FINDIT_ADMIN;
GRANT SELECT, INSERT ON AUDIT_LOG TO RL_FINDIT_ADMIN;
GRANT SELECT, INSERT ON TRANSACTION_LOG TO RL_FINDIT_ADMIN;

PROMPT Granted administrative privileges to RL_FINDIT_ADMIN.

-- 1.4 REVOKE Demonstration: Prevent students from deleting reports directly
-- (Students can only request cancellation, but deletion is prohibited)
REVOKE DELETE ON REPORTS FROM RL_FINDIT_STUDENT;
PROMPT Executed REVOKE DELETE ON REPORTS FROM RL_FINDIT_STUDENT.

PROMPT ========================================================================
PROMPT PART 2: TRANSACTION CONTROL LANGUAGE (TCL)
PROMPT ========================================================================

-- 2.1 Demonstration of Read-Only Transaction Boundary
SET TRANSACTION READ ONLY;
SELECT COUNT(*) AS total_open_reports FROM REPORTS WHERE status = 'OPEN';
COMMIT; -- Releases read-only transaction state
PROMPT TCL: Read-Only transaction executed and committed cleanly.

-- 2.2 Demonstration of Multi-Step Transaction with SAVEPOINT and ROLLBACK
DECLARE
    v_report_id NUMBER;
    v_item_id   NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Starting Multi-Step Transaction ---');
    
    -- Step 1: Create an item
    INSERT INTO ITEMS (item_id, item_name, category, description, color)
    VALUES (SEQ_ITEMS.NEXTVAL, 'Draft Test Bicycle Lock', 'Accessories', 'Steel cable combination lock', 'Black')
    RETURNING item_id INTO v_item_id;
    
    DBMS_OUTPUT.PUT_LINE('Step 1: Inserted test item ID: ' || v_item_id);
    
    -- Set intermediate Savepoint A
    SAVEPOINT sp_after_item;
    
    -- Step 2: Create a report
    INSERT INTO REPORTS (report_id, item_id, user_id, report_type, location, status)
    VALUES (SEQ_REPORTS.NEXTVAL, v_item_id, 1003, 'LOST', 'Bicycle Stand North', 'OPEN')
    RETURNING report_id INTO v_report_id;
    
    DBMS_OUTPUT.PUT_LINE('Step 2: Inserted test report ID: ' || v_report_id);
    
    -- Set intermediate Savepoint B
    SAVEPOINT sp_after_report;
    
    -- Step 3: Simulate an intentional mistake that must be undone
    UPDATE REPORTS SET location = 'INVALID_UNKNOWN_CORRUPT_ZONE' WHERE report_id = v_report_id;
    DBMS_OUTPUT.PUT_LINE('Step 3: Made an erroneous update to location.');
    
    -- Step 4: Undo erroneous change by rolling back to Savepoint B
    ROLLBACK TO sp_after_report;
    DBMS_OUTPUT.PUT_LINE('Step 4: Rolled back to SAVEPOINT sp_after_report! Location change undone.');
    
    -- Step 5: Complete full rollback to clean up test rows completely
    ROLLBACK;
    DBMS_OUTPUT.PUT_LINE('Step 5: Full ROLLBACK executed. Database restored to initial state.');
END;
/

PROMPT DCL and TCL demonstrations completed successfully!

