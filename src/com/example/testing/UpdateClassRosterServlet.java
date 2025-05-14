package com.example.testing;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.*;
import java.util.List;

/**
 * Servlet that updates a class roster by either inserting new students or updating existing ones.
 *
 * Expects a JSON payload containing the class ID and a list of student records.
 * Automatically handles duplicate usernames and ensures consistency.
 */

@WebServlet("/update-class-roster")
public class UpdateClassRosterServlet extends HttpServlet {

    /**
     * Represents a student to be added or updated in the roster.
     */

    static class Student {
        public String fullName;
        public String username;
        public String password;
    }

    /**
     * Represents the structure of the roster update request.
     */

    static class RosterUpdate {
        public int classId;
        public List<Student> students;
    }

    /**
     * Handles POST requests to update a class roster.
     *
     * This includes deleting previous class-student links,
     * adding or updating users in the users table,
     * and establishing links in the class_students table.
     *
     * @param request  the HttpServletRequest containing roster JSON
     * @param response the HttpServletResponse used to return status messages
     * @throws IOException if there is an error reading the input or writing the output
     */

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("❌ Not logged in.");
            return;
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            BufferedReader reader = request.getReader();
            Gson gson = new Gson();
            RosterUpdate roster = gson.fromJson(reader, RosterUpdate.class);

            try (Connection conn = DriverManager.getConnection(
                    DBConfig.getUrl(),
                    DBConfig.getUser(),
                    DBConfig.getPassword()
            )) {
                conn.setAutoCommit(false);

                // Clear existing links
                try (PreparedStatement deleteStmt = conn.prepareStatement(
                        "DELETE FROM class_students WHERE class_id = ?")) {
                    deleteStmt.setInt(1, roster.classId);
                    deleteStmt.executeUpdate();
                }

                // Prepare reusable statements
                try (
                    PreparedStatement selectUser = conn.prepareStatement("SELECT id, password, full_name FROM users WHERE username = ?");
                    PreparedStatement updateUser = conn.prepareStatement("UPDATE users SET password = ?, full_name = ? WHERE id = ?");
                    PreparedStatement insertUser = conn.prepareStatement(
                        "INSERT INTO users (username, full_name, password, role) VALUES (?, ?, ?, 'student')",
                        Statement.RETURN_GENERATED_KEYS);
                    PreparedStatement insertLink = conn.prepareStatement(
                        "INSERT INTO class_students (class_id, student_id) VALUES (?, ?)")
                ) {
                    for (Student s : roster.students) {
                        // Auto-generate username if missing

                        String username;
                        if (s.username != null && !s.username.isBlank() && userExists(conn, s.username)) {
                            // Use provided username if it already exists
                            username = s.username.trim().toLowerCase();
                        } else {
                            // Generate a username from full name if blank or if no user found
                            String baseUsername = s.fullName.trim().toLowerCase().replaceAll("\\s+", "_");
                            username = baseUsername;
                            int suffix = 1;
                            while (userExists(conn, username)) {
                                username = baseUsername + suffix++;
                            }
                        }

                        int userId = -1;

                        // Check if this exact username now exists
                        selectUser.setString(1, username);
                        ResultSet rs = selectUser.executeQuery();
                        if (rs.next()) {
                            userId = rs.getInt("id");
                            String currentPassword = rs.getString("password");
                            String currentName = rs.getString("full_name");

                            if (!currentPassword.equals(s.password) || !currentName.equals(s.fullName)) {
                                updateUser.setString(1, s.password);
                                updateUser.setString(2, s.fullName);
                                updateUser.setInt(3, userId);
                                updateUser.executeUpdate();
                            }
                        } else {
                            // Insert new student
                            String password = (s.password != null && !s.password.isBlank())
                                    ? s.password
                                    : generatePassword(8);

                            insertUser.setString(1, username);
                            insertUser.setString(2, s.fullName);
                            insertUser.setString(3, password);
                            insertUser.executeUpdate();
                            ResultSet keys = insertUser.getGeneratedKeys();
                            if (keys.next()) {
                                userId = keys.getInt(1);
                                System.out.println("✔ userId resolved: " + userId);
                            }
                        }

                        if (userId != -1) {
                            insertLink.setInt(1, roster.classId);
                            insertLink.setInt(2, userId);
                            insertLink.addBatch();
                        }
                    }
                    insertLink.executeBatch();
                }

                conn.commit();
                response.setStatus(200);
                response.getWriter().write("✅ Roster updated.");
            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(500);
                response.getWriter().write("❌ Error updating roster.");
            }

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ MySQL driver not found", e);
        }
    }

    private boolean userExists(Connection conn, String username) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    private String generatePassword(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return password.toString();
    }
}
