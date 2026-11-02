package com.findit.database;

import com.findit.config.AppConfig;
import java.sql.*;
import java.util.*;

/**
 * Enterprise Oracle Database Provider utilizing JDBC PreparedStatement,
 * connection pooling semantics, and full transaction control.
 */
public class OracleDatabaseProvider implements DatabaseManager {
    private String url;
    private String user;
    private String password;
    private boolean connected = false;
    private Connection currentTransactionConn = null;

    public OracleDatabaseProvider() {
        this.url = AppConfig.ORACLE_URL;
        this.user = AppConfig.ORACLE_USER;
        this.password = AppConfig.ORACLE_PASSWORD;
    }

    @Override
    public void initialize() {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                this.connected = true;
                System.out.println("[Oracle DB] Successfully connected to Oracle Database at " + url);
            }
        } catch (ClassNotFoundException e) {
            this.connected = false;
            System.err.println("[Oracle DB] Oracle JDBC Driver (ojdbc8) not found on classpath.");
        } catch (SQLException e) {
            this.connected = false;
            System.err.println("[Oracle DB] Could not connect to Oracle at " + url + ": " + e.getMessage());
        }
    }

    private Connection getConnection() throws SQLException {
        if (currentTransactionConn != null && !currentTransactionConn.isClosed()) {
            return currentTransactionConn;
        }
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public boolean isOracleActive() {
        return connected;
    }

    @Override
    public String getDatabaseStatusDescription() {
        if (connected) {
            return "Connected to Oracle Database 19c/21c/23c via JDBC (" + url + ")";
        } else {
            return "Oracle Database offline at " + url;
        }
    }

    @Override
    public List<Map<String, Object>> query(String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            setParameters(stmt, params);
            rs = stmt.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    String colName = meta.getColumnLabel(i);
                    Object val = rs.getObject(i);
                    row.put(colName.toLowerCase(), val);
                }
                results.add(row);
            }
        } catch (SQLException e) {
            System.err.println("[Oracle DB Query Error] SQL: " + sql + " | Error: " + e.getMessage());
            throw new RuntimeException("Oracle Query Failed: " + e.getMessage(), e);
        } finally {
            closeQuietly(rs);
            closeQuietly(stmt);
            if (currentTransactionConn == null) {
                closeQuietly(conn);
            }
        }
        return results;
    }

    @Override
    public int executeUpdate(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            setParameters(stmt, params);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Oracle DB Update Error] SQL: " + sql + " | Error: " + e.getMessage());
            throw new RuntimeException("Oracle Update Failed: " + e.getMessage(), e);
        } finally {
            closeQuietly(stmt);
            if (currentTransactionConn == null) {
                closeQuietly(conn);
            }
        }
    }

    @Override
    public long executeInsert(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            setParameters(stmt, params);
            stmt.executeUpdate();
            rs = stmt.getGeneratedKeys();
            if (rs != null && rs.next()) {
                return rs.getLong(1);
            }
            return 1L;
        } catch (SQLException e) {
            System.err.println("[Oracle DB Insert Error] SQL: " + sql + " | Error: " + e.getMessage());
            throw new RuntimeException("Oracle Insert Failed: " + e.getMessage(), e);
        } finally {
            closeQuietly(rs);
            closeQuietly(stmt);
            if (currentTransactionConn == null) {
                closeQuietly(conn);
            }
        }
    }

    @Override
    public void beginTransaction() {
        try {
            if (currentTransactionConn == null || currentTransactionConn.isClosed()) {
                currentTransactionConn = DriverManager.getConnection(url, user, password);
                currentTransactionConn.setAutoCommit(false);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to begin transaction", e);
        }
    }

    @Override
    public void commit() {
        try {
            if (currentTransactionConn != null && !currentTransactionConn.isClosed()) {
                currentTransactionConn.commit();
                currentTransactionConn.setAutoCommit(true);
                currentTransactionConn.close();
                currentTransactionConn = null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to commit transaction", e);
        }
    }

    @Override
    public void rollback() {
        try {
            if (currentTransactionConn != null && !currentTransactionConn.isClosed()) {
                currentTransactionConn.rollback();
                currentTransactionConn.setAutoCommit(true);
                currentTransactionConn.close();
                currentTransactionConn = null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to rollback transaction", e);
        }
    }

    @Override
    public void savepoint(String name) {
        try {
            if (currentTransactionConn != null) {
                currentTransactionConn.setSavepoint(name);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create savepoint", e);
        }
    }

    @Override
    public void rollbackToSavepoint(String name) {
        try {
            if (currentTransactionConn != null) {
                // Savepoint rollback
                Savepoint sp = currentTransactionConn.setSavepoint(name);
                currentTransactionConn.rollback(sp);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to rollback to savepoint " + name, e);
        }
    }

    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
        }
    }

    private void closeQuietly(AutoCloseable c) {
        if (c != null) {
            try { c.close(); } catch (Exception ignored) {}
        }
    }
}

