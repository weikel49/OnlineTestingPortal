package com.example.testing;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * Servlet that allows a teacher to create a new class and add a student roster.
 *
 * Generates unique usernames and random passwords for each student,
 * then stores them in the users table and links them to the new class.
 */

@WebServlet("/manage-class-roster")
public class ManageClassRosterServlet extends HttpServlet {

    /**
     * Represents the incoming request structure for class name and student names.
     */

    static class RosterRequest {
        String className;
        List<String> students;
    }

    /**
     * Represents the generated login credentials returned for each student.
     */

    static class StudentCredential {
        String fullName;
        String username;
        String password;
    }

    /**
     * Handles POST requests to create a new class and add students to it.
     *
     * The user must be logged in as a teacher. Each student is assigned a unique
     * username and random password. All students are linked to the newly created class.
     *
     * @param request  the HttpServletRequest containing the JSON roster input
     * @param response the HttpServletResponse used to return the generated student credentials
     * @throws IOException if reading input or writing output fails
     */

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(401);
            response.getWriter().write("❌ Not logged in");
            return;
        }

        Gson gson = new Gson();
        RosterRequest roster = gson.fromJson(request.getReader(), RosterRequest.class);
        int teacherId = (int) session.getAttribute("userId");
        List<StudentCredential> generated = new ArrayList<>();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(DBConfig.getUrl(), DBConfig.getUser(), DBConfig.getPassword())) {
                conn.setAutoCommit(false);

                // Insert class
                int classId;
                try (PreparedStatement classStmt = conn.prepareStatement(
                        "INSERT INTO classes (name, teacher_id) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    classStmt.setString(1, roster.className);
                    classStmt.setInt(2, teacherId);
                    classStmt.executeUpdate();
                    ResultSet rs = classStmt.getGeneratedKeys();
                    if (rs.next()) {
                        classId = rs.getInt(1);
                    } else {
                        throw new SQLException("❌ Failed to get generated class ID.");
                    }
                }

                for (String fullName : roster.students) {
                    String baseUsername = fullName.trim().toLowerCase().replaceAll("\\s+", "_");
                    String username = baseUsername;
                    int suffix = 1;

                    // Make sure username is unique
                    while (userExists(conn, username)) {
                        username = baseUsername + suffix++;
                    }

                    String password = generatePassword(8);

                    // Insert into users
                    try (PreparedStatement userStmt = conn.prepareStatement(
                            "INSERT INTO users (username, full_name, password, role) VALUES (?, ?, ?, 'student')",
                            Statement.RETURN_GENERATED_KEYS)) {
                        userStmt.setString(1, username);
                        userStmt.setString(2, fullName);
                        userStmt.setString(3, password);
                        userStmt.executeUpdate();
                        ResultSet userRs = userStmt.getGeneratedKeys();
                        if (!userRs.next()) continue;

                        int studentId = userRs.getInt(1);

                        // Link to class
                        try (PreparedStatement linkStmt = conn.prepareStatement(
                                "INSERT INTO class_students (class_id, student_id) VALUES (?, ?)")) {
                            linkStmt.setInt(1, classId);
                            linkStmt.setInt(2, studentId);
                            linkStmt.executeUpdate();
                        }

                        StudentCredential cred = new StudentCredential();
                        cred.fullName = fullName;
                        cred.username = username;
                        cred.password = password;
                        generated.add(cred);
                    }
                }

                conn.commit();
                response.setContentType("application/json");
                gson.toJson(Map.of("className", roster.className, "students", generated), response.getWriter());

            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(500);
                response.getWriter().write("❌ Server error while processing class roster");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ MySQL driver not found", e);
        }
    }

    /**
     * Checks whether a username already exists in the users table.
     *
     * @param conn     the database connection
     * @param username the username to check
     * @return true if the username exists, false otherwise
     * @throws SQLException if a database error occurs
     */

    private boolean userExists(Connection conn, String username) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    /**
     * Generates a random alphanumeric password of the given length.
     *
     * @param length the number of characters in the password
     * @return the generated password string
     */

    private String generatePassword(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random rand = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
