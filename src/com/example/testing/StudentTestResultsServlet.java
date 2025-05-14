package com.example.testing;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import com.google.gson.Gson;

/**
 * Servlet that retrieves the test results for a student for a specific test.
 *
 * It returns a JSON response including the student's score, total number of questions,
 * and a breakdown of each question with their answer and the correct answer.
 */

@WebServlet("/student-test-results")
public class StudentTestResultsServlet extends HttpServlet {

    /**
     * Represents details for a single answered question.
     */

    static class ResultDetail {
        String question;
        String yourAnswer;
        String correctAnswer;
        boolean correct;
    }

    /**
     * Represents the entire result for a student, including score and answer details.
     */

    static class StudentResult {
        int score;
        int total;
        List<ResultDetail> answers;
    }

    /**
     * Handles GET requests to retrieve results for a specific test.
     *
     * The student must be logged in. The servlet returns question text, selected and correct answers,
     * along with a calculated score and total number of questions.
     *
     * @param request  the HttpServletRequest containing the testId parameter
     * @param response the HttpServletResponse where the JSON result is written
     * @throws IOException if reading input or writing output fails
     */

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(401);
            response.getWriter().write("❌ Not logged in.");
            return;
        }

        String testIdParam = request.getParameter("testId");
        int testId = Integer.parseInt(testIdParam);
        String username = (String) session.getAttribute("username");
        StudentResult result = new StudentResult();
        result.answers = new ArrayList<>();
        result.score = 0;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(
                DBConfig.getUrl(),
                DBConfig.getUser(),
                DBConfig.getPassword()
            );) {

                // Get student ID
                int studentId = -1;
                try (PreparedStatement userStmt = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {
                    userStmt.setString(1, username);
                    ResultSet userRs = userStmt.executeQuery();
                    if (userRs.next()) {
                        studentId = userRs.getInt("id");
                    } else {
                        response.setStatus(404);
                        response.getWriter().write("❌ Student not found.");
                        return;
                    }
                }

                // Get question and result details
                String questionSql = """
                    SELECT q.question_text, r.selected_answer, r.correct_answer
                    FROM results r
                    JOIN questions q ON r.question_id = q.id
                    WHERE r.test_id = ? AND r.student_id = ?
                """;

                try (PreparedStatement stmt = conn.prepareStatement(questionSql)) {
                    stmt.setInt(1, testId);
                    stmt.setInt(2, studentId);
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        ResultDetail detail = new ResultDetail();
                        detail.question = rs.getString("question_text");
                        detail.yourAnswer = rs.getString("selected_answer");
                        detail.correctAnswer = rs.getString("correct_answer");
                        detail.correct = detail.yourAnswer.equals(detail.correctAnswer);
                        if (detail.correct) result.score++;
                        result.answers.add(detail);
                    }

                    result.total = result.answers.size();
                }

                response.setContentType("application/json");
                new Gson().toJson(result, response.getWriter());

            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(500);
            }
        }catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ MySQL driver not found", e);
        }
        
    }
}
