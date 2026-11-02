package com.findit.config;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    public static int PORT = 8080;
    public static String ORACLE_URL = "jdbc:oracle:thin:@localhost:1521/XEPDB1";
    public static String ORACLE_USER = "findit_user";
    public static String ORACLE_PASSWORD = "findit_password";
    public static String DB_MODE = "AUTO"; // AUTO, ORACLE, IN_MEMORY
    public static String STATIC_DIR = "frontend";

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                if (props.getProperty("server.port") != null) {
                    PORT = Integer.parseInt(props.getProperty("server.port"));
                }
                if (props.getProperty("oracle.url") != null) {
                    ORACLE_URL = props.getProperty("oracle.url");
                }
                if (props.getProperty("oracle.user") != null) {
                    ORACLE_USER = props.getProperty("oracle.user");
                }
                if (props.getProperty("oracle.password") != null) {
                    ORACLE_PASSWORD = props.getProperty("oracle.password");
                }
                if (props.getProperty("db.mode") != null) {
                    DB_MODE = props.getProperty("db.mode");
                }
                if (props.getProperty("static.dir") != null) {
                    STATIC_DIR = props.getProperty("static.dir");
                }
            }
        } catch (Exception ignored) {
        }
    }
}

