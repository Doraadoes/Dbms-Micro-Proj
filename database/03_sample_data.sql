-- ============================================================================
-- FindIt - Campus Lost & Found Management System
-- Script: 03_sample_data.sql
-- Description: Populates realistic sample data across all 8 tables to test
--              reporting, smart matching, claims, notifications, and auditing.
-- Target DBMS: Oracle Database 19c / 21c / 23c / XE
-- ============================================================================

-- Disable triggers during bulk seed if needed (or keep active for audit verification)
SAVEPOINT sample_data_start;

-- ----------------------------------------------------------------------------
-- 1. SEED USERS
-- Password hashes for testing:
-- Admin@123   -> $2a$10$wT0XkO0b54rBw31v3QzT4.o9MvFvWzVjDlh1K11y16c6vj0yUeG1e (or PBKDF2 representation)
-- Student@123 -> $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi
-- ----------------------------------------------------------------------------
INSERT INTO USERS (user_id, name, email, phone, password_hash, role) VALUES
(1001, 'Campus Admin', 'admin@campus.edu', '9876543210', 'pbkdf2$10000$5a7d6e8f$e9b274291ff979b048590d9bf84db96a0149091816f731dfb8e7fe6db68ab654', 'ADMIN');

INSERT INTO USERS (user_id, name, email, phone, password_hash, role) VALUES
(1002, 'Security Desk Admin', 'security@campus.edu', '9876543211', 'pbkdf2$10000$5a7d6e8f$e9b274291ff979b048590d9bf84db96a0149091816f731dfb8e7fe6db68ab654', 'ADMIN');

INSERT INTO USERS (user_id, name, email, phone, password_hash, role) VALUES
(1003, 'Aarav Sharma', 'aarav.sharma@campus.edu', '9123456780', 'pbkdf2$10000$5a7d6e8f$e9b274291ff979b048590d9bf84db96a0149091816f731dfb8e7fe6db68ab654', 'STUDENT');

INSERT INTO USERS (user_id, name, email, phone, password_hash, role) VALUES
(1004, 'Priya Patel', 'priya.patel@campus.edu', '9123456781', 'pbkdf2$10000$5a7d6e8f$e9b274291ff979b048590d9bf84db96a0149091816f731dfb8e7fe6db68ab654', 'STUDENT');

INSERT INTO USERS (user_id, name, email, phone, password_hash, role) VALUES
(1005, 'Rohit Verma', 'rohit.verma@campus.edu', '9123456782', 'pbkdf2$10000$5a7d6e8f$e9b274291ff979b048590d9bf84db96a0149091816f731dfb8e7fe6db68ab654', 'STUDENT');

INSERT INTO USERS (user_id, name, email, phone, password_hash, role) VALUES
(1006, 'Ananya Deshmukh', 'ananya.deshmukh@campus.edu', '9123456783', 'pbkdf2$10000$5a7d6e8f$e9b274291ff979b048590d9bf84db96a0149091816f731dfb8e7fe6db68ab654', 'STUDENT');

INSERT INTO USERS (user_id, name, email, phone, password_hash, role) VALUES
(1007, 'Karan Mehta', 'karan.mehta@campus.edu', '9123456784', 'pbkdf2$10000$5a7d6e8f$e9b274291ff979b048590d9bf84db96a0149091816f731dfb8e7fe6db68ab654', 'STUDENT');

INSERT INTO USERS (user_id, name, email, phone, password_hash, role) VALUES
(1008, 'Sneha Kulkarni', 'sneha.kulkarni@campus.edu', '9123456785', 'pbkdf2$10000$5a7d6e8f$e9b274291ff979b048590d9bf84db96a0149091816f731dfb8e7fe6db68ab654', 'STUDENT');

-- ----------------------------------------------------------------------------
-- 2. SEED ITEMS
-- ----------------------------------------------------------------------------
-- Item 2001: Casio Scientific Calculator
INSERT INTO ITEMS (item_id, item_name, category, description, color, image_url) VALUES
(2001, 'Casio fx-991EX Scientific Calculator', 'Electronics', 'Black Casio scientific calculator with solar panel and transparent sliding hard case.', 'Black', '/assets/items/calculator.jpg');

-- Item 2002: Found Casio Scientific Calculator (Matching 2001)
INSERT INTO ITEMS (item_id, item_name, category, description, color, image_url) VALUES
(2002, 'Casio fx-991EX Calculator', 'Electronics', 'Found on 3rd bench in Central Library reading hall. Has battery & solar cell.', 'Black', '/assets/items/calculator.jpg');

-- Item 2003: Apple AirPods Pro 2nd Gen
INSERT INTO ITEMS (item_id, item_name, category, description, color, image_url) VALUES
(2003, 'Apple AirPods Pro Gen 2', 'Electronics', 'White wireless earbuds in MagSafe charging case with lanyard loop.', 'White', '/assets/items/airpods.jpg');

-- Item 2004: Found Wireless Earbuds
INSERT INTO ITEMS (item_id, item_name, category, description, color, image_url) VALUES
(2004, 'Apple AirPods Pro Wireless Case', 'Electronics', 'White charging case found on table outside CS Seminar Hall.', 'White', '/assets/items/airpods.jpg');

-- Item 2005: Student ID Card
INSERT INTO ITEMS (item_id, item_name, category, description, color, image_url) VALUES
(2005, 'College ID Card - Computer Dept', 'Documents', 'Laminated college RFID identity card with blue lanyard.', 'Blue', '/assets/items/idcard.jpg');

-- Item 2006: Found Student ID Card
INSERT INTO ITEMS (item_id, item_name, category, description, color, image_url) VALUES
(2006, 'College Student ID Card', 'Documents', 'RFID ID card found near Campus Cafeteria billing counter.', 'Blue', '/assets/items/idcard.jpg');

-- Item 2007: Titan Fastrack Watch
INSERT INTO ITEMS (item_id, item_name, category, description, color, image_url) VALUES
(2007, 'Fastrack Analog Watch', 'Accessories', 'Analog wrist watch with dark brown leather strap and silver dial.', 'Brown', '/assets/items/watch.jpg');

-- Item 2008: Found Titan Watch
INSERT INTO ITEMS (item_id, item_name, category, description, color, image_url) VALUES
(2008, 'Fastrack Leather Strap Watch', 'Accessories', 'Brown strap wrist watch picked up from Sports Complex pavilion.', 'Brown', '/assets/items/watch.jpg');

-- Item 2009: Wildcraft Blue Backpack
INSERT INTO ITEMS (item_id, item_name, category, description, color, image_url) VALUES
(2009, 'Wildcraft College Backpack', 'Personal Items', '3-compartment navy blue backpack with orange accents and rain cover in base.', 'Blue', '/assets/items/backpack.jpg');

-- Item 2010: Parker Fountain Pen
INSERT INTO ITEMS (item_id, item_name, category, description, color, image_url) VALUES
(2010, 'Parker Vector Matte Black Pen', 'Stationery', 'Metallic matte black rollerball pen with silver arrow clip.', 'Black', '/assets/items/pen.jpg');

-- Item 2011: Sony Noise Cancelling Headphones
INSERT INTO ITEMS (item_id, item_name, category, description, color, image_url) VALUES
(2011, 'Sony WH-1000XM4 Headphones', 'Electronics', 'Over-ear silver grey Bluetooth headphones in zippered carrying case.', 'Grey', '/assets/items/headphones.jpg');

-- Item 2012: Leather Bi-fold Wallet
INSERT INTO ITEMS (item_id, item_name, category, description, color, image_url) VALUES
(2012, 'Tan Leather Bi-fold Wallet', 'Accessories', 'Genuine leather wallet containing transit pass and library card.', 'Tan', '/assets/items/wallet.jpg');

-- ----------------------------------------------------------------------------
-- 3. SEED REPORTS
-- ----------------------------------------------------------------------------
-- Report 3001: LOST Calculator by Aarav
INSERT INTO REPORTS (report_id, item_id, user_id, report_type, location, report_date, status, private_details) VALUES
(3001, 2001, 1003, 'LOST', 'Central Library Reading Hall', DATE '2026-10-15', 'MATCHED', 'Small scratch on back cover near battery screw.');

-- Report 3002: FOUND Calculator by Priya (Match for 3001)
INSERT INTO REPORTS (report_id, item_id, user_id, report_type, location, report_date, status, private_details) VALUES
(3002, 2002, 1004, 'FOUND', 'Central Library Reading Hall', DATE '2026-10-15', 'MATCHED', 'Initials A.S. faintly etched on sliding lid interior.');

-- Report 3003: LOST AirPods by Rohit
INSERT INTO REPORTS (report_id, item_id, user_id, report_type, location, report_date, status, private_details) VALUES
(3003, 2003, 1005, 'LOST', 'Computer Science Seminar Hall', DATE '2026-10-18', 'MATCHED', 'Serial: H7K92P8L1X.');

-- Report 3004: FOUND AirPods by Ananya (Match for 3003)
INSERT INTO REPORTS (report_id, item_id, user_id, report_type, location, report_date, status, private_details) VALUES
(3004, 2004, 1006, 'FOUND', 'Computer Science Seminar Hall', DATE '2026-10-19', 'MATCHED', 'Custom green silicone lanyard attached.');

-- Report 3005: LOST ID Card by Sneha
INSERT INTO REPORTS (report_id, item_id, user_id, report_type, location, report_date, status, private_details) VALUES
(3005, 2005, 1008, 'LOST', 'Campus Cafeteria', DATE '2026-10-20', 'RETURNED', 'Roll Number: 23CE1042.');

-- Report 3006: FOUND ID Card by Karan (Match for 3005, successfully claimed & returned!)
INSERT INTO REPORTS (report_id, item_id, user_id, report_type, location, report_date, status, private_details) VALUES
(3006, 2006, 1007, 'FOUND', 'Campus Cafeteria', DATE '2026-10-20', 'RETURNED', 'Handed over to Student Affairs office counter.');

-- Report 3007: LOST Watch by Karan
INSERT INTO REPORTS (report_id, item_id, user_id, report_type, location, report_date, status, private_details) VALUES
(3007, 2007, 1007, 'LOST', 'Sports Complex Pavilion', DATE '2026-10-22', 'OPEN', 'Engraved graduation gift inscription on stainless back.');

-- Report 3008: FOUND Watch by Priya (Match for 3007)
INSERT INTO REPORTS (report_id, item_id, user_id, report_type, location, report_date, status, private_details) VALUES
(3008, 2008, 1004, 'FOUND', 'Sports Complex Pavilion', DATE '2026-10-23', 'OPEN', 'Found near the badminton court spectator bench.');

-- Report 3009: LOST Backpack by Aarav
INSERT INTO REPORTS (report_id, item_id, user_id, report_type, location, report_date, status, private_details) VALUES
(3009, 2009, 1003, 'LOST', 'Mechanical Workshop Block B', DATE '2026-10-24', 'OPEN', 'Contains engineering drawing sheets and compass set.');

-- Report 3010: LOST Parker Pen by Rohit
INSERT INTO REPORTS (report_id, item_id, user_id, report_type, location, report_date, status, private_details) VALUES
(3010, 2010, 1005, 'LOST', 'Room 304 Lecture Hall', DATE '2026-10-25', 'OPEN', 'Blue ink cartridge loaded.');

-- Report 3011: FOUND Sony Headphones by Sneha
INSERT INTO REPORTS (report_id, item_id, user_id, report_type, location, report_date, status, private_details) VALUES
(3011, 2011, 1008, 'FOUND', 'Main Auditorium 2nd Floor', DATE '2026-10-26', 'OPEN', 'Black case with USB-C cable inside.');

-- Report 3012: FOUND Leather Wallet by Karan
INSERT INTO REPORTS (report_id, item_id, user_id, report_type, location, report_date, status, private_details) VALUES
(3012, 2012, 1007, 'FOUND', 'Campus Bus Stop Shelter', DATE '2026-10-27', 'OPEN', 'No cash inside; contain two metro smart cards.');

-- ----------------------------------------------------------------------------
-- 4. SEED MATCHES (Computed via 4-point Smart Matching Algorithm)
-- Match criteria: Name (1pt), Category (1pt), Color (1pt), Location (1pt)
-- ----------------------------------------------------------------------------
-- Match 4001: Calculator (Lost 3001 vs Found 3002) -> Perfect 4.0 Match!
INSERT INTO MATCHES (match_id, lost_report_id, found_report_id, match_score, match_status) VALUES
(4001, 3001, 3002, 4.0, 'CLAIMED');

-- Match 4002: AirPods (Lost 3003 vs Found 3004) -> 4.0 Strong Match
INSERT INTO MATCHES (match_id, lost_report_id, found_report_id, match_score, match_status) VALUES
(4002, 3003, 3004, 4.0, 'PENDING');

-- Match 4003: ID Card (Lost 3005 vs Found 3006) -> 4.0 Perfect Match (Completed)
INSERT INTO MATCHES (match_id, lost_report_id, found_report_id, match_score, match_status) VALUES
(4003, 3005, 3006, 4.0, 'CONFIRMED');

-- Match 4004: Fastrack Watch (Lost 3007 vs Found 3008) -> 4.0 Perfect Match
INSERT INTO MATCHES (match_id, lost_report_id, found_report_id, match_score, match_status) VALUES
(4004, 3007, 3008, 4.0, 'PENDING');

-- ----------------------------------------------------------------------------
-- 5. SEED CLAIMS
-- ----------------------------------------------------------------------------
-- Claim 5001: Aarav claims Calculator (PENDING review by admin)
INSERT INTO CLAIMS (claim_id, match_id, claimant_id, claim_date, claim_status, reviewed_by, review_date, verification_details, admin_comments) VALUES
(5001, 4001, 1003, DATE '2026-10-16', 'PENDING', NULL, NULL, 
 'The calculator has my initials A.S. scratched on the inside of the sliding protective cover, and the reset button has a light blue ink smudge.', 
 NULL);

-- Claim 5002: Sneha claimed ID Card (APPROVED by Admin 1001) -> RETURNED
INSERT INTO CLAIMS (claim_id, match_id, claimant_id, claim_date, claim_status, reviewed_by, review_date, verification_details, admin_comments) VALUES
(5002, 4003, 1008, DATE '2026-10-21', 'APPROVED', 1001, DATE '2026-10-21', 
 'Matched government Aadhaar photo and student portal enrollment record 23CE1042.', 
 'Identity verified with college admission registrar record. Physical card returned to student.');

-- Claim 5003: Rohit submitted invalid claim for Headphones (REJECTED by Admin)
INSERT INTO CLAIMS (claim_id, match_id, claimant_id, claim_date, claim_status, reviewed_by, review_date, verification_details, admin_comments) VALUES
(5003, 4002, 1005, DATE '2026-10-20', 'REJECTED', 1001, DATE '2026-10-21', 
 'Claimed he lost black AirPods, but the found item in report 3004 is white with custom serial.', 
 'Color and serial mismatch with proof submitted.');

-- ----------------------------------------------------------------------------
-- 6. SEED NOTIFICATIONS
-- ----------------------------------------------------------------------------
INSERT INTO NOTIFICATIONS (notification_id, user_id, match_id, message, notification_date, read_status) VALUES
(6001, 1003, 4001, 'A high-confidence potential match (Score: 4/4) was found for your Casio fx-991EX Scientific Calculator!', TIMESTAMP '2026-10-15 16:30:00', 'READ');

INSERT INTO NOTIFICATIONS (notification_id, user_id, match_id, message, notification_date, read_status) VALUES
(6002, 1004, 4001, 'Student Aarav Sharma has submitted an ownership claim for the Casio Calculator you turned in.', TIMESTAMP '2026-10-16 10:15:00', 'UNREAD');

INSERT INTO NOTIFICATIONS (notification_id, user_id, match_id, message, notification_date, read_status) VALUES
(6003, 1008, 4003, 'Your claim #5002 for College ID Card has been APPROVED! Please collect it from Admin Desk.', TIMESTAMP '2026-10-21 14:00:00', 'READ');

INSERT INTO NOTIFICATIONS (notification_id, user_id, match_id, message, notification_date, read_status) VALUES
(6004, 1007, 4004, 'Potential match (Score: 4/4) detected for your Fastrack watch found at Sports Complex!', TIMESTAMP '2026-10-23 11:20:00', 'UNREAD');

INSERT INTO NOTIFICATIONS (notification_id, user_id, match_id, message, notification_date, read_status) VALUES
(6005, 1005, 4002, 'Update on claim #5003: Your claim was reviewed and REJECTED due to specification mismatch.', TIMESTAMP '2026-10-21 15:45:00', 'UNREAD');

-- ----------------------------------------------------------------------------
-- 7. SEED AUDIT_LOG
-- ----------------------------------------------------------------------------
INSERT INTO AUDIT_LOG (audit_id, user_id, operation, table_name, record_id, details, log_date) VALUES
(7001, 1003, 'INSERT', 'REPORTS', 3001, 'Reported lost Casio Calculator fx-991EX at Central Library.', TIMESTAMP '2026-10-15 14:10:00');

INSERT INTO AUDIT_LOG (audit_id, user_id, operation, table_name, record_id, details, log_date) VALUES
(7002, 1004, 'INSERT', 'REPORTS', 3002, 'Reported found Casio Calculator at Central Library.', TIMESTAMP '2026-10-15 15:40:00');

INSERT INTO AUDIT_LOG (audit_id, user_id, operation, table_name, record_id, details, log_date) VALUES
(7003, 1003, 'INSERT', 'CLAIMS', 5001, 'Submitted ownership claim for match #4001.', TIMESTAMP '2026-10-16 10:14:00');

INSERT INTO AUDIT_LOG (audit_id, user_id, operation, table_name, record_id, details, log_date) VALUES
(7004, 1001, 'APPROVE', 'CLAIMS', 5002, 'Approved claim #5002 submitted by Sneha Kulkarni for ID Card.', TIMESTAMP '2026-10-21 13:58:00');

INSERT INTO AUDIT_LOG (audit_id, user_id, operation, table_name, record_id, details, log_date) VALUES
(7005, 1001, 'UPDATE', 'REPORTS', 3005, 'Status transitioned from MATCHED to RETURNED following claim approval.', TIMESTAMP '2026-10-21 13:58:30');

INSERT INTO AUDIT_LOG (audit_id, user_id, operation, table_name, record_id, details, log_date) VALUES
(7006, 1001, 'REJECT', 'CLAIMS', 5003, 'Rejected claim #5003 submitted by Rohit Verma due to mismatch.', TIMESTAMP '2026-10-21 15:44:00');

-- ----------------------------------------------------------------------------
-- 8. SEED TRANSACTION_LOG
-- ----------------------------------------------------------------------------
INSERT INTO TRANSACTION_LOG (log_id, transaction_id, operation, table_name, record_id, old_value, new_value, log_status, log_date) VALUES
(8001, 'TXN-20261021-001', 'UPDATE', 'CLAIMS', 5002, 'claim_status=PENDING', 'claim_status=APPROVED, reviewed_by=1001', 'COMMITTED', TIMESTAMP '2026-10-21 13:58:00');

INSERT INTO TRANSACTION_LOG (log_id, transaction_id, operation, table_name, record_id, old_value, new_value, log_status, log_date) VALUES
(8002, 'TXN-20261021-001', 'UPDATE', 'REPORTS', 3005, 'status=MATCHED', 'status=RETURNED', 'COMMITTED', TIMESTAMP '2026-10-21 13:58:10');

INSERT INTO TRANSACTION_LOG (log_id, transaction_id, operation, table_name, record_id, old_value, new_value, log_status, log_date) VALUES
(8003, 'TXN-20261021-001', 'UPDATE', 'REPORTS', 3006, 'status=MATCHED', 'status=RETURNED', 'COMMITTED', TIMESTAMP '2026-10-21 13:58:20');

COMMIT;

PROMPT Realistic sample data populated successfully across all 8 tables!

