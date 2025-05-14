package com.example.testing;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.*;
import java.util.*;

import com.google.gson.Gson;

/**
 * Servlet that generates a summary of student test results for a given test.
 *
 * Returns a JSON list of students with their score and the total number of questions.
 * Requires the user to be logged in.
 */

@WebServlet("/test-results-summary")
public class TestResultsSummaryServlet extends HttpServlet {

    /**
     * Handles GET requests to generate a results summary for a test.
     *
     * Requires a valid session. Takes the test ID as a query parameter.
     * For each student who took the test, it returns their username,
     * number of correct answers, and total number of questions.
     *
     * @param request  the HttpServletRequest containing the test ID
     * @param response the HttpServletResponse used to write the JSON result
     * @throws IOException if an I/O error occurs during response writing
     */

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("❌ Not logged in.");
            return;
        }
        String testIdParam = request.getParameter("id");
        List<Map<String, Object>> summaryList = new ArrayList<>();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try {
                int testId = Integer.parseInt(testIdParam);

                try (Connection conn = DriverManager.getConnection(
                    DBConfig.getUrl(),
                    DBConfig.getUser(),
                    DBConfig.getPassword()
                );) {
                    String sql = """
                        SELECT u.username AS student,
                            SUM(CASE WHEN r.selected_answer = r.correct_answer THEN 1 ELSE 0 END) AS score,
                            COUNT(*) AS total
                        FROM results r
                        JOIN users u ON r.student_id = u.id
                        WHERE r.test_id = ?
                        GROUP BY u.username
                    """;

                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, testId);
                        ResultSet rs = stmt.executeQuery();

                        while (rs.next()) {
                            Map<String, Object> row = new HashMap<>();
                            row.put("student", rs.getString("student"));
                            row.put("score", rs.getInt("score"));
                            row.put("total", rs.getInt("total"));
                            summaryList.add(row);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(500);
                response.getWriter().write("❌ Error retrieving results summary.");
                return;
            }
        }catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ MySQL driver not found", e);
        }
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        new Gson().toJson(summaryList, response.getWriter());
    }
}
