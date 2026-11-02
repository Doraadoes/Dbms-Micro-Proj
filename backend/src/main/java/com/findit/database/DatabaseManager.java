package com.findit.database;

import java.util.List;
import java.util.Map;

/**
 * Universal Database Interface providing parameterised queries, transactions,
 * and seamless switching between Oracle SQL Database and the active SQL provider.
 */
public interface DatabaseManager {
    void initialize();
    boolean isOracleActive();
    String getDatabaseStatusDescription();

    List<Map<String, Object>> query(String sql, Object... params);
    int executeUpdate(String sql, Object... params);
    long executeInsert(String sql, Object... params);

    void beginTransaction();
    void commit();
    void rollback();
    void savepoint(String name);
    void rollbackToSavepoint(String name);
}

