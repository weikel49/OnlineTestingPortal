package com.example.testing;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

/**
 * Servlet that returns the full structure of a test for viewing or editing.
 *
 * This includes the test's title, duration, all associated questions,
 * and each question's list of answer options.
 */

@WebServlet("/view-test")
public class ViewTestServlet extends HttpServlet {

    /**
     * Handles GET requests to retrieve the full details of a test by ID.
     *
     * The user must be logged in. Responds with a JSON Test object that includes
     * the test title, duration, and each question with its options and correct answer.
     *
     * @param request  the HttpServletRequest containing the test ID
     * @param response the HttpServletResponse used to write the JSON test
     * @throws IOException if an I/O error occurs
     */

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("❌ Not logged in.");
            return;
        }
        int testId = Integer.parseInt(request.getParameter("id"));
        Test test = new Test();
        System.out.println("🚩 ViewTestServlet reached for testId: " + testId);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            try (Connection conn = DriverManager.getConnection(
                DBConfig.getUrl(),
                DBConfig.getUser(),
                DBConfig.getPassword()
            );) {

                // Get test title
                String testSql = "SELECT title , duration_minutes FROM tests WHERE id = ?";
                try (PreparedStatement testStmt = conn.prepareStatement(testSql)) {
                    testStmt.setInt(1, testId);
                    ResultSet testRs = testStmt.executeQuery();

                    if (testRs.next()) {
                        test.setId(testId);
                        test.setTitle(testRs.getString("title"));
                        test.setDurationMinutes(testRs.getInt("duration_minutes"));
                    } else {
                        response.setStatus(404);
                        response.getWriter().write("❌ Test not found.");
                        return;
                    }
                }

                // Get questions
                String qSql = "SELECT id, question_text, correct_answer FROM questions WHERE test_id = ?";
                List<Question> questions = new ArrayList<>();

                try (PreparedStatement qStmt = conn.prepareStatement(qSql)) {
                    qStmt.setInt(1, testId);
                    ResultSet qRs = qStmt.executeQuery();

                    while (qRs.next()) {
                        int questionId = qRs.getInt("id");
                        String questionText = qRs.getString("question_text");
                        String correctAnswer = qRs.getString("correct_answer");

                        // Fetch options for this question
                        String optSql = "SELECT option_text FROM options WHERE question_id = ?";
                        try (PreparedStatement optStmt = conn.prepareStatement(optSql)) {
                            optStmt.setInt(1, questionId);
                            ResultSet optRs = optStmt.executeQuery();

                            List<String> optionList = new ArrayList<>();
                            while (optRs.next()) {
                                optionList.add(optRs.getString("option_text"));
                            }

                            Collections.shuffle(optionList);
                            Map<String, String> shuffledOptions = new LinkedHashMap<>();
                            int count = 1;

                            for (String opt : optionList) {
                                String key = "option" + count++;
                                shuffledOptions.put(key, opt);
                            }

                            Question question = new Question();
                            question.setId(questionId);
                            question.setQuestionText(questionText);
                            question.setOptions(shuffledOptions);
                            question.setCorrectAnswer(correctAnswer);

                            questions.add(question);
                        }
                    }
                }
                
                Collections.shuffle(questions);
                test.setQuestions(questions);

            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(500);
                response.getWriter().write("❌ Error retrieving test.");
                return;
            }
        }catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ MySQL driver not found", e);
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        new Gson().toJson(test, response.getWriter());
    }
}
