-- ============================================================================
-- FindIt - Campus Lost & Found Management System
-- Script: 09_cursors.sql
-- Description: Demonstrates explicit PL/SQL cursors, parameterized cursors,
--              cursor attributes (%FOUND, %NOTFOUND, %ROWCOUNT, %ISOPEN),
--              Cursor FOR loops, and cursor-based batch processing.
-- Target DBMS: Oracle Database 19c / 21c / 23c / XE
-- ============================================================================

SET SERVEROUTPUT ON;
SET LINESIZE 200;

PROMPT ========================================================================
PROMPT 1. EXPLICIT CURSOR (OPEN, FETCH, %NOTFOUND, CLOSE)
PROMPT Processes and displays pending ownership claims for administrative review.
PROMPT ========================================================================

DECLARE
    -- 1. Declare explicit cursor for pending claims
    CURSOR cur_pending_claims IS
        SELECT 
            c.claim_id,
            c.claim_date,
            u.name AS claimant_name,
            u.email AS claimant_email,
            i.item_name,
            i.category,
            m.match_score,
            c.verification_details
        FROM CLAIMS c
        JOIN MATCHES m ON c.match_id = m.match_id
        JOIN USERS u ON c.claimant_id = u.user_id
        JOIN REPORTS r ON m.lost_report_id = r.report_id
        JOIN ITEMS i ON r.item_id = i.item_id
        WHERE c.claim_status = 'PENDING'
        ORDER BY m.match_score DESC, c.claim_date ASC;

    -- Record variable to hold fetched row
    v_rec cur_pending_claims%ROWTYPE;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--------------------------------------------------------------------------');
    DBMS_OUTPUT.PUT_LINE('           ADMINISTRATIVE PENDING CLAIMS QUEUE (EXPLICIT CURSOR)          ');
    DBMS_OUTPUT.PUT_LINE('--------------------------------------------------------------------------');

    -- 2. Open the cursor
    OPEN cur_pending_claims;
    
    IF cur_pending_claims%ISOPEN THEN
        DBMS_OUTPUT.PUT_LINE('Cursor CUR_PENDING_CLAIMS successfully opened.');
    END IF;

    -- 3. Fetch rows in a loop
    LOOP
        FETCH cur_pending_claims INTO v_rec;
        
        -- Check %NOTFOUND attribute to exit when all records processed
        EXIT WHEN cur_pending_claims%NOTFOUND;

        DBMS_OUTPUT.PUT_LINE('Claim ID     : #' || v_rec.claim_id || ' (Logged: ' || TO_CHAR(v_rec.claim_date, 'YYYY-MM-DD') || ')');
        DBMS_OUTPUT.PUT_LINE('Claimant     : ' || v_rec.claimant_name || ' (' || v_rec.claimant_email || ')');
        DBMS_OUTPUT.PUT_LINE('Item Name    : ' || v_rec.item_name || ' [' || v_rec.category || ']');
        DBMS_OUTPUT.PUT_LINE('Match Score  : ' || v_rec.match_score || '/4.0 (' || (v_rec.match_score * 25) || '% confidence)');
        DBMS_OUTPUT.PUT_LINE('Proof Detail : ' || SUBSTR(v_rec.verification_details, 1, 80) || '...');
        DBMS_OUTPUT.PUT_LINE('--------------------------------------------------------------------------');
    END LOOP;

    DBMS_OUTPUT.PUT_LINE('Total Pending Claims Processed: ' || cur_pending_claims%ROWCOUNT);

    -- 4. Close the cursor
    CLOSE cur_pending_claims;
END;
/

PROMPT ========================================================================
PROMPT 2. PARAMETERIZED CURSOR
PROMPT Filters reports by category and minimum match score.
PROMPT ========================================================================

DECLARE
    -- Parameterized cursor accepting category name and score threshold
    CURSOR cur_high_confidence_matches (
        p_category VARCHAR2,
        p_min_score NUMBER
    ) IS
        SELECT 
            m.match_id,
            m.match_score,
            i_lost.item_name AS lost_name,
            u_lost.name AS lost_owner,
            i_found.item_name AS found_name,
            u_found.name AS finder,
            r_found.location
        FROM MATCHES m
        JOIN REPORTS r_lost ON m.lost_report_id = r_lost.report_id
        JOIN ITEMS i_lost ON r_lost.item_id = i_lost.item_id
        JOIN USERS u_lost ON r_lost.user_id = u_lost.user_id
        JOIN REPORTS r_found ON m.found_report_id = r_found.report_id
        JOIN ITEMS i_found ON r_found.item_id = i_found.item_id
        JOIN USERS u_found ON r_found.user_id = u_found.user_id
        WHERE i_lost.category = p_category
          AND m.match_score >= p_min_score
        ORDER BY m.match_score DESC;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- High-Confidence Electronics Matches (Score >= 3.0) ---');
    
    FOR m IN cur_high_confidence_matches('Electronics', 3.0) LOOP
        DBMS_OUTPUT.PUT_LINE('Match #' || m.match_id || ' [Score: ' || m.match_score || '/4.0]');
        DBMS_OUTPUT.PUT_LINE('  Lost By   : ' || m.lost_owner || ' - ' || m.lost_name);
        DBMS_OUTPUT.PUT_LINE('  Found By  : ' || m.finder || ' - ' || m.found_name || ' at ' || m.location);
    END LOOP;
END;
/

PROMPT ========================================================================
PROMPT 3. CURSOR FOR LOOP (Category-Wise Summary Report)
PROMPT Concise syntax without manual OPEN/FETCH/CLOSE.
PROMPT ========================================================================

DECLARE
    CURSOR cur_cat_stats IS
        SELECT 
            i.category,
            COUNT(r.report_id) AS total_count,
            SUM(CASE WHEN r.report_type = 'LOST' THEN 1 ELSE 0 END) AS lost_cnt,
            SUM(CASE WHEN r.report_type = 'FOUND' THEN 1 ELSE 0 END) AS found_cnt,
            SUM(CASE WHEN r.status = 'RETURNED' THEN 1 ELSE 0 END) AS ret_cnt
        FROM ITEMS i
        JOIN REPORTS r ON i.item_id = r.item_id
        GROUP BY i.category
        ORDER BY total_count DESC;
BEGIN
    DBMS_OUTPUT.PUT_LINE(RPAD('Category', 18) || RPAD('Total', 8) || RPAD('Lost', 8) || RPAD('Found', 8) || RPAD('Returned', 10) || 'Recovery Rate');
    DBMS_OUTPUT.PUT_LINE(RPAD('-', 65, '-'));

    FOR rec IN cur_cat_stats LOOP
        DECLARE
            v_rate VARCHAR2(10);
        BEGIN
            IF rec.lost_cnt > 0 THEN
                v_rate := TO_CHAR(ROUND((rec.ret_cnt / rec.lost_cnt) * 100, 1)) || '%';
            ELSE
                v_rate := '0.0%';
            END IF;

            DBMS_OUTPUT.PUT_LINE(
                RPAD(rec.category, 18) || 
                RPAD(rec.total_count, 8) || 
                RPAD(rec.lost_cnt, 8) || 
                RPAD(rec.found_cnt, 8) || 
                RPAD(rec.ret_cnt, 10) || 
                v_rate
            );
        END;
    END LOOP;
END;
/

PROMPT Cursor demonstrations completed successfully!

