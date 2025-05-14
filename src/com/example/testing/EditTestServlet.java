package com.example.testing;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

/**
 * Servlet that allows teachers to view and update an existing test.
 *
 * Supports both GET and POST requests:
 * 
 * GET: Retrieves a test and its questions/options for editing.
 * POST: Updates the test’s title, questions, and options in the database.
 * 
 */

@WebServlet("/edit-test")
public class EditTestServlet extends HttpServlet {

    /**
     * Handles GET requests to load test details for editing.
     *
     * Fetches test title, duration, questions, and associated options for a given test ID.
     * Requires a valid session with a logged-in user.
     *
     * @param request  the {@link HttpServletRequest} containing the test ID
     * @param response the {@link HttpServletResponse} where the test JSON is written
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

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(
                DBConfig.getUrl(),
                DBConfig.getUser(),
                DBConfig.getPassword()
            );
            ) {
                String titleSql = "SELECT title, duration_minutes FROM tests WHERE id = ?";
                PreparedStatement titleStmt = conn.prepareStatement(titleSql);
                titleStmt.setInt(1, testId);
                ResultSet Rs = titleStmt.executeQuery();
    
                if (Rs.next()) {
                    test.setId(testId);
                    test.setTitle(Rs.getString("title"));
                    test.setDurationMinutes(Rs.getInt("duration_minutes"));
                    List<Question> questions = new ArrayList<>();
    
                    String qSql = "SELECT id, question_text, correct_answer FROM questions WHERE test_id = ?";
                    PreparedStatement qStmt = conn.prepareStatement(qSql);
                    qStmt.setInt(1, testId);
                    ResultSet qRs = qStmt.executeQuery();
    
                    while (qRs.next()) {
                        int questionId = qRs.getInt("id");
                        String questionText = qRs.getString("question_text");
                        String correctAnswer = qRs.getString("correct_answer");
    
                        // Get options for this question
                        String optSql = "SELECT option_text FROM options WHERE question_id = ?";
                        PreparedStatement optStmt = conn.prepareStatement(optSql);
                        optStmt.setInt(1, questionId);
                        ResultSet optRs = optStmt.executeQuery();
    
                        List<String> options = new ArrayList<>();
                        while (optRs.next()) {
                            options.add(optRs.getString("option_text"));
                        }
    
                        Map<String, String> optionMap = new LinkedHashMap<>();
                        int count = 1;
                        for (String opt : options) {
                            optionMap.put("option" + count++, opt);
                        }

                        Question question = new Question();
                        question.setId(questionId);
                        question.setQuestionText(questionText);
                        question.setOptions(optionMap);
                        question.setCorrectAnswer(correctAnswer);
                        questions.add(question);

                    }
    
                    test.setQuestions(questions);
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(500);
                response.getWriter().write("❌ Failed to load test");
            }
        }catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ MySQL driver not found", e);
        }

        response.setContentType("application/json");
        new Gson().toJson(test, response.getWriter());
    }

    /**
     * Handles POST requests to update a test's title, questions, and options.
     *
     * Deletes all previous questions/options, then inserts the updated test content.
     * Commits changes as a transaction.
     *
     * @param request  the {@link HttpServletRequest} containing the updated test JSON
     * @param response the {@link HttpServletResponse} that returns success or failure
     * @throws IOException if an I/O error occurs
     */

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("❌ Not logged in.");
            return;
        }
        Test test = new Gson().fromJson(request.getReader(), Test.class);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(
                DBConfig.getUrl(),
                DBConfig.getUser(),
                DBConfig.getPassword()
            );) {
                conn.setAutoCommit(false);
    
                // Update test title
                String updateTitle = "UPDATE tests SET title = ? WHERE id = ?";
                try (PreparedStatement titleStmt = conn.prepareStatement(updateTitle)) {
                    titleStmt.setString(1, test.getTitle());
                    titleStmt.setInt(2, test.getId());
                    titleStmt.executeUpdate();
                }
                
                try (PreparedStatement delO =conn.prepareStatement("DELETE FROM options WHERE question_id IN (SELECT id FROM questions WHERE test_id = ?)")) {
                    delO.setInt(1, test.getId());
                    delO.executeUpdate();
                }
                
                try (PreparedStatement delQ = conn.prepareStatement("DELETE FROM questions WHERE test_id = ?")) {
                    delQ.setInt(1, test.getId());
                    delQ.executeUpdate();
                }
    
                // Insert new questions and options
                String insertQ = "INSERT INTO questions (test_id, question_text, correct_answer) VALUES (?, ?, ?)";
                String insertOpt = "INSERT INTO options (question_id, option_text) VALUES (?, ?)";
    
                try (PreparedStatement qStmt = conn.prepareStatement(insertQ);
                     PreparedStatement optStmt = conn.prepareStatement(insertOpt)) {
    
                    for (Question q : test.getQuestions()) {
                        qStmt.setInt(1, test.getId());
                        qStmt.setString(2, q.getQuestionText());
                        qStmt.setString(3, q.getCorrectAnswer());
                        qStmt.executeUpdate();
    
                        // Get new question ID
                        ResultSet rs = conn.createStatement().executeQuery("SELECT LAST_INSERT_ID()");
                        int questionId = rs.next() ? rs.getInt(1) : -1;
    
                        for (String opt : q.getOptions().values()) {
                            optStmt.setInt(1, questionId);
                            optStmt.setString(2, opt);
                            optStmt.addBatch();
                        }
                    }
                    optStmt.executeBatch();
                }
    
                conn.commit();
                response.setStatus(200);
                
            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(500);
                response.getWriter().write("❌ Failed to update test.");
            }
        }catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ MySQL driver not found", e);
        }
    }
}
