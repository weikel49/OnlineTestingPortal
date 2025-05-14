package com.example.testing;

import java.io.InputStream;
import java.util.Properties;

/**
 * Provides static access to database configuration values.
 *
 * This class loads the MySQL JDBC driver and reads connection settings from
 * a `db.properties` file located in the classpath.
 *
 * The expected keys in the properties file are:
 *   db.url – JDBC connection URL
 *   db.user – database username
 *   db.password – database password
 *
 * If the file or driver is missing, the class throws a runtime exception at startup.
 */

public class DBConfig {
    private static Properties props = new Properties();

    static {
        try {
            // Register MySQL driver manually
            Class.forName("com.mysql.cj.jdbc.Driver");
    
            // Load db.properties
            try (InputStream input = DBConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (input == null) {
                    throw new RuntimeException("❌ db.properties not found in classpath");
                }
                props.load(input);
            }
    
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ MySQL driver not found", e);
        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to load DB configuration", e);
        }
    }
    
     /**
     * @return the database URL from db.url property
     */
    public static String getUrl() {
        return props.getProperty("db.url");
    }

    /**
     * @return the database user from db.user property
     */
    public static String getUser() {
        return props.getProperty("db.user");
    }

    /**
     * @return the database password from db.password property
     */
    public static String getPassword() {
        return props.getProperty("db.password");
    }
}
