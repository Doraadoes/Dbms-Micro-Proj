# FindIt – Oracle Database Setup & Execution Guide

This document provides complete instructions for configuring Oracle Database 19c / 21c / 23c XE, executing the SQL/PL/SQL scripts, and running the FindIt web application.

---

## 1. Oracle Database Configuration

### Step 1.1: Connect to Oracle as SYSDBA
Open your terminal or SQL*Plus and log in with administrative privileges:
```bash
sqlplus sys/YourPassword@localhost:1521/XE as sysdba
```
*(On Oracle 21c/23c XE, switch to the pluggable database `XEPDB1`)*:
```sql
ALTER SESSION SET CONTAINER = XEPDB1;
```

### Step 1.2: Create Dedicated FindIt User Schema
Execute the following commands to create the application schema and grant required privileges:
```sql
-- Create application user
CREATE USER findit_user IDENTIFIED BY findit_password;

-- Grant required database privileges
GRANT CONNECT, RESOURCE TO findit_user;
GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE SEQUENCE, 
      CREATE PROCEDURE, CREATE TRIGGER, CREATE ROLE TO findit_user;

-- Grant tablespace quota
ALTER USER findit_user DEFAULT TABLESPACE USERS QUOTA UNLIMITED ON USERS;

COMMIT;
```

### Step 1.3: Execute Database Setup Suite
Connect as `findit_user` and execute the master installation script:
```bash
sqlplus findit_user/findit_password@localhost:1521/XEPDB1 @database/run_all.sql
```
This executes all 12 SQL scripts in order:
1. `01_create_tables.sql` – 8 relational tables and sequences
2. `02_constraints_indexes.sql` – Performance indexes and integrity constraints
3. `03_sample_data.sql` – Comprehensive campus dataset
4. `04_ddl_dml.sql` – DDL and DML operations demonstration
5. `05_advanced_queries.sql` – WHERE, subqueries, set operations, aggregates
6. `06_joins_views.sql` – Inner, outer, cross, self, non-equi joins & 4 database views
7. `07_dcl_tcl.sql` – Security roles (student/admin) and transaction boundaries
8. `08_plsql.sql` – Stored procedures, functions, and exception handling
9. `09_cursors.sql` – Explicit and parameterized cursors
10. `10_triggers.sql` – Automatic audit, validation, and notification triggers
11. `11_concurrency.sql` – Row locks, SELECT FOR UPDATE, and deadlock demo
12. `12_recovery.sql` – ACID recovery, UNDO, REDO, and flashback queries

---

## 2. Running the Web Application

### Prerequisites:
- Java JDK 21+ or JDK 25 LTS installed
- Standard web browser (Chrome, Edge, Firefox, Safari)

### Method 1: Using the Included Run Script (Recommended)
From the project root directory:
```bash
./run.sh
```
The script will automatically detect `JAVA_HOME`, compile all Java backend sources into `backend/bin`, and start the HTTP server on port 8080.

### Method 2: Manual Build & Execution
```bash
# 1. Compile sources
mkdir -p backend/bin
javac -d backend/bin $(find backend/src/main/java -name "*.java")
cp backend/src/main/resources/application.properties backend/bin/

# 2. Run application
java -cp backend/bin com.findit.Application
```

### Method 3: Using Apache Maven
```bash
cd backend
mvn clean compile exec:java -Dexec.mainClass="com.findit.Application"
```

Once started, open your browser and navigate to:
**http://localhost:8080/**

---

## 3. Database Abstraction & Dual-Mode Architecture

FindIt is built with an enterprise database abstraction layer:
- **Oracle JDBC Mode**: When an active Oracle instance is detected at `localhost:1521/XEPDB1`, FindIt executes all queries, transactions, and stored procedures directly on Oracle Database.
- **In-Process SQL Provider**: If an Oracle daemon is not active on the local machine during development or quick evaluation, FindIt automatically activates its robust in-process relational database engine with zero external dependencies. It implements the exact 8-table schema, sequences, constraints, PreparedStatement mapping, and atomic transactions.

To explicitly force Oracle connectivity, edit `backend/src/main/resources/application.properties`:
```properties
db.mode=ORACLE
oracle.url=jdbc:oracle:thin:@localhost:1521/XEPDB1
oracle.user=findit_user
oracle.password=findit_password
```

---

## 4. Default Demonstration Accounts

| Role | Email | Password | Access Level |
|---|---|---|---|
| **Administrator** | `admin@campus.edu` | `Admin@123` | Full Admin Dashboard, Claims Review Queue, Reports & User Management, Audit Logs |
| **Security Admin**| `security@campus.edu` | `Admin@123` | Security Desk Administrative Access |
| **Student (Aarav)**| `aarav.sharma@campus.edu`| `Student@123`| Student Dashboard, Report Lost Item, Claim Matched Calculator |
| **Student (Priya)**| `priya.patel@campus.edu`| `Student@123`| Student Dashboard, Report Found Items |
| **Student (Sneha)**| `sneha.kulkarni@campus.edu`| `Student@123`| Student Dashboard, Successfully Returned ID Card history |

---

## 5. College Viva Demonstration Workflow

For the practical examination, demonstrate the following end-to-end workflow:
1. **Landing Page**: Observe the live statistics counter fetched from the database and recently reported items.
2. **Student Login**: Click "Login as Student (Aarav)".
3. **Report Lost Item**: Navigate to "Report Lost Item" and file a report for an item (e.g., Scientific Calculator).
4. **Smart Matching**: Observe how the 4-point matching engine (Name, Category, Color, Location) scores candidates and alerts the student with a high-confidence match badge.
5. **Ownership Claim**: Click "Claim This Item" and submit detailed proof of ownership. The claim enters `PENDING` state.
6. **Admin Review & Atomic Transaction**:
   - Log out and log in as `admin@campus.edu`.
   - Go to "Manage Claims".
   - Review the pending claim and click "Approve".
   - Point out that this triggers a single atomic multi-table transaction that updates the Claim to `APPROVED`, both Lost and Found reports to `RETURNED`, generates student notifications, and records entries in `AUDIT_LOG` and `TRANSACTION_LOG`.
7. **Audit Trail**: Open "Audit Logs" to show the complete permanent record of the operation.
8. **Oracle SQL Scripts**: Open the "DBMS & SQL Guide" tab in the application or SQL Developer to show the corresponding PL/SQL stored procedure `SP_APPROVE_CLAIM` in `database/08_plsql.sql`.

