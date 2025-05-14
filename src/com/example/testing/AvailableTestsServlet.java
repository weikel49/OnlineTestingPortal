package com.example.testing;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Servlet responsible for fetching available tests for a logged-in student.
 *
 * This servlet returns a list of tests assigned to the student’s class that they
 * have not yet taken. It verifies the user session and filters results based on the
 * student's ID and completed test records.
 */

@WebServlet("/available-tests")
public class AvailableTestsServlet extends HttpServlet {

    /**
     * Handles GET requests to fetch a list of tests available to the currently logged-in student.
     *
     * The servlet checks if a session exists, retrieves the student's ID, and
     * queries the database for all tests assigned to that student’s class that the
     * student hasn’t already completed.
     *
     * @param request  the {@link HttpServletRequest} containing client request information
     * @param response the {@link HttpServletResponse} used to return the JSON result or an error
     * @throws IOException if an I/O error occurs while writing the response
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("❌ Not logged in.");
            return;
        }
        List<TestSummary> availableTests = new ArrayList<>();

        int studentId = (int) session.getAttribute("userId");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(
                DBConfig.getUrl(),
                DBConfig.getUser(),
                DBConfig.getPassword()
            );
            ) {
                String sql = """
                    SELECT t.id, t.title
                    FROM tests t
                    JOIN classes c ON t.created_by = c.teacher_id
                    JOIN class_students cs ON cs.class_id = c.id
                    WHERE cs.student_id = ?
                        AND t.id NOT IN (
                            SELECT DISTINCT test_id FROM results WHERE student_id = ?
                        )
                """;

                try(PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, studentId);
                    stmt.setInt(2, studentId);
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        availableTests.add(new TestSummary(
                            rs.getInt("id"),
                            rs.getString("title"),
                            0
                        ));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("🔥 Servlet caught exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("❌ Failed to fetch available tests.");
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        new Gson().toJson(availableTests, response.getWriter());
    }
}
