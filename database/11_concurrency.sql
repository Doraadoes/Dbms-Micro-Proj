-- ============================================================================
-- FindIt - Campus Lost & Found Management System
-- Script: 11_concurrency.sql
-- Description: Comprehensive Concurrency Control & Locking demonstration:
--              Pessimistic locking with SELECT ... FOR UPDATE, Row-Level Locks,
--              Transaction Blocking, and Controlled Deadlock Reproduction.
-- Target DBMS: Oracle Database 19c / 21c / 23c / XE
-- ============================================================================

SET SERVEROUTPUT ON;

PROMPT ========================================================================
PROMPT DEMONSTRATION 1: PESSIMISTIC LOCKING WITH SELECT ... FOR UPDATE
PROMPT Prevents lost updates when an administrator begins reviewing a claim.
PROMPT ========================================================================

-- Session 1 locks the target claim row for exclusive evaluation
DECLARE
    v_claim_id   NUMBER := 5001;
    v_status     VARCHAR2(20);
    v_claimant   VARCHAR2(100);
BEGIN
    DBMS_OUTPUT.PUT_LINE('Acquiring exclusive lock on Claim #' || v_claim_id || ' using SELECT ... FOR UPDATE...');
    
    -- Acquires row-exclusive lock on row 5001
    SELECT c.claim_status, u.name
    INTO v_status, v_claimant
    FROM CLAIMS c
    JOIN USERS u ON c.claimant_id = u.user_id
    WHERE c.claim_id = v_claim_id
    FOR UPDATE OF c.claim_status;

    DBMS_OUTPUT.PUT_LINE('Lock acquired successfully on Claim #' || v_claim_id || ' (Claimant: ' || v_claimant || ', Status: ' || v_status || ')');
    DBMS_OUTPUT.PUT_LINE('Other sessions attempting to modify this specific row will be blocked until COMMIT or ROLLBACK.');
    
    -- Release lock via rollback
    ROLLBACK;
    DBMS_OUTPUT.PUT_LINE('Lock released cleanly via ROLLBACK.');
END;
/

PROMPT ========================================================================
PROMPT DEMONSTRATION 2: TABLE LOCK MODES (ROW SHARE vs EXCLUSIVE)
PROMPT ========================================================================

-- Lock table in SHARE MODE (Allows concurrent queries, blocks concurrent DDL & DML updates)
LOCK TABLE REPORTS IN SHARE MODE;
PROMPT Acquired SHARE MODE lock on REPORTS table. Concurrent SELECT is permitted.
COMMIT; -- Releases lock

-- Lock table in EXCLUSIVE MODE (Blocks all other sessions from modifying table)
LOCK TABLE CLAIMS IN EXCLUSIVE MODE;
PROMPT Acquired EXCLUSIVE MODE lock on CLAIMS table.
COMMIT; -- Releases lock

PROMPT ========================================================================
PROMPT DEMONSTRATION 3: TWO-SESSION TRANSACTION BLOCKING SIMULATION
PROMPT (Instructions for student viva demonstration with two SQL*Plus windows)
PROMPT ========================================================================

/*
STEP-BY-STEP TWO-SESSION BLOCKING TEST:

SESSION 1 (Admin Desk Terminal):
--------------------------------
-- 1. Begin transaction by updating Report #3001 without committing
UPDATE REPORTS 
SET status = 'MATCHED' 
WHERE report_id = 3001;

-- DO NOT ISSUE COMMIT YET! Row 3001 is now exclusively locked by Session 1.

SESSION 2 (Student Terminal):
-----------------------------
-- 2. Attempt to update the exact same report
UPDATE REPORTS 
SET location = 'Central Library 1st Floor' 
WHERE report_id = 3001;

-- OBSERVE: Session 2 is BLOCKED and waits indefinitely for Session 1.

BACK TO SESSION 1:
------------------
-- 3. Release the lock by committing
COMMIT;

-- OBSERVE IN SESSION 2: Session 2 unblocks immediately and completes its update!
*/

PROMPT ========================================================================
PROMPT DEMONSTRATION 4: CONTROLLED DEADLOCK REPRODUCTION (ORA-00060)
PROMPT ========================================================================

/*
HOW TO DEMONSTRATE ORACLE DEADLOCK DETECTION (ORA-00060):

Prepare two terminal windows connected as the same or different database users.

WINDOW 1 (Transaction T1):
--------------------------
UPDATE REPORTS SET status = 'OPEN' WHERE report_id = 3001;
-- Lock on Report 3001 acquired by T1.

WINDOW 2 (Transaction T2):
--------------------------
UPDATE REPORTS SET status = 'OPEN' WHERE report_id = 3002;
-- Lock on Report 3002 acquired by T2.

WINDOW 1 (Transaction T1):
--------------------------
-- Now T1 requests Report 3002 (currently held by T2)
UPDATE REPORTS SET status = 'OPEN' WHERE report_id = 3002;
-- WINDOW 1 hangs waiting for lock on Report 3002.

WINDOW 2 (Transaction T2):
--------------------------
-- Now T2 requests Report 3001 (currently held by T1)
UPDATE REPORTS SET status = 'OPEN' WHERE report_id = 3001;

OBSERVE RESULT:
- Oracle kernel deadlock detector checks wait-for graph.
- Within 3 seconds, Oracle breaks the circular wait cycle!
- One session receives error:
  "ORA-00060: deadlock detected while waiting for resource"
- Oracle automatically rolls back the offending statement, allowing the other session to proceed.
- Issue ROLLBACK in both sessions to restore clean state.
*/

PROMPT Concurrency and locking demonstration script completed!

