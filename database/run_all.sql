-- ============================================================================
-- FindIt - Campus Lost & Found Management System
-- Master Script: run_all.sql
-- Description: Executes the complete database setup suite in correct order.
-- Usage: SQL> @database/run_all.sql
-- ============================================================================

SET SERVEROUTPUT ON;
SET ECHO ON;
SET FEEDBACK ON;

PROMPT ========================================================================
PROMPT EXECUTING FINDIT DATABASE INSTALLATION AND DEMONSTRATION SUITE
PROMPT ========================================================================

PROMPT Step 1: Creating 8 core tables and sequences...
@@01_create_tables.sql;

PROMPT Step 2: Creating indexes and integrity constraints...
@@02_constraints_indexes.sql;

PROMPT Step 3: Populating realistic campus sample dataset...
@@03_sample_data.sql;

PROMPT Step 4: Executing DDL and DML operations...
@@04_ddl_dml.sql;

PROMPT Step 5: Testing simple and advanced queries (WHERE, subqueries, aggregates)...
@@05_advanced_queries.sql;

PROMPT Step 6: Creating views and testing join operations...
@@06_joins_views.sql;

PROMPT Step 7: Configuring DCL security roles and testing TCL transactions...
@@07_dcl_tcl.sql;

PROMPT Step 8: Compiling PL/SQL blocks, functions and stored procedures...
@@08_plsql.sql;

PROMPT Step 9: Running explicit and parameterized cursors...
@@09_cursors.sql;

PROMPT Step 10: Compiling and activating audit and validation triggers...
@@10_triggers.sql;

PROMPT Step 11: Reviewing concurrency, locking, and deadlock scripts...
@@11_concurrency.sql;

PROMPT Step 12: Reviewing ACID transaction recovery and flashback mechanisms...
@@12_recovery.sql;

PROMPT ========================================================================
PROMPT ALL FINDIT ORACLE DATABASE SCRIPTS COMPLETED SUCCESSFULLY!
PROMPT ========================================================================

