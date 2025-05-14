package com.example.testing;

import java.sql.*;

/**
 * Utility class to initialize the application database.
 *
 * This class checks whether a teacher user exists in the `users` table and inserts
 * a default teacher account if none is found. It is typically run manually to prepare
 * the database during setup or development.
 */

public class DatabaseInitializer {

    /**
     * Connects to the database and inserts a default teacher user if none exists.
     *
     * It connects using the {@link DBConfig} settings and executes SQL statements
     * to query and optionally update the `users` table.
     */

    public static void main() {
        try (Connection conn = DriverManager.getConnection(
                DBConfig.getUrl(),
                DBConfig.getUser(),
                DBConfig.getPassword());
             Statement stmt = conn.createStatement()) {

            System.out.println("✅ Connected to MySQL database.");

            // Check if any teacher exits
            String checkSql = "SELECT COUNT(*) FROM users WHERE role = 'teacher'";
            ResultSet rs = stmt.executeQuery(checkSql);
            rs.next();
            
            if (rs.getInt(1) == 0) {
                String insertSql = "INSERT INTO users (username, password, role) VALUES ('teacher1', 'test123', 'teacher')";
                stmt.executeUpdate(insertSql);
                System.out.println("✅ Default teacher inserted.");
            } else {
                System.out.println("ℹ️ Teacher already exists.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
