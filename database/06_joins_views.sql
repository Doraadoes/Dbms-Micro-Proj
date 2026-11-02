-- ============================================================================
-- FindIt - Campus Lost & Found Management System
-- Script: 06_joins_views.sql
-- Description: Demonstrates all SQL Join types (Inner, Left Outer, Right Outer,
--              Full Outer, Cross, Self, Equi, Non-Equi/Theta Join) and creates
--              the 4 core database views required by the specification.
-- Target DBMS: Oracle Database 19c / 21c / 23c / XE
-- ============================================================================

SET LINESIZE 220;
SET PAGESIZE 60;

PROMPT ========================================================================
PROMPT PART 1: COMPREHENSIVE SQL JOINS DEMONSTRATION
PROMPT ========================================================================

-- 1.1 INNER JOIN (Equi-Join): Correlate reports, items, and reporters
SELECT 
    r.report_id,
    r.report_type,
    i.item_name,
    i.category,
    u.name AS reporter_name,
    r.location,
    r.status
FROM REPORTS r
INNER JOIN ITEMS i ON r.item_id = i.item_id
INNER JOIN USERS u ON r.user_id = u.user_id
ORDER BY r.report_id;

-- 1.2 LEFT OUTER JOIN: Retrieve ALL reports and their claims if any exist
SELECT 
    r.report_id,
    r.report_type,
    i.item_name,
    r.status AS report_status,
    NVL(TO_CHAR(c.claim_id), 'No Claim Filed') AS claim_reference,
    NVL(c.claim_status, 'N/A') AS claim_status
FROM REPORTS r
JOIN ITEMS i ON r.item_id = i.item_id
LEFT OUTER JOIN MATCHES m ON (r.report_id = m.lost_report_id OR r.report_id = m.found_report_id)
LEFT OUTER JOIN CLAIMS c ON m.match_id = c.match_id
ORDER BY r.report_id;

-- 1.3 RIGHT OUTER JOIN: Match all registered students with reports they may have filed
SELECT 
    u.user_id,
    u.name AS student_name,
    u.email,
    r.report_id,
    r.report_type,
    r.status
FROM REPORTS r
RIGHT OUTER JOIN USERS u ON r.user_id = u.user_id
WHERE u.role = 'STUDENT'
ORDER BY u.user_id;

-- 1.4 FULL OUTER JOIN: Reconcile all lost and found reports by campus location
SELECT 
    NVL(l.location, f.location) AS campus_zone,
    l.report_id AS lost_report_id,
    f.report_id AS found_report_id,
    l.status AS lost_status,
    f.status AS found_status
FROM (SELECT * FROM REPORTS WHERE report_type = 'LOST') l
FULL OUTER JOIN (SELECT * FROM REPORTS WHERE report_type = 'FOUND') f
    ON l.location = f.location AND l.item_id = f.item_id;

-- 1.5 CROSS JOIN (Cartesian Product): Generate audit matrix of categories x locations
SELECT cat.category, loc.location
FROM (SELECT DISTINCT category FROM ITEMS) cat
CROSS JOIN (SELECT DISTINCT location FROM REPORTS) loc
WHERE ROWNUM <= 10;

-- 1.6 SELF JOIN: Pair reports that happened in the exact same location on different items
SELECT 
    r1.report_id AS report_a_id,
    r1.report_type AS type_a,
    r2.report_id AS report_b_id,
    r2.report_type AS type_b,
    r1.location
FROM REPORTS r1
JOIN REPORTS r2 ON r1.location = r2.location 
               AND r1.report_id < r2.report_id;

-- 1.7 NON-EQUI / THETA JOIN: Match lost and found items within a 7-day plausible timeline
-- (Found date must be on or after the lost date, but within 7 days of being lost)
SELECT 
    l_rep.report_id AS lost_id,
    l_item.item_name AS lost_item,
    l_rep.report_date AS lost_date,
    f_rep.report_id AS found_id,
    f_item.item_name AS found_item,
    f_rep.report_date AS found_date,
    (f_rep.report_date - l_rep.report_date) AS days_elapsed
FROM REPORTS l_rep
JOIN ITEMS l_item ON l_rep.item_id = l_item.item_id
JOIN REPORTS f_rep ON f_rep.report_type = 'FOUND'
JOIN ITEMS f_item ON f_rep.item_id = f_item.item_id
WHERE l_rep.report_type = 'LOST'
  AND l_item.category = f_item.category
  AND f_rep.report_date >= l_rep.report_date 
  AND f_rep.report_date <= (l_rep.report_date + 7);

PROMPT ========================================================================
PROMPT PART 2: DATABASE VIEWS
PROMPT ========================================================================

-- 2.1 VIEW: V_LOST_ITEMS
CREATE OR REPLACE VIEW V_LOST_ITEMS AS
SELECT 
    r.report_id,
    r.item_id,
    i.item_name,
    i.category,
    i.description,
    i.color,
    i.image_url,
    r.location AS lost_location,
    r.report_date AS lost_date,
    r.status AS report_status,
    u.user_id AS reporter_id,
    u.name AS reporter_name,
    u.email AS reporter_email,
    u.phone AS reporter_phone
FROM REPORTS r
JOIN ITEMS i ON r.item_id = i.item_id
JOIN USERS u ON r.user_id = u.user_id
WHERE r.report_type = 'LOST';

-- 2.2 VIEW: V_FOUND_ITEMS (Hides sensitive identifying marks for public safety)
CREATE OR REPLACE VIEW V_FOUND_ITEMS AS
SELECT 
    r.report_id,
    r.item_id,
    i.item_name,
    i.category,
    i.description,
    i.color,
    i.image_url,
    r.location AS found_location,
    r.report_date AS found_date,
    r.status AS report_status,
    u.user_id AS finder_id,
    u.name AS finder_name,
    -- Intentionally hide private_details to prevent fraudulent claims
    CASE 
        WHEN r.private_details IS NOT NULL THEN 'Protected by Finder / Admin'
        ELSE 'None'
    END AS verification_status
FROM REPORTS r
JOIN ITEMS i ON r.item_id = i.item_id
JOIN USERS u ON r.user_id = u.user_id
WHERE r.report_type = 'FOUND';

-- 2.3 VIEW: V_PENDING_CLAIMS (Admin actionable queue)
CREATE OR REPLACE VIEW V_PENDING_CLAIMS AS
SELECT 
    c.claim_id,
    c.match_id,
    m.match_score,
    c.claim_date,
    c.claim_status,
    c.verification_details,
    u.user_id AS claimant_id,
    u.name AS claimant_name,
    u.email AS claimant_email,
    u.phone AS claimant_phone,
    i_lost.item_name AS lost_item_name,
    r_lost.report_id AS lost_report_id,
    i_found.item_name AS found_item_name,
    r_found.report_id AS found_report_id,
    r_found.location AS found_location
FROM CLAIMS c
JOIN MATCHES m ON c.match_id = m.match_id
JOIN USERS u ON c.claimant_id = u.user_id
JOIN REPORTS r_lost ON m.lost_report_id = r_lost.report_id
JOIN ITEMS i_lost ON r_lost.item_id = i_lost.item_id
JOIN REPORTS r_found ON m.found_report_id = r_found.report_id
JOIN ITEMS i_found ON r_found.item_id = i_found.item_id
WHERE c.claim_status = 'PENDING';

-- 2.4 VIEW: V_RETURNED_ITEMS (Success metrics and recovery history)
CREATE OR REPLACE VIEW V_RETURNED_ITEMS AS
SELECT 
    c.claim_id,
    r_lost.report_id AS lost_report_id,
    r_found.report_id AS found_report_id,
    i.item_name,
    i.category,
    u_owner.name AS owner_name,
    u_owner.email AS owner_email,
    u_finder.name AS finder_name,
    u_admin.name AS resolved_by_admin,
    c.review_date AS date_returned,
    c.admin_comments
FROM CLAIMS c
JOIN MATCHES m ON c.match_id = m.match_id
JOIN REPORTS r_lost ON m.lost_report_id = r_lost.report_id
JOIN REPORTS r_found ON m.found_report_id = r_found.report_id
JOIN ITEMS i ON r_lost.item_id = i.item_id
JOIN USERS u_owner ON c.claimant_id = u_owner.user_id
JOIN USERS u_finder ON r_found.user_id = u_finder.user_id
LEFT JOIN USERS u_admin ON c.reviewed_by = u_admin.user_id
WHERE c.claim_status = 'APPROVED';

PROMPT Views V_LOST_ITEMS, V_FOUND_ITEMS, V_PENDING_CLAIMS, V_RETURNED_ITEMS created!

