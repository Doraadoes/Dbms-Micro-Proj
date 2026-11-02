-- ============================================================================
-- FindIt - Campus Lost & Found Management System
-- Script: 01_create_tables.sql
-- Description: Creates sequences and the 8 core relational tables with 
--              proper Oracle data types, primary keys, and foreign keys.
-- Target DBMS: Oracle Database 19c / 21c / 23c / XE
-- ============================================================================

SET SERVEROUTPUT ON;

-- ----------------------------------------------------------------------------
-- 0. CLEANUP (Drop tables in reverse dependency order if they already exist)
-- ----------------------------------------------------------------------------
BEGIN
    FOR t IN (SELECT table_name FROM user_tables WHERE table_name IN 
             ('TRANSACTION_LOG', 'AUDIT_LOG', 'NOTIFICATIONS', 'CLAIMS', 
              'MATCHES', 'REPORTS', 'ITEMS', 'USERS')) LOOP
        EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS PURGE';
        DBMS_OUTPUT.PUT_LINE('Dropped existing table: ' || t.table_name);
    END LOOP;
END;
/

BEGIN
    FOR s IN (SELECT sequence_name FROM user_sequences WHERE sequence_name IN 
             ('SEQ_USERS', 'SEQ_ITEMS', 'SEQ_REPORTS', 'SEQ_MATCHES', 
              'SEQ_CLAIMS', 'SEQ_NOTIFICATIONS', 'SEQ_AUDIT_LOG', 'SEQ_TRANSACTION_LOG')) LOOP
        EXECUTE IMMEDIATE 'DROP SEQUENCE ' || s.sequence_name;
        DBMS_OUTPUT.PUT_LINE('Dropped existing sequence: ' || s.sequence_name);
    END LOOP;
END;
/

-- ----------------------------------------------------------------------------
-- 1. CREATE SEQUENCES FOR PRIMARY KEYS
-- ----------------------------------------------------------------------------
CREATE SEQUENCE SEQ_USERS START WITH 1001 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_ITEMS START WITH 2001 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_REPORTS START WITH 3001 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_MATCHES START WITH 4001 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_CLAIMS START WITH 5001 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_NOTIFICATIONS START WITH 6001 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_AUDIT_LOG START WITH 7001 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_TRANSACTION_LOG START WITH 8001 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ----------------------------------------------------------------------------
-- TABLE 1: USERS
-- Stores credentials and profiles of college students and administrators.
-- ----------------------------------------------------------------------------
CREATE TABLE USERS (
    user_id         NUMBER(10)          DEFAULT SEQ_USERS.NEXTVAL,
    name            VARCHAR2(100)       NOT NULL,
    email           VARCHAR2(150)       NOT NULL,
    phone           VARCHAR2(20)        NOT NULL,
    password_hash   VARCHAR2(255)       NOT NULL,
    role            VARCHAR2(20)        DEFAULT 'STUDENT' NOT NULL,
    created_at      TIMESTAMP           DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PK_USERS PRIMARY KEY (user_id),
    CONSTRAINT UQ_USERS_EMAIL UNIQUE (email),
    CONSTRAINT CK_USERS_ROLE CHECK (role IN ('STUDENT', 'ADMIN'))
);

COMMENT ON TABLE USERS IS 'Stores registered students and system administrators.';
COMMENT ON COLUMN USERS.user_id IS 'Primary key generated via SEQ_USERS.';
COMMENT ON COLUMN USERS.role IS 'Role of user: STUDENT or ADMIN.';

-- ----------------------------------------------------------------------------
-- TABLE 2: ITEMS
-- Stores physical and descriptive properties of reported items.
-- ----------------------------------------------------------------------------
CREATE TABLE ITEMS (
    item_id         NUMBER(10)          DEFAULT SEQ_ITEMS.NEXTVAL,
    item_name       VARCHAR2(120)       NOT NULL,
    category        VARCHAR2(50)        NOT NULL,
    description     VARCHAR2(1000)      NOT NULL,
    color           VARCHAR2(40)        NOT NULL,
    image_url       VARCHAR2(500),
    created_at      TIMESTAMP           DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PK_ITEMS PRIMARY KEY (item_id),
    CONSTRAINT CK_ITEMS_CATEGORY CHECK (category IN (
        'Electronics', 'Accessories', 'Stationery', 
        'Personal Items', 'Documents', 'Others'
    ))
);

COMMENT ON TABLE ITEMS IS 'Descriptive attributes of items reported lost or found.';

-- ----------------------------------------------------------------------------
-- TABLE 3: REPORTS
-- Links an item to a reporting user, tracking report type, place, date & status.
-- ----------------------------------------------------------------------------
CREATE TABLE REPORTS (
    report_id       NUMBER(10)          DEFAULT SEQ_REPORTS.NEXTVAL,
    item_id         NUMBER(10)          NOT NULL,
    user_id         NUMBER(10)          NOT NULL,
    report_type     VARCHAR2(10)        NOT NULL,
    location        VARCHAR2(150)       NOT NULL,
    report_date     DATE                DEFAULT SYSDATE NOT NULL,
    status          VARCHAR2(20)        DEFAULT 'OPEN' NOT NULL,
    private_details VARCHAR2(500),
    created_at      TIMESTAMP           DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PK_REPORTS PRIMARY KEY (report_id),
    CONSTRAINT FK_REPORTS_ITEMS FOREIGN KEY (item_id) 
        REFERENCES ITEMS(item_id) ON DELETE CASCADE,
    CONSTRAINT FK_REPORTS_USERS FOREIGN KEY (user_id) 
        REFERENCES USERS(user_id) ON DELETE CASCADE,
    CONSTRAINT CK_REPORTS_TYPE CHECK (report_type IN ('LOST', 'FOUND')),
    CONSTRAINT CK_REPORTS_STATUS CHECK (status IN ('OPEN', 'MATCHED', 'RETURNED', 'CLOSED'))
);

COMMENT ON TABLE REPORTS IS 'Links an item to the student who reported it as lost or found.';
COMMENT ON COLUMN REPORTS.private_details IS 'Sensitive identifying marks not exposed in general search.';

-- ----------------------------------------------------------------------------
-- TABLE 4: MATCHES
-- Records algorithmic pairings between LOST and FOUND reports with a match score.
-- ----------------------------------------------------------------------------
CREATE TABLE MATCHES (
    match_id        NUMBER(10)          DEFAULT SEQ_MATCHES.NEXTVAL,
    lost_report_id  NUMBER(10)          NOT NULL,
    found_report_id NUMBER(10)          NOT NULL,
    match_score     NUMBER(3,1)         NOT NULL,
    match_status    VARCHAR2(20)        DEFAULT 'PENDING' NOT NULL,
    created_at      TIMESTAMP           DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PK_MATCHES PRIMARY KEY (match_id),
    CONSTRAINT FK_MATCHES_LOST FOREIGN KEY (lost_report_id) 
        REFERENCES REPORTS(report_id) ON DELETE CASCADE,
    CONSTRAINT FK_MATCHES_FOUND FOREIGN KEY (found_report_id) 
        REFERENCES REPORTS(report_id) ON DELETE CASCADE,
    CONSTRAINT UQ_MATCH_PAIR UNIQUE (lost_report_id, found_report_id),
    CONSTRAINT CK_MATCH_SCORE CHECK (match_score BETWEEN 0 AND 4),
    CONSTRAINT CK_MATCH_STATUS CHECK (match_status IN ('PENDING', 'CONFIRMED', 'REJECTED', 'CLAIMED'))
);

COMMENT ON TABLE MATCHES IS 'Algorithmic correlation between lost and found reports (Score 0-4).';

-- ----------------------------------------------------------------------------
-- TABLE 5: CLAIMS
-- Manages student ownership claims on matched items, requiring admin review.
-- ----------------------------------------------------------------------------
CREATE TABLE CLAIMS (
    claim_id             NUMBER(10)     DEFAULT SEQ_CLAIMS.NEXTVAL,
    match_id             NUMBER(10)     NOT NULL,
    claimant_id          NUMBER(10)     NOT NULL,
    claim_date           DATE           DEFAULT SYSDATE NOT NULL,
    claim_status         VARCHAR2(20)   DEFAULT 'PENDING' NOT NULL,
    reviewed_by          NUMBER(10),
    review_date          DATE,
    verification_details VARCHAR2(1000) NOT NULL,
    admin_comments       VARCHAR2(500),
    created_at           TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PK_CLAIMS PRIMARY KEY (claim_id),
    CONSTRAINT FK_CLAIMS_MATCH FOREIGN KEY (match_id) 
        REFERENCES MATCHES(match_id) ON DELETE CASCADE,
    CONSTRAINT FK_CLAIMS_CLAIMANT FOREIGN KEY (claimant_id) 
        REFERENCES USERS(user_id) ON DELETE CASCADE,
    CONSTRAINT FK_CLAIMS_ADMIN FOREIGN KEY (reviewed_by) 
        REFERENCES USERS(user_id) ON DELETE SET NULL,
    CONSTRAINT CK_CLAIMS_STATUS CHECK (claim_status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

COMMENT ON TABLE CLAIMS IS 'Ownership claims submitted by students with proof and admin verdict.';

-- ----------------------------------------------------------------------------
-- TABLE 6: NOTIFICATIONS
-- In-app notifications generated on matches, claims, reviews, and status changes.
-- ----------------------------------------------------------------------------
CREATE TABLE NOTIFICATIONS (
    notification_id   NUMBER(10)        DEFAULT SEQ_NOTIFICATIONS.NEXTVAL,
    user_id           NUMBER(10)        NOT NULL,
    match_id          NUMBER(10),
    message           VARCHAR2(500)     NOT NULL,
    notification_date TIMESTAMP         DEFAULT CURRENT_TIMESTAMP NOT NULL,
    read_status       VARCHAR2(10)      DEFAULT 'UNREAD' NOT NULL,
    CONSTRAINT PK_NOTIFICATIONS PRIMARY KEY (notification_id),
    CONSTRAINT FK_NOTIFICATIONS_USER FOREIGN KEY (user_id) 
        REFERENCES USERS(user_id) ON DELETE CASCADE,
    CONSTRAINT FK_NOTIFICATIONS_MATCH FOREIGN KEY (match_id) 
        REFERENCES MATCHES(match_id) ON DELETE SET NULL,
    CONSTRAINT CK_NOTIF_READ_STATUS CHECK (read_status IN ('READ', 'UNREAD'))
);

COMMENT ON TABLE NOTIFICATIONS IS 'In-app notification messages delivered to student/admin accounts.';

-- ----------------------------------------------------------------------------
-- TABLE 7: AUDIT_LOG
-- Records critical database actions, who executed them, and targeted records.
-- ----------------------------------------------------------------------------
CREATE TABLE AUDIT_LOG (
    audit_id        NUMBER(10)          DEFAULT SEQ_AUDIT_LOG.NEXTVAL,
    user_id         NUMBER(10),
    operation       VARCHAR2(20)        NOT NULL,
    table_name      VARCHAR2(50)        NOT NULL,
    record_id       NUMBER(10)          NOT NULL,
    details         VARCHAR2(1000),
    log_date        TIMESTAMP           DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PK_AUDIT_LOG PRIMARY KEY (audit_id),
    CONSTRAINT FK_AUDIT_USER FOREIGN KEY (user_id) 
        REFERENCES USERS(user_id) ON DELETE SET NULL,
    CONSTRAINT CK_AUDIT_OPERATION CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE', 'APPROVE', 'REJECT', 'LOGIN', 'LOGOUT'))
);

COMMENT ON TABLE AUDIT_LOG IS 'Permanent audit trail for security and database traceability.';

-- ----------------------------------------------------------------------------
-- TABLE 8: TRANSACTION_LOG
-- Records before-and-after values of state-changing transactions for recovery tracing.
-- ----------------------------------------------------------------------------
CREATE TABLE TRANSACTION_LOG (
    log_id          NUMBER(10)          DEFAULT SEQ_TRANSACTION_LOG.NEXTVAL,
    transaction_id  VARCHAR2(64)        NOT NULL,
    operation       VARCHAR2(20)        NOT NULL,
    table_name      VARCHAR2(50)        NOT NULL,
    record_id       NUMBER(10)          NOT NULL,
    old_value       VARCHAR2(1000),
    new_value       VARCHAR2(1000),
    log_status      VARCHAR2(20)        DEFAULT 'COMMITTED' NOT NULL,
    log_date        TIMESTAMP           DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PK_TRANSACTION_LOG PRIMARY KEY (log_id),
    CONSTRAINT CK_TXN_LOG_STATUS CHECK (log_status IN ('PREPARED', 'COMMITTED', 'ROLLED_BACK'))
);

COMMENT ON TABLE TRANSACTION_LOG IS 'Application transaction audit log capturing state transitions.';

PROMPT All 8 tables and sequences created successfully!

