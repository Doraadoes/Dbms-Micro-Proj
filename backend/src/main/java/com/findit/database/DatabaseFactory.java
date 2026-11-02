package com.findit.database;

import com.findit.config.AppConfig;

public class DatabaseFactory {
    private static DatabaseManager instance;

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            String mode = AppConfig.DB_MODE.toUpperCase();
            if ("ORACLE".equals(mode)) {
                System.out.println("[DB Initialization] Mode: ORACLE explicitly requested.");
                OracleDatabaseProvider oracle = new OracleDatabaseProvider();
                oracle.initialize();
                instance = oracle;
            } else if ("IN_MEMORY".equals(mode)) {
                System.out.println("[DB Initialization] Mode: IN_MEMORY explicitly requested.");
                InMemoryDatabaseProvider inMem = new InMemoryDatabaseProvider();
                inMem.initialize();
                instance = inMem;
            } else {
                // AUTO mode: Check Oracle first
                System.out.println("[DB Initialization] Mode: AUTO. Checking Oracle Database at " + AppConfig.ORACLE_URL + "...");
                OracleDatabaseProvider oracle = new OracleDatabaseProvider();
                oracle.initialize();
                if (oracle.isOracleActive()) {
                    System.out.println("[DB Initialization] Oracle Database detected! Operating in live Oracle JDBC mode.");
                    instance = oracle;
                } else {
                    System.out.println("--------------------------------------------------------------------------------");
                    System.out.println("[DB Notice] Local Oracle Database not reachable at " + AppConfig.ORACLE_URL);
                    System.out.println("[DB Notice] Activating in-process Relational SQL Provider (zero-dependency mode).");
                    System.out.println("[DB Notice] Complete Oracle SQL/PL/SQL scripts (01-12) are located in /database.");
                    System.out.println("--------------------------------------------------------------------------------");
                    InMemoryDatabaseProvider inMem = new InMemoryDatabaseProvider();
                    inMem.initialize();
                    instance = inMem;
                }
            }
        }
        return instance;
    }
}

