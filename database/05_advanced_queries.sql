-- ============================================================================
-- FindIt - Campus Lost & Found Management System
-- Script: 05_advanced_queries.sql
-- Description: Demonstrates simple & advanced SQL queries: WHERE, DISTINCT, 
--              BETWEEN, IN, LIKE, ORDER BY, Nested Subqueries, Set Operations,
--              String Functions, and Aggregates with GROUP BY & HAVING.
-- Target DBMS: Oracle Database 19c / 21c / 23c / XE
-- ============================================================================

SET LINESIZE 200;
SET PAGESIZE 50;
COL "Item Name" FORMAT A35;
COL "Location" FORMAT A25;
COL "User Name" FORMAT A20;

PROMPT ========================================================================
PROMPT 1. BASIC CLAUSES: WHERE, DISTINCT, BETWEEN, IN, LIKE, ORDER BY
PROMPT ========================================================================

-- 1.1 DISTINCT: Distinct locations where items have been lost or found
SELECT DISTINCT location 
FROM REPORTS 
ORDER BY location ASC;

-- 1.2 BETWEEN: Reports logged within a 10-day window
SELECT report_id, report_type, location, report_date, status
FROM REPORTS
WHERE report_date BETWEEN DATE '2026-10-15' AND DATE '2026-10-25'
ORDER BY report_date DESC;

-- 1.3 IN: Filter items belonging to high-value tech and document categories
SELECT item_id, item_name, category, color
FROM ITEMS
WHERE category IN ('Electronics', 'Documents')
ORDER BY category, item_name;

-- 1.4 LIKE: Search items with pattern matching (wildcard % and _)
SELECT item_id, item_name, description, color
FROM ITEMS
WHERE UPPER(item_name) LIKE '%CALCULATOR%' 
   OR UPPER(description) LIKE '%AIRPODS%';

-- 1.5 Compound WHERE with AND, OR, NOT and ORDER BY multiple columns
SELECT r.report_id, r.report_type, i.item_name, r.location, r.status
FROM REPORTS r
JOIN ITEMS i ON r.item_id = i.item_id
WHERE (r.report_type = 'LOST' AND r.status != 'CLOSED')
   OR (r.report_type = 'FOUND' AND UPPER(r.location) LIKE '%LIBRARY%')
ORDER BY r.status ASC, r.report_date DESC;

PROMPT ========================================================================
PROMPT 2. NESTED QUERIES (SUBQUERIES)
PROMPT ========================================================================

-- 2.1 Non-Correlated Subquery: Find all students who have logged at least one LOST report
SELECT user_id, name, email, phone
FROM USERS
WHERE user_id IN (
    SELECT DISTINCT user_id 
    FROM REPORTS 
    WHERE report_type = 'LOST'
);

-- 2.2 Correlated Subquery with EXISTS: Students with active unreviewed claims
SELECT u.user_id, u.name, u.email
FROM USERS u
WHERE EXISTS (
    SELECT 1 
    FROM CLAIMS c 
    WHERE c.claimant_id = u.user_id 
      AND c.claim_status = 'PENDING'
);

-- 2.3 NOT EXISTS: Find students who have registered but never filed any report
SELECT u.user_id, u.name, u.email, u.role
FROM USERS u
WHERE NOT EXISTS (
    SELECT 1 
    FROM REPORTS r 
    WHERE r.user_id = u.user_id
) AND u.role = 'STUDENT';

-- 2.4 Scalar Subquery: Display each item along with the total count of reports in that category
SELECT 
    i.item_id, 
    i.item_name, 
    i.category,
    (SELECT COUNT(*) 
     FROM REPORTS r 
     JOIN ITEMS i2 ON r.item_id = i2.item_id 
     WHERE i2.category = i.category) AS total_category_reports
FROM ITEMS i
ORDER BY total_category_reports DESC;

PROMPT ========================================================================
PROMPT 3. SET OPERATIONS (UNION, UNION ALL, INTERSECT, MINUS)
PROMPT ========================================================================

-- 3.1 UNION: Combined unique list of student names who reported Lost OR Found items
SELECT u.name AS "Student Name", 'Lost Reporter' AS activity
FROM USERS u
JOIN REPORTS r ON u.user_id = r.user_id
WHERE r.report_type = 'LOST'
UNION
SELECT u.name AS "Student Name", 'Found Reporter' AS activity
FROM USERS u
JOIN REPORTS r ON u.user_id = r.user_id
WHERE r.report_type = 'FOUND'
ORDER BY 1;

-- 3.2 INTERSECT: Students who have reported BOTH a lost item AND a found item
SELECT u.user_id, u.name, u.email
FROM USERS u
JOIN REPORTS r ON u.user_id = r.user_id
WHERE r.report_type = 'LOST'
INTERSECT
SELECT u.user_id, u.name, u.email
FROM USERS u
JOIN REPORTS r ON u.user_id = r.user_id
WHERE r.report_type = 'FOUND';

-- 3.3 MINUS: Students who filed lost reports but NEVER filed any found reports
SELECT u.user_id, u.name, u.email
FROM USERS u
JOIN REPORTS r ON u.user_id = r.user_id
WHERE r.report_type = 'LOST'
MINUS
SELECT u.user_id, u.name, u.email
FROM USERS u
JOIN REPORTS r ON u.user_id = r.user_id
WHERE r.report_type = 'FOUND';

PROMPT ========================================================================
PROMPT 4. STRING & DATE MANIPULATION FUNCTIONS
PROMPT ========================================================================

SELECT 
    item_id,
    UPPER(item_name) AS upper_name,
    LOWER(category) AS lower_category,
    INITCAP(color) AS formatted_color,
    LENGTH(description) AS desc_character_count,
    SUBSTR(description, 1, 40) || '...' AS truncated_summary,
    INSTR(UPPER(description), 'CASE') AS position_of_keyword
FROM ITEMS
WHERE ROWNUM <= 5;

PROMPT ========================================================================
PROMPT 5. AGGREGATE FUNCTIONS & GROUP BY WITH HAVING
PROMPT ========================================================================

-- 5.1 Category breakdown: Count, Min, Max, and percentage of total items
SELECT 
    i.category,
    COUNT(r.report_id) AS total_reports,
    SUM(CASE WHEN r.report_type = 'LOST' THEN 1 ELSE 0 END) AS lost_count,
    SUM(CASE WHEN r.report_type = 'FOUND' THEN 1 ELSE 0 END) AS found_count,
    SUM(CASE WHEN r.status = 'RETURNED' THEN 1 ELSE 0 END) AS returned_count,
    ROUND(
        (SUM(CASE WHEN r.status = 'RETURNED' THEN 1 ELSE 0 END) / NULLIF(COUNT(r.report_id), 0)) * 100, 
        2
    ) AS recovery_percentage
FROM ITEMS i
JOIN REPORTS r ON i.item_id = r.item_id
GROUP BY i.category
ORDER BY total_reports DESC;

-- 5.2 Hotspot analysis with HAVING: Locations with more than 1 incident
SELECT 
    r.location,
    COUNT(r.report_id) AS incident_count,
    MAX(r.report_date) AS most_recent_incident
FROM REPORTS r
GROUP BY r.location
HAVING COUNT(r.report_id) >= 2
ORDER BY incident_count DESC;

PROMPT Advanced queries demonstration completed successfully!

