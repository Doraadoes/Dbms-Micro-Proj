-- ============================================================================
-- FindIt - Campus Lost & Found Management System
-- Script: 10_triggers.sql
-- Description: Implements row-level and statement-level database triggers for
--              automated audit logging, match relationship validation, and
--              instant notification generation.
-- Target DBMS: Oracle Database 19c / 21c / 23c / XE
-- ============================================================================

SET SERVEROUTPUT ON;

-- ----------------------------------------------------------------------------
-- TRIGGER 1: TRG_VALIDATE_MATCH_PAIR (BEFORE INSERT ON MATCHES)
-- Ensures business rules:
--   1. Lost report must have report_type = 'LOST'
--   2. Found report must have report_type = 'FOUND'
--   3. Cannot pair reports belonging to the exact same user (prevents self-match)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_VALIDATE_MATCH_PAIR
BEFORE INSERT OR UPDATE ON MATCHES
FOR EACH ROW
DECLARE
    v_lost_type   VARCHAR2(10);
    v_found_type  VARCHAR2(10);
    v_lost_user   NUMBER;
    v_found_user  NUMBER;
BEGIN
    -- Fetch lost report metadata
    SELECT report_type, user_id INTO v_lost_type, v_lost_user
    FROM REPORTS WHERE report_id = :NEW.lost_report_id;

    -- Fetch found report metadata
    SELECT report_type, user_id INTO v_found_type, v_found_user
    FROM REPORTS WHERE report_id = :NEW.found_report_id;

    IF v_lost_type != 'LOST' THEN
        RAISE_APPLICATION_ERROR(-20001, 'Match Validation Error: lost_report_id must refer to a LOST report.');
    END IF;

    IF v_found_type != 'FOUND' THEN
        RAISE_APPLICATION_ERROR(-20002, 'Match Validation Error: found_report_id must refer to a FOUND report.');
    END IF;

    IF v_lost_user = v_found_user THEN
        RAISE_APPLICATION_ERROR(-20003, 'Match Validation Error: Student cannot match a lost item with an item they reported finding.');
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- TRIGGER 2: TRG_AUDIT_REPORT_STATUS (AFTER UPDATE ON REPORTS)
-- Automatically writes to AUDIT_LOG when report status changes.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_AUDIT_REPORT_STATUS
AFTER UPDATE OF status ON REPORTS
FOR EACH ROW
WHEN (OLD.status != NEW.status)
BEGIN
    INSERT INTO AUDIT_LOG (
        audit_id,
        user_id,
        operation,
        table_name,
        record_id,
        details,
        log_date
    ) VALUES (
        SEQ_AUDIT_LOG.NEXTVAL,
        :NEW.user_id,
        'UPDATE',
        'REPORTS',
        :NEW.report_id,
        'Report status changed from [' || :OLD.status || '] to [' || :NEW.status || ']. Location: ' || :NEW.location,
        SYSTIMESTAMP
    );
END;
/

-- ----------------------------------------------------------------------------
-- TRIGGER 3: TRG_AUDIT_CLAIM_STATUS (AFTER UPDATE ON CLAIMS)
-- Automatically writes to AUDIT_LOG and TRANSACTION_LOG upon claim review.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_AUDIT_CLAIM_STATUS
AFTER UPDATE OF claim_status ON CLAIMS
FOR EACH ROW
WHEN (OLD.claim_status != NEW.claim_status)
BEGIN
    -- 1. Insert into AUDIT_LOG
    INSERT INTO AUDIT_LOG (
        audit_id,
        user_id,
        operation,
        table_name,
        record_id,
        details,
        log_date
    ) VALUES (
        SEQ_AUDIT_LOG.NEXTVAL,
        :NEW.reviewed_by,
        CASE WHEN :NEW.claim_status = 'APPROVED' THEN 'APPROVE' ELSE 'REJECT' END,
        'CLAIMS',
        :NEW.claim_id,
        'Claim status transitioned from ' || :OLD.claim_status || ' to ' || :NEW.claim_status || '. Comments: ' || :NEW.admin_comments,
        SYSTIMESTAMP
    );

    -- 2. Insert into TRANSACTION_LOG
    INSERT INTO TRANSACTION_LOG (
        log_id,
        transaction_id,
        operation,
        table_name,
        record_id,
        old_value,
        new_value,
        log_status,
        log_date
    ) VALUES (
        SEQ_TRANSACTION_LOG.NEXTVAL,
        'TXN-CLAIM-' || :NEW.claim_id,
        'UPDATE',
        'CLAIMS',
        :NEW.claim_id,
        'claim_status=' || :OLD.claim_status,
        'claim_status=' || :NEW.claim_status || ', admin=' || :NEW.reviewed_by,
        'COMMITTED',
        SYSTIMESTAMP
    );
END;
/

-- ----------------------------------------------------------------------------
-- TRIGGER 4: TRG_NOTIFY_ON_STRONG_MATCH (AFTER INSERT ON MATCHES)
-- Automatically dispatches in-app notifications to both the student who lost 
-- the item and the finder whenever a strong match (Score >= 3.0) is recorded.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_NOTIFY_ON_STRONG_MATCH
AFTER INSERT ON MATCHES
FOR EACH ROW
WHEN (NEW.match_score >= 3.0)
DECLARE
    v_lost_user_id   NUMBER;
    v_found_user_id  NUMBER;
    v_item_name      VARCHAR2(120);
BEGIN
    -- Get user IDs and item name
    SELECT r.user_id, i.item_name
    INTO v_lost_user_id, v_item_name
    FROM REPORTS r
    JOIN ITEMS i ON r.item_id = i.item_id
    WHERE r.report_id = :NEW.lost_report_id;

    SELECT user_id INTO v_found_user_id
    FROM REPORTS WHERE report_id = :NEW.found_report_id;

    -- Notify student who lost the item
    INSERT INTO NOTIFICATIONS (notification_id, user_id, match_id, message)
    VALUES (
        SEQ_NOTIFICATIONS.NEXTVAL,
        v_lost_user_id,
        :NEW.match_id,
        'Potential Match Found! A reported found item matches your "' || v_item_name || '" with a score of ' || :NEW.match_score || '/4. Review details and file a claim.'
    );

    -- Notify finder
    INSERT INTO NOTIFICATIONS (notification_id, user_id, match_id, message)
    VALUES (
        SEQ_NOTIFICATIONS.NEXTVAL,
        v_found_user_id,
        :NEW.match_id,
        'Your found item "' || v_item_name || '" has been matched with a reported lost item (Score: ' || :NEW.match_score || '/4).'
    );
END;
/

PROMPT Triggers TRG_VALIDATE_MATCH_PAIR, TRG_AUDIT_REPORT_STATUS, TRG_AUDIT_CLAIM_STATUS, TRG_NOTIFY_ON_STRONG_MATCH created successfully!

