package com.findit;

import com.findit.config.AppConfig;
import com.findit.controllers.ApiController;
import com.findit.controllers.StaticFileHandler;
import com.findit.database.DatabaseFactory;
import com.findit.database.DatabaseManager;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Application {
    public static void main(String[] args) {
        try {
            System.out.println("================================================================================");
            System.out.println("               FindIt - Campus Lost & Found Management System                   ");
            System.out.println("                   DBMS Microproject - Academic Edition                         ");
            System.out.println("================================================================================");

            // Initialize Database Layer
            DatabaseManager db = DatabaseFactory.getInstance();
            System.out.println("[FindIt DB Status] " + db.getDatabaseStatusDescription());

            // Initialize HTTP Server
            int port = AppConfig.PORT;
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

            // Register Handlers
            server.createContext("/api", new ApiController());
            server.createContext("/", new StaticFileHandler());

            server.start();

            System.out.println("--------------------------------------------------------------------------------");
            System.out.println(" FindIt Web Server started successfully!");
            System.out.println(" Web Application URL : http://localhost:" + port + "/");
            System.out.println(" REST API Base URL   : http://localhost:" + port + "/api/");
            System.out.println(" Database Engine     : " + (db.isOracleActive() ? "Oracle Database via JDBC" : "In-Process Relational SQL Provider"));
            System.out.println(" Oracle SQL Scripts  : /database (01 to 12 SQL/PL/SQL scripts)");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println(" Default Test Accounts:");
            System.out.println("   [Admin]   Email: admin@campus.edu         Password: Admin@123");
            System.out.println("   [Student] Email: aarav.sharma@campus.edu   Password: Student@123");
            System.out.println("   [Student] Email: priya.patel@campus.edu    Password: Student@123");
            System.out.println("================================================================================");

        } catch (Exception e) {
            System.err.println("Failed to start FindIt Application Server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

