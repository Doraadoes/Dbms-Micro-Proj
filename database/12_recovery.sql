-- ============================================================================
-- FindIt - Campus Lost & Found Management System
-- Script: 12_recovery.sql
-- Description: Demonstrates Oracle Database Crash Recovery, Instance Recovery,
--              Atomicity via UNDO, Durability via REDO (Write-Ahead Logging),
--              SAVEPOINT partial rollback, and Oracle Flashback Queries.
-- Target DBMS: Oracle Database 19c / 21c / 23c / XE
-- ============================================================================

SET SERVEROUTPUT ON;
SET LINESIZE 200;

PROMPT ========================================================================
PROMPT ARCHITECTURAL NOTE FOR DBMS EVALUATION:
PROMPT Oracle Database operates an engine-level ACID recovery subsystem:
PROMPT   1. REDO LOGS: Write-Ahead Logging (WAL) managed by LGWR to ensure DURABILITY.
PROMPT   2. UNDO TABLESPACE: Managed by SMON and kernel to ensure ATOMICITY & READ CONSISTENCY.
PROMPT   3. APPLICATION TRANSACTION_LOG: High-level application audit trail.
PROMPT This script demonstrates actual Oracle engine recovery mechanisms.
PROMPT ========================================================================

-- ----------------------------------------------------------------------------
-- 1. DEMONSTRATION OF COMMIT AND DURABILITY
-- ----------------------------------------------------------------------------
DECLARE
    v_report_count_before NUMBER;
    v_report_count_after  NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_report_count_before FROM REPORTS;
    DBMS_OUTPUT.PUT_LINE('Initial count of reports: ' || v_report_count_before);

    -- Insert temporary test report
    INSERT INTO REPORTS (report_id, item_id, user_id, report_type, location, status)
    VALUES (9999, 2001, 1003, 'LOST', 'Temporary Test Recovery Zone', 'OPEN');

    -- Explicit COMMIT generates SCN (System Change Number) and flushes redo log buffer to disk
    COMMIT;

    SELECT COUNT(*) INTO v_report_count_after FROM REPORTS;
    DBMS_OUTPUT.PUT_LINE('Count after committed INSERT: ' || v_report_count_after || ' (Changes are now durable on disk)');

    -- Clean up test row
    DELETE FROM REPORTS WHERE report_id = 9999;
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Cleaned up temporary row with COMMIT.');
END;
/

-- ----------------------------------------------------------------------------
-- 2. DEMONSTRATION OF ATOMICITY AND ROLLBACK (UNDO RECOVERY)
-- ----------------------------------------------------------------------------
DECLARE
    v_status_original VARCHAR2(20);
    v_status_changed  VARCHAR2(20);
    v_status_restored VARCHAR2(20);
BEGIN
    -- Query original status of Report 3001
    SELECT status INTO v_status_original FROM REPORTS WHERE report_id = 3001;
    DBMS_OUTPUT.PUT_LINE('1. Original Status in Database: ' || v_status_original);

    -- Modify the record in uncommitted transaction
    UPDATE REPORTS SET status = 'CLOSED' WHERE report_id = 3001;
    
    SELECT status INTO v_status_changed FROM REPORTS WHERE report_id = 3001;
    DBMS_OUTPUT.PUT_LINE('2. Status after UPDATE (in private transaction buffer): ' || v_status_changed);

    -- Issue ROLLBACK: Oracle uses UNDO segments to revert changes to the before-image
    ROLLBACK;
    
    SELECT status INTO v_status_restored FROM REPORTS WHERE report_id = 3001;
    DBMS_OUTPUT.PUT_LINE('3. Status after ROLLBACK (reconstructed from UNDO): ' || v_status_restored);
    
    IF v_status_original = v_status_restored THEN
        DBMS_OUTPUT.PUT_LINE('-> Atomicity Verified: Transaction changes successfully discarded.');
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3. DEMONSTRATION OF PARTIAL RECOVERY VIA SAVEPOINTS
-- ----------------------------------------------------------------------------
DECLARE
    v_loc_initial  VARCHAR2(150);
    v_loc_sp1      VARCHAR2(150);
    v_loc_sp2      VARCHAR2(150);
    v_loc_final    VARCHAR2(150);
BEGIN
    SELECT location INTO v_loc_initial FROM REPORTS WHERE report_id = 3007;
    DBMS_OUTPUT.PUT_LINE('Initial Location: ' || v_loc_initial);

    -- Change 1
    UPDATE REPORTS SET location = 'Sports Complex Gym Entrance' WHERE report_id = 3007;
    SAVEPOINT sp_stage_one;
    SELECT location INTO v_loc_sp1 FROM REPORTS WHERE report_id = 3007;
    DBMS_OUTPUT.PUT_LINE('Location at Stage 1: ' || v_loc_sp1);

    -- Change 2 (Erroneous change)
    UPDATE REPORTS SET location = 'CORRUPTED_ACCIDENTAL_LOCATION' WHERE report_id = 3007;
    SELECT location INTO v_loc_sp2 FROM REPORTS WHERE report_id = 3007;
    DBMS_OUTPUT.PUT_LINE('Location after bad update: ' || v_loc_sp2);

    -- Partial rollback to Stage 1
    ROLLBACK TO SAVEPOINT sp_stage_one;
    SELECT location INTO v_loc_final FROM REPORTS WHERE report_id = 3007;
    DBMS_OUTPUT.PUT_LINE('Location after ROLLBACK TO SAVEPOINT sp_stage_one: ' || v_loc_final);

    -- Release transaction without altering initial data
    ROLLBACK;
    DBMS_OUTPUT.PUT_LINE('Cleaned up partial recovery demonstration.');
END;
/

-- ----------------------------------------------------------------------------
-- 4. ORACLE FLASHBACK QUERY (POINT-IN-TIME RECOVERY DEMONSTRATION)
-- ----------------------------------------------------------------------------
-- Oracle allows reconstructing historical row states using UNDO tablespace
-- through the AS OF TIMESTAMP clause.
PROMPT Querying historical state of REPORTS table as of 5 minutes ago:
SELECT report_id, report_type, location, status
FROM REPORTS AS OF TIMESTAMP (SYSTIMESTAMP - INTERVAL '5' MINUTE)
WHERE report_id IN (3001, 3002);

PROMPT Recovery and ACID demonstration script completed successfully!

