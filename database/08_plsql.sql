-- ============================================================================
-- FindIt - Campus Lost & Found Management System
-- Script: 08_plsql.sql
-- Description: Complete PL/SQL implementation including anonymous blocks,
--              variables/constants, control flow (IF-ELSE, CASE, LOOPS),
--              stored procedures, stored functions, and exception handling.
-- Target DBMS: Oracle Database 19c / 21c / 23c / XE
-- ============================================================================

SET SERVEROUTPUT ON;

PROMPT ========================================================================
PROMPT 1. PL/SQL ANONYMOUS BLOCK (Variables, Constants, IF-THEN, CASE, Loops)
PROMPT ========================================================================

DECLARE
    -- Variables and constants
    c_max_score       CONSTANT NUMBER := 4;
    v_total_reports   NUMBER := 0;
    v_open_reports    NUMBER := 0;
    v_returned_items  NUMBER := 0;
    v_recovery_pct    NUMBER(5,2) := 0.0;
    v_campus_status   VARCHAR2(50);
    v_counter         PLS_INTEGER := 1;
BEGIN
    DBMS_OUTPUT.PUT_LINE('==================================================');
    DBMS_OUTPUT.PUT_LINE('       CAMPUS LOST & FOUND STATUS OVERVIEW        ');
    DBMS_OUTPUT.PUT_LINE('==================================================');

    -- Aggregate metrics
    SELECT COUNT(*) INTO v_total_reports FROM REPORTS;
    SELECT COUNT(*) INTO v_open_reports FROM REPORTS WHERE status = 'OPEN';
    SELECT COUNT(*) INTO v_returned_items FROM REPORTS WHERE status = 'RETURNED';

    IF v_total_reports > 0 THEN
        v_recovery_pct := (v_returned_items / v_total_reports) * 100;
    ELSE
        v_recovery_pct := 0.0;
    END IF;

    -- IF - ELSIF - ELSE Decision Construct
    IF v_recovery_pct >= 50 THEN
        v_campus_status := 'High Recovery Efficiency (>= 50%)';
    ELSIF v_recovery_pct >= 20 THEN
        v_campus_status := 'Moderate Recovery Efficiency (20-49%)';
    ELSE
        v_campus_status := 'Low Recovery Efficiency (< 20%) - Needs Attention';
    END IF;

    DBMS_OUTPUT.PUT_LINE('Total Reports Logged : ' || v_total_reports);
    DBMS_OUTPUT.PUT_LINE('Open Incidents       : ' || v_open_reports);
    DBMS_OUTPUT.PUT_LINE('Returned Belongings  : ' || v_returned_items);
    DBMS_OUTPUT.PUT_LINE('Campus Recovery Rate : ' || TO_CHAR(v_recovery_pct, '990.99') || '%');
    DBMS_OUTPUT.PUT_LINE('Assessment           : ' || v_campus_status);

    -- CASE Statement demonstration
    DBMS_OUTPUT.PUT_LINE('--- Category Severity Matrix ---');
    FOR r IN (SELECT category, COUNT(*) as cnt FROM ITEMS GROUP BY category) LOOP
        CASE
            WHEN r.cnt >= 3 THEN
                DBMS_OUTPUT.PUT_LINE(RPAD(r.category, 20) || ' : [HIGH VOLUME] ' || r.cnt || ' items');
            WHEN r.cnt BETWEEN 1 AND 2 THEN
                DBMS_OUTPUT.PUT_LINE(RPAD(r.category, 20) || ' : [NORMAL VOLUME] ' || r.cnt || ' items');
            ELSE
                DBMS_OUTPUT.PUT_LINE(RPAD(r.category, 20) || ' : [NO INCIDENTS]');
        END CASE;
    END LOOP;

    -- WHILE Loop demonstration
    DBMS_OUTPUT.PUT_LINE('--- Processing Simulation Check ---');
    WHILE v_counter <= 3 LOOP
        DBMS_OUTPUT.PUT_LINE('System Health Check Iteration #' || v_counter || ': Normal');
        v_counter := v_counter + 1;
    END LOOP;
END;
/

PROMPT ========================================================================
PROMPT 2. STORED FUNCTION: FN_CALCULATE_MATCH_SCORE
PROMPT ========================================================================
-- Calculates the 4-point matching score between a LOST report and a FOUND report:
-- 1. Item Name match = 1.0 point
-- 2. Category match  = 1.0 point
-- 3. Color match     = 1.0 point
-- 4. Location match  = 1.0 point
CREATE OR REPLACE FUNCTION FN_CALCULATE_MATCH_SCORE (
    p_lost_report_id  IN NUMBER,
    p_found_report_id IN NUMBER
) RETURN NUMBER IS
    v_lost_name      VARCHAR2(120);
    v_lost_cat       VARCHAR2(50);
    v_lost_color     VARCHAR2(40);
    v_lost_loc       VARCHAR2(150);
    
    v_found_name     VARCHAR2(120);
    v_found_cat      VARCHAR2(50);
    v_found_color    VARCHAR2(40);
    v_found_loc      VARCHAR2(150);
    
    v_score          NUMBER(3,1) := 0.0;
    
    -- User-defined exception
    ex_invalid_comparison EXCEPTION;
BEGIN
    -- Ensure reports are distinct
    IF p_lost_report_id = p_found_report_id THEN
        RAISE ex_invalid_comparison;
    END IF;

    -- Fetch Lost report attributes
    SELECT UPPER(i.item_name), UPPER(i.category), UPPER(i.color), UPPER(r.location)
    INTO v_lost_name, v_lost_cat, v_lost_color, v_lost_loc
    FROM REPORTS r
    JOIN ITEMS i ON r.item_id = i.item_id
    WHERE r.report_id = p_lost_report_id AND r.report_type = 'LOST';

    -- Fetch Found report attributes
    SELECT UPPER(i.item_name), UPPER(i.category), UPPER(i.color), UPPER(r.location)
    INTO v_found_name, v_found_cat, v_found_color, v_found_loc
    FROM REPORTS r
    JOIN ITEMS i ON r.item_id = i.item_id
    WHERE r.report_id = p_found_report_id AND r.report_type = 'FOUND';

    -- 1. Category Match (1 point)
    IF v_lost_cat = v_found_cat THEN
        v_score := v_score + 1.0;
    END IF;

    -- 2. Color Match (1 point)
    IF v_lost_color = v_found_color OR INSTR(v_lost_color, v_found_color) > 0 OR INSTR(v_found_color, v_lost_color) > 0 THEN
        v_score := v_score + 1.0;
    END IF;

    -- 3. Location Match (1 point)
    IF v_lost_loc = v_found_loc OR INSTR(v_lost_loc, v_found_loc) > 0 OR INSTR(v_found_loc, v_lost_loc) > 0 THEN
        v_score := v_score + 1.0;
    END IF;

    -- 4. Item Name Keyword/Token Match (1 point)
    IF v_lost_name = v_found_name 
       OR INSTR(v_lost_name, v_found_name) > 0 
       OR INSTR(v_found_name, v_lost_name) > 0 
       OR INSTR(v_found_name, SUBSTR(v_lost_name, 1, 5)) > 0 THEN
        v_score := v_score + 1.0;
    END IF;

    RETURN v_score;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('Error: One or both reports do not exist or mismatch LOST/FOUND types.');
        RETURN 0.0;
    WHEN ex_invalid_comparison THEN
        DBMS_OUTPUT.PUT_LINE('Error: Cannot calculate match score between identical report ID.');
        RETURN 0.0;
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Unexpected error calculating match: ' || SQLERRM);
        RETURN 0.0;
END FN_CALCULATE_MATCH_SCORE;
/

PROMPT ========================================================================
PROMPT 3. STORED FUNCTION: FN_GET_RECOVERY_RATE
PROMPT ========================================================================
CREATE OR REPLACE FUNCTION FN_GET_RECOVERY_RATE RETURN NUMBER IS
    v_total_lost NUMBER := 0;
    v_returned   NUMBER := 0;
BEGIN
    SELECT COUNT(*) INTO v_total_lost FROM REPORTS WHERE report_type = 'LOST';
    SELECT COUNT(*) INTO v_returned FROM REPORTS WHERE report_type = 'LOST' AND status = 'RETURNED';

    IF v_total_lost = 0 THEN
        RETURN 0.0;
    ELSE
        RETURN ROUND((v_returned / v_total_lost) * 100, 2);
    END IF;
END FN_GET_RECOVERY_RATE;
/

PROMPT ========================================================================
PROMPT 4. STORED PROCEDURE: SP_APPROVE_CLAIM (Atomic Multi-Table Transaction)
PROMPT ========================================================================
CREATE OR REPLACE PROCEDURE SP_APPROVE_CLAIM (
    p_claim_id        IN NUMBER,
    p_admin_id        IN NUMBER,
    p_admin_comments  IN VARCHAR2,
    p_status_out      OUT VARCHAR2
) IS
    v_match_id        NUMBER;
    v_claimant_id     NUMBER;
    v_current_status  VARCHAR2(20);
    v_lost_report_id  NUMBER;
    v_found_report_id NUMBER;
    v_finder_id       NUMBER;
    v_item_name       VARCHAR2(120);
    v_admin_role      VARCHAR2(20);
    
    -- Custom Exceptions
    ex_unauthorized_admin EXCEPTION;
    ex_invalid_claim_state EXCEPTION;
BEGIN
    -- Verify reviewing user has ADMIN role
    SELECT role INTO v_admin_role FROM USERS WHERE user_id = p_admin_id;
    IF v_admin_role != 'ADMIN' THEN
        RAISE ex_unauthorized_admin;
    END IF;

    -- Fetch claim record with lock to avoid race conditions (concurrency safety)
    SELECT match_id, claimant_id, claim_status 
    INTO v_match_id, v_claimant_id, v_current_status
    FROM CLAIMS
    WHERE claim_id = p_claim_id
    FOR UPDATE;

    IF v_current_status != 'PENDING' THEN
        RAISE ex_invalid_claim_state;
    END IF;

    -- Fetch match and report details
    SELECT m.lost_report_id, m.found_report_id, i.item_name, r_found.user_id
    INTO v_lost_report_id, v_found_report_id, v_item_name, v_finder_id
    FROM MATCHES m
    JOIN REPORTS r_lost ON m.lost_report_id = r_lost.report_id
    JOIN ITEMS i ON r_lost.item_id = i.item_id
    JOIN REPORTS r_found ON m.found_report_id = r_found.report_id
    WHERE m.match_id = v_match_id;

    -- STEP 1: Update Claim status
    UPDATE CLAIMS
    SET claim_status = 'APPROVED',
        reviewed_by = p_admin_id,
        review_date = SYSDATE,
        admin_comments = NVL(p_admin_comments, 'Verified by campus admin.')
    WHERE claim_id = p_claim_id;

    -- STEP 2: Update Match status
    UPDATE MATCHES
    SET match_status = 'CONFIRMED'
    WHERE match_id = v_match_id;

    -- STEP 3: Update both Lost and Found reports to RETURNED
    UPDATE REPORTS
    SET status = 'RETURNED'
    WHERE report_id IN (v_lost_report_id, v_found_report_id);

    -- STEP 4: Generate notification for claimant
    INSERT INTO NOTIFICATIONS (notification_id, user_id, match_id, message)
    VALUES (
        SEQ_NOTIFICATIONS.NEXTVAL,
        v_claimant_id,
        v_match_id,
        'Good news! Your claim #' || p_claim_id || ' for "' || v_item_name || '" has been APPROVED. Visit the Security Desk to collect your item.'
    );

    -- STEP 5: Generate notification for finder
    INSERT INTO NOTIFICATIONS (notification_id, user_id, match_id, message)
    VALUES (
        SEQ_NOTIFICATIONS.NEXTVAL,
        v_finder_id,
        v_match_id,
        'Thank you! The "' || v_item_name || '" you found was successfully verified and returned to its rightful owner.'
    );

    -- STEP 6: Write to AUDIT_LOG
    INSERT INTO AUDIT_LOG (audit_id, user_id, operation, table_name, record_id, details)
    VALUES (
        SEQ_AUDIT_LOG.NEXTVAL,
        p_admin_id,
        'APPROVE',
        'CLAIMS',
        p_claim_id,
        'Approved claim for match #' || v_match_id || '. Both reports set to RETURNED.'
    );

    -- Commit transaction atomically
    COMMIT;
    p_status_out := 'SUCCESS';
    DBMS_OUTPUT.PUT_LINE('Claim #' || p_claim_id || ' successfully approved and processed atomically.');

EXCEPTION
    WHEN ex_unauthorized_admin THEN
        ROLLBACK;
        p_status_out := 'ERROR: User is not authorized as an administrator.';
        DBMS_OUTPUT.PUT_LINE(p_status_out);
    WHEN ex_invalid_claim_state THEN
        ROLLBACK;
        p_status_out := 'ERROR: Claim is not in PENDING state.';
        DBMS_OUTPUT.PUT_LINE(p_status_out);
    WHEN NO_DATA_FOUND THEN
        ROLLBACK;
        p_status_out := 'ERROR: Target claim record or admin was not found.';
        DBMS_OUTPUT.PUT_LINE(p_status_out);
    WHEN OTHERS THEN
        ROLLBACK;
        p_status_out := 'ERROR: ' || SQLERRM;
        DBMS_OUTPUT.PUT_LINE('Rollback executed due to error: ' || SQLERRM);
END SP_APPROVE_CLAIM;
/

PROMPT ========================================================================
PROMPT 5. STORED PROCEDURE: SP_RUN_MATCHING_ENGINE
PROMPT ========================================================================
CREATE OR REPLACE PROCEDURE SP_RUN_MATCHING_ENGINE (
    p_new_matches_count OUT NUMBER
) IS
    v_score NUMBER(3,1);
    v_count NUMBER := 0;
BEGIN
    FOR l IN (SELECT report_id FROM REPORTS WHERE report_type = 'LOST' AND status = 'OPEN') LOOP
        FOR f IN (SELECT report_id FROM REPORTS WHERE report_type = 'FOUND' AND status = 'OPEN') LOOP
            v_score := FN_CALCULATE_MATCH_SCORE(l.report_id, f.report_id);
            
            -- If score >= 2.0, record in MATCHES table if not already present
            IF v_score >= 2.0 THEN
                BEGIN
                    INSERT INTO MATCHES (match_id, lost_report_id, found_report_id, match_score, match_status)
                    VALUES (SEQ_MATCHES.NEXTVAL, l.report_id, f.report_id, v_score, 'PENDING');
                    
                    v_count := v_count + 1;
                EXCEPTION
                    WHEN DUP_VAL_ON_INDEX THEN
                        -- Match pair already recorded; update score if needed
                        UPDATE MATCHES 
                        SET match_score = v_score 
                        WHERE lost_report_id = l.report_id AND found_report_id = f.report_id;
                END;
            END IF;
        END LOOP;
    END LOOP;

    COMMIT;
    p_new_matches_count := v_count;
    DBMS_OUTPUT.PUT_LINE('Smart matching engine completed. Processed ' || v_count || ' matching pairs.');
END SP_RUN_MATCHING_ENGINE;
/

PROMPT PL/SQL blocks, functions, and stored procedures created successfully!

