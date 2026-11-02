package com.findit.repositories;

import com.findit.database.DatabaseFactory;
import com.findit.database.DatabaseManager;
import com.findit.database.InMemoryDatabaseProvider;
import com.findit.models.AuditLog;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AuditRepository {
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DatabaseManager db = DatabaseFactory.getInstance();

    public void log(Long userId, String operation, String tableName, Long recordId, String details) {
        String now = LocalDateTime.now().format(ISO_FMT);
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            long auditId = inMem.nextId("SEQ_AUDIT_LOG");
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("audit_id", auditId);
            r.put("user_id", userId);
            r.put("operation", operation);
            r.put("table_name", tableName);
            r.put("record_id", recordId);
            r.put("details", details);
            r.put("log_date", now);
            inMem.getTables().get("AUDIT_LOG").put(auditId, r);
        } else {
            String sql = "INSERT INTO AUDIT_LOG (audit_id, user_id, operation, table_name, record_id, details, log_date) " +
                         "VALUES (SEQ_AUDIT_LOG.NEXTVAL, ?, ?, ?, ?, ?, SYSTIMESTAMP)";
            db.executeInsert(sql, userId, operation, tableName, recordId, details);
        }
    }

    public void logTransaction(String txnId, String operation, String tableName, Long recordId, String oldVal, String newVal, String status) {
        String now = LocalDateTime.now().format(ISO_FMT);
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            long logId = inMem.nextId("SEQ_TRANSACTION_LOG");
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("log_id", logId);
            r.put("transaction_id", txnId);
            r.put("operation", operation);
            r.put("table_name", tableName);
            r.put("record_id", recordId);
            r.put("old_value", oldVal);
            r.put("new_value", newVal);
            r.put("log_status", status);
            r.put("log_date", now);
            inMem.getTables().get("TRANSACTION_LOG").put(logId, r);
        } else {
            String sql = "INSERT INTO TRANSACTION_LOG (log_id, transaction_id, operation, table_name, record_id, old_value, new_value, log_status, log_date) " +
                         "VALUES (SEQ_TRANSACTION_LOG.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)";
            db.executeInsert(sql, txnId, operation, tableName, recordId, oldVal, newVal, status);
        }
    }

    public List<AuditLog> findAll() {
        List<AuditLog> list = new ArrayList<>();
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            for (Map<String, Object> r : inMem.getTables().get("AUDIT_LOG").values()) {
                AuditLog log = mapAudit(r);
                if (log.getUserId() != null) {
                    Map<String, Object> u = inMem.getTables().get("USERS").get(log.getUserId());
                    if (u != null) log.setUserName(getString(u, "name"));
                }
                list.add(log);
            }
        } else {
            String sql = "SELECT a.*, u.name AS user_name " +
                         "FROM AUDIT_LOG a " +
                         "LEFT JOIN USERS u ON a.user_id = u.user_id " +
                         "ORDER BY a.log_date DESC";
            List<Map<String, Object>> rows = db.query(sql);
            for (Map<String, Object> r : rows) {
                AuditLog log = mapAudit(r);
                log.setUserName(getString(r, "user_name"));
                list.add(log);
            }
        }
        list.sort((a, b) -> Long.compare(b.getAuditId(), a.getAuditId()));
        return list;
    }

    private AuditLog mapAudit(Map<String, Object> r) {
        AuditLog a = new AuditLog();
        a.setAuditId(getLong(r, "audit_id"));
        a.setUserId(getLong(r, "user_id"));
        a.setOperation(getString(r, "operation"));
        a.setTableName(getString(r, "table_name"));
        a.setRecordId(getLong(r, "record_id"));
        a.setDetails(getString(r, "details"));
        a.setLogDate(getString(r, "log_date"));
        return a;
    }

    private Long getLong(Map<String, Object> r, String key) {
        Object o = r.get(key);
        if (o == null) o = r.get(key.toUpperCase());
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        return Long.parseLong(o.toString());
    }

    private String getString(Map<String, Object> r, String key) {
        Object o = r.get(key);
        if (o == null) o = r.get(key.toUpperCase());
        return o != null ? o.toString() : null;
    }
}

