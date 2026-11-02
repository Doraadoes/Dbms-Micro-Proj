# FindIt – Campus Lost & Found Management System

> **DBMS Microproject – Academic Edition**  
> Computer Engineering • Semester 3  
> *“Lost something? Found something? Let's bring it back.”*

---

## 1. Project Overview

**FindIt** is a centralised, full-stack campus lost and found management platform designed to replace ad-hoc campus WhatsApp groups and notice boards with an audited, privacy-conscious relational system. 

Built specifically for a Semester 3 Database Management Systems (DBMS) microproject, it demonstrates real-world implementation of advanced relational database concepts using **Oracle SQL**, **PL/SQL**, **JDBC parameterised transactions**, and a modern web interface.

### Key Capabilities:
- **Role-Based Workflows**: Separate, secure access pathways for **Students** and **Administrators**.
- **Privacy Protection**: Masks sensitive identifying marks (serials, private engraving, contents) from public search queries to prevent fraudulent claims.
- **4-Point Smart Matching Algorithm**: Correlates lost and found reports using an automated scoring formula based on Item Name, Category, Colour, and Location.
- **Audited Ownership Claims**: Students file proof of ownership; administrators approve or reject claims inside an **atomic multi-table database transaction**.
- **Real-Time Notification Centre**: Automated in-app alerts on match discoveries, claim submissions, and administrative verdicts.
- **Full DBMS Concept Coverage**: DDL, DML, DCL, TCL, Indexes, Joins, Views, PL/SQL blocks/procedures/functions, Cursors, Triggers, Concurrency locking, and ACID recovery.

---

## 2. Technology Stack

| Layer | Technology | Description |
|---|---|---|
| **Database** | Oracle Database (19c / 21c / 23c XE) & PL/SQL | 8 relational tables, sequences, views, stored procedures, functions, cursors, triggers |
| **Backend** | Java REST API (JDK 21+ / 25 LTS) | Clean Controller-Service-Repository architecture with HTTP router and JSON serializer |
| **Connectivity** | JDBC (`java.sql.PreparedStatement`) | Parameterised queries, explicit transaction boundaries (`commit`, `rollback`, `savepoint`) |
| **Security** | PBKDF2-HMAC-SHA256 & Session Tokens | Constant-time password hashing with cryptographic salt, role-based access control (RBAC) |
| **Frontend** | HTML5, CSS3, ES6+ JavaScript | Modern Single-Page Application (SPA), dark forest green & muted golden beige palette |
| **Icons & UI** | SVG & Lucide Icons | Responsive layout for desktop, tablet, and mobile devices |

---

## 3. Design & Brand Identity

- **Primary Brand**: Elegant Dark Forest Green (`#153225`, `#1c4433`, `#0d2118`)
- **Background**: Warm Off-White and Light Cream (`#fbfbfa`, `#ffffff`, `#f4f6f4`)
- **Accent**: Muted Golden Beige (`#c5a059`, `#d4af37`, `#f5eedf`)
- **Cards & Elements**: Rounded cards (`border-radius: 14px`), subtle elevation shadows (`0 8px 24px rgba(21, 50, 37, 0.07)`), crisp modern typography.
- **Logo**: Precision magnifying glass combined with a location pin and checkmark.

---

## 4. Project Directory Structure

```
findit/
├── backend/
│   ├── pom.xml                                  # Maven project descriptor (standard build)
│   ├── src/main/resources/
│   │   └── application.properties               # Oracle connection & server port settings
│   └── src/main/java/com/findit/
│       ├── Application.java                     # Main server entry point
│       ├── config/
│       │   └── AppConfig.java                   # Configuration loader
│       ├── controllers/
│       │   ├── ApiController.java               # REST API route handlers
│       │   └── StaticFileHandler.java           # SPA static file server
│       ├── database/
│       │   ├── DatabaseManager.java             # Universal database interface
│       │   ├── OracleDatabaseProvider.java      # Oracle JDBC implementation
│       │   ├── InMemoryDatabaseProvider.java    # Active in-process SQL provider (Oracle schema)
│       │   └── DatabaseFactory.java             # Dynamic provider resolver
│       ├── models/                              # Data models (User, Item, Report, Match, Claim, etc.)
│       ├── repositories/                        # JDBC/SQL repositories (User, Report, Claim, etc.)
│       ├── security/
│       │   ├── PasswordHasher.java              # PBKDF2 with HMAC-SHA256
│       │   ├── SessionManager.java              # Bearer session token management
│       │   └── InputValidator.java              # Sanitization & regex validations
│       ├── services/                            # Business logic (Auth, Report, Matching, Claim, etc.)
│       └── util/
│           └── JsonUtil.java                    # Zero-dependency JSON parser & serializer
├── frontend/
│   ├── index.html                               # Modern SPA container & modals
│   ├── css/
│   │   └── styles.css                           # Forest green & golden beige design system
│   ├── js/
│   │   ├── api.js                               # Fetch API client with Bearer auth
│   │   └── app.js                               # Views rendering & interactive workflows
│   └── assets/
│       └── logo.svg                             # FindIt brand logo
├── database/
│   ├── run_all.sql                              # Master execution script for SQL*Plus
│   ├── 01_create_tables.sql                     # 8 core relational tables & sequences
│   ├── 02_constraints_indexes.sql               # Indexes & domain check constraints
│   ├── 03_sample_data.sql                       # Realistic campus test dataset
│   ├── 04_ddl_dml.sql                           # DDL (ALTER, DROP, TRUNCATE, RENAME) & DML
│   ├── 05_advanced_queries.sql                  # Subqueries, set ops, string functions, aggregates
│   ├── 06_joins_views.sql                       # 7 SQL join types & 4 database views
│   ├── 07_dcl_tcl.sql                           # Security roles (GRANT/REVOKE) & transactions
│   ├── 08_plsql.sql                             # Stored procedures, functions, exceptions
│   ├── 09_cursors.sql                           # Explicit & parameterized cursors, cursor loops
│   ├── 10_triggers.sql                          # Audit, relationship validation, & notification triggers
│   ├── 11_concurrency.sql                       # Row locking, SELECT FOR UPDATE, & deadlock demo
│   └── 12_recovery.sql                          # UNDO, REDO, SAVEPOINT, & Flashback queries
├── documentation/
│   ├── ER_diagram.md                            # Entity-Relationship diagram & cardinality
│   ├── relational_schema.md                     # Schema notation & normalization proof (1NF-BCNF)
│   └── setup_guide.md                           # Oracle setup & viva presentation guide
├── build.sh                                     # Automated compilation script
├── run.sh                                       # Server start script
└── README.md
```

---

## 5. Relational Database Design (8 Core Tables)

1. **`USERS`**: Stores student and admin credentials, roles, and profiles.
2. **`ITEMS`**: Physical and descriptive properties of items (`category`, `color`, `description`).
3. **`REPORTS`**: Event entity linking items to reporting users with `report_type` (`LOST`/`FOUND`), `location`, `report_date`, and masked `private_details`.
4. **`MATCHES`**: Pairs a `LOST` report with a `FOUND` report along with a computed `match_score` (0.0 to 4.0).
5. **`CLAIMS`**: Ownership claims submitted by students with verification proof and administrative verdict.
6. **`NOTIFICATIONS`**: In-app alerts for match discoveries, claim submissions, and review results.
7. **`AUDIT_LOG`**: Permanent security log recording user, table, record, operation, and timestamp.
8. **`TRANSACTION_LOG`**: Before-and-after state transitions capturing multi-table atomic operations.

---

## 6. The 4-Point Smart Matching Algorithm

The matching engine compares active `LOST` and `FOUND` reports using four independent criteria:

$$\text{Match Score} = \text{Name Match} + \text{Category Match} + \text{Colour Match} + \text{Location Match}$$

- **Item Name Match** (1.0 pt): Normalized keyword/token or substring match.
- **Category Match** (1.0 pt): Exact category alignment.
- **Colour Match** (1.0 pt): Color equality or common color variation.
- **Location Match** (1.0 pt): Campus landmark or location proximity.
- **Maximum Score**: `4.0` points (Score $\ge 3.0$ triggers a **Strong Potential Match** notification).

---

## 7. Syllabus DBMS Concepts Mapped to Code

| Syllabus Requirement | Concept Description | Implementation File |
|---|---|---|
| **DDL** | `CREATE`, `ALTER` (add/modify/drop column), `DROP`, `TRUNCATE`, `RENAME` | [`database/04_ddl_dml.sql`](database/04_ddl_dml.sql) |
| **DML** | `INSERT` (single & subquery CTAS), `UPDATE`, `DELETE`, `SELECT` | [`database/04_ddl_dml.sql`](database/04_ddl_dml.sql) |
| **SQL Constraints** | PK, FK (CASCADE/SET NULL), UNIQUE, NOT NULL, CHECK | [`database/01_create_tables.sql`](database/01_create_tables.sql), [`02_constraints_indexes.sql`](database/02_constraints_indexes.sql) |
| **Advanced Queries** | `WHERE`, `DISTINCT`, `BETWEEN`, `IN`, `LIKE`, nested subqueries, `EXISTS`, `NOT EXISTS` | [`database/05_advanced_queries.sql`](database/05_advanced_queries.sql) |
| **Set Operations** | `UNION`, `UNION ALL`, `INTERSECT`, `MINUS` | [`database/05_advanced_queries.sql`](database/05_advanced_queries.sql) |
| **Aggregates** | `COUNT`, `SUM`, `AVG`, `MIN`, `MAX`, `GROUP BY`, `HAVING` | [`database/05_advanced_queries.sql`](database/05_advanced_queries.sql) |
| **Joins** | Inner, Left, Right, Full Outer, Cross, Self, Equi, Non-Equi (Theta) | [`database/06_joins_views.sql`](database/06_joins_views.sql) |
| **Views** | `V_LOST_ITEMS`, `V_FOUND_ITEMS`, `V_PENDING_CLAIMS`, `V_RETURNED_ITEMS` | [`database/06_joins_views.sql`](database/06_joins_views.sql) |
| **DCL** | `CREATE ROLE`, `GRANT` (least privilege), `REVOKE` | [`database/07_dcl_tcl.sql`](database/07_dcl_tcl.sql) |
| **TCL** | `COMMIT`, `ROLLBACK`, `SAVEPOINT`, `SET TRANSACTION READ ONLY` | [`database/07_dcl_tcl.sql`](database/07_dcl_tcl.sql) |
| **PL/SQL** | Anonymous blocks, IF-ELSE, CASE, loops, variables, constants | [`database/08_plsql.sql`](database/08_plsql.sql) |
| **Procedures & Functions** | `SP_APPROVE_CLAIM`, `FN_CALCULATE_MATCH_SCORE`, `FN_GET_RECOVERY_RATE` | [`database/08_plsql.sql`](database/08_plsql.sql) |
| **Cursors** | Explicit cursors (`OPEN`/`FETCH`/`CLOSE`), parameterized cursors, cursor attributes (`%FOUND`, `%NOTFOUND`, `%ROWCOUNT`), cursor FOR loops | [`database/09_cursors.sql`](database/09_cursors.sql) |
| **Triggers** | `TRG_VALIDATE_MATCH_PAIR`, `TRG_AUDIT_REPORT_STATUS`, `TRG_AUDIT_CLAIM_STATUS`, `TRG_NOTIFY_ON_STRONG_MATCH` | [`database/10_triggers.sql`](database/10_triggers.sql) |
| **Concurrency & Locks** | `SELECT ... FOR UPDATE`, row-level locking, table SHARE/EXCLUSIVE locks, deadlock demonstration (ORA-00060) | [`database/11_concurrency.sql`](database/11_concurrency.sql) |
| **Recovery** | Atomicity via UNDO, Durability via REDO (Write-Ahead Logging), SAVEPOINT recovery, Oracle Flashback queries | [`database/12_recovery.sql`](database/12_recovery.sql) |
| **SQL Injection Defense** | Parameterised queries, `PreparedStatement`, strict input sanitization | [`backend/src/main/java/com/findit/repositories/`](backend/src/main/java/com/findit/repositories/) |

---

## 8. Quick Start & Execution

### 1. Launch the Application
Run the execution script from the root directory:
```bash
./run.sh
```
*(The script automatically compiles all Java sources with JDK and starts the server on port 8080).*

### 2. Open in Browser
Visit: **`http://localhost:8080/`**

### 3. Demo Accounts for Evaluation:

| Account | Email | Password | Role |
|---|---|---|---|
| **Admin** | `admin@campus.edu` | `Admin@123` | Administrator (Claims Queue, Audit Log, User Mgmt) |
| **Student** | `aarav.sharma@campus.edu` | `Student@123` | Student (Lost Calculator report, Active Claims) |
| **Student** | `priya.patel@campus.edu` | `Student@123` | Student (Found items, Match correlations) |

*(Quick-login buttons are also provided on the landing page and login modal for instant evaluation).*

---

## 9. Oracle Database Setup (Optional for Live External Oracle)

FindIt includes an active in-process relational database provider that runs immediately out of the box with zero external daemon requirements, while full Oracle SQL scripts (01 to 12) are provided in `/database`.

To connect to a live Oracle Database 19c/21c/23c XE:
1. Run `sqlplus findit_user/findit_password@localhost:1521/XEPDB1 @database/run_all.sql`.
2. Set `db.mode=ORACLE` in `backend/src/main/resources/application.properties`.
3. Restart `./run.sh`. FindIt will connect via Oracle JDBC!

