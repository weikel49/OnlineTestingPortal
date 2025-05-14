package com.example.testing;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import com.google.gson.*;

/**
 * Servlet that handles the submission of student answers for a test.
 *
 * Accepts a JSON payload containing a test ID and a list of answers.
 * Verifies that the user is logged in, then records the answers in the database.
 */

@WebServlet("/submit-answers")
public class SubmitAnswersServlet extends HttpServlet {

    /**
     * Represents a single submitted answer from the student.
     */

    private static class Answer {
        int questionId;
        String selectedAnswer;
        String correctAnswer;
    }

    /**
     * Represents the full submission, including test ID and a list of answers.
     */

    private static class Submission {
        int testId;
        List<Answer> answers;
    }

    /**
     * Handles POST requests to submit a student's test answers.
     *
     * Requires a valid session. Saves all answers into the results table
     * in a single batch and commits the transaction. Retries on database lock.
     *
     * @param request  the HttpServletRequest containing the JSON submission
     * @param response the HttpServletResponse used to send status messages
     * @throws IOException if reading or writing fails
     */

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("📥 SubmitAnswersServlet called");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(401);
            response.getWriter().write("❌ Not logged in.");
            return;
        }

        Gson gson = new Gson();
        Submission submission = gson.fromJson(request.getReader(), Submission.class);
        System.out.println("➡ Received testId: " + submission.testId);
        System.out.println("➡ Received " + submission.answers.size() + " answers");

        synchronized (SubmitAnswersServlet.class) {
            try {
                String username = (String) session.getAttribute("username");
                int studentId;

                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");

                    try (Connection conn = DriverManager.getConnection(
                        DBConfig.getUrl(),
                        DBConfig.getUser(),
                        DBConfig.getPassword()
                    )) {
                        conn.setAutoCommit(false);

                        // Get student ID
                        try (PreparedStatement userStmt = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {
                            userStmt.setString(1, username);
                            ResultSet rs = userStmt.executeQuery();
                            if (rs.next()) {
                                studentId = rs.getInt("id");
                                System.out.println("👤 studentId resolved: " + studentId);
                            } else {
                                response.setStatus(404);
                                response.getWriter().write("❌ Student not found.");
                                return;
                            }
                        }

                        // Insert each answer into results table
                        String insertSql = """
                            INSERT INTO results (test_id, student_id, question_id, selected_answer, correct_answer)
                            VALUES (?, ?, ?, ?, ?)
                        """;

                        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                            for (Answer a : submission.answers) {
                                System.out.println("🔄 Inserting answer: qid=" + a.questionId + ", selected=" + a.selectedAnswer + ", correct=" + a.correctAnswer);
                                stmt.setInt(1, submission.testId);
                                stmt.setInt(2, studentId);
                                stmt.setInt(3, a.questionId);
                                stmt.setString(4, a.selectedAnswer);
                                stmt.setString(5, a.correctAnswer);
                                stmt.addBatch();
                            }
                            stmt.executeBatch();
                            System.out.println("📦 Batch insert executed");
                        }

                        // Commit with retry
                        int retries = 5;
                        while (retries-- > 0) {
                            try {
                                conn.commit();
                                System.out.println("✅ Commit complete for testId=" + submission.testId + ", studentId=" + studentId);
                                break;
                            } catch (SQLException e) {
                                if (e.getMessage().contains("database is locked")) {
                                    System.out.println("⏳ Commit retry...");
                                    Thread.sleep(100);
                                } else {
                                    throw e;
                                }
                            }
                        }

                        response.setContentType("text/plain");
                        response.getWriter().write("✅ Test submitted successfully.");
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("❌ MySQL driver not found", e);
                }
            } catch (Exception e) {
                System.out.println("❌ Error in SubmitAnswersServlet: " + e.getMessage());
                e.printStackTrace();
                response.setStatus(500);
                response.getWriter().write("❌ Server error while submitting test.");
            }
        }
    }
}
