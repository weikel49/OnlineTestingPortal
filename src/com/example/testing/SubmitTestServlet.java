package com.example.testing;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;

import com.google.gson.Gson;

/**
 * Servlet that handles submission of new tests by teachers.
 *
 * Accepts a JSON payload representing the test, including questions and options.
 * Converts correct answer keys into answer text and saves the test using TestDAO.
 */

@WebServlet("/submit-test")
public class SubmitTestServlet extends HttpServlet {
    public TestDAO testDAO = new TestDAO();

    /**
     * Handles POST requests to create and save a new test.
     *
     * Requires a logged-in teacher. Reads a Test object from JSON, injects the
     * teacher ID, converts answer keys to actual answer text, and stores the test.
     *
     * @param request  the HttpServletRequest containing the test JSON
     * @param response the HttpServletResponse used to send success or error status
     * @throws IOException if reading input or writing output fails
     */

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("❌ User not logged in");
            return;
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            BufferedReader reader = request.getReader();
            Gson gson = new Gson();
            Test test = gson.fromJson(reader, Test.class);

            for (Question q : test.getQuestions()) {
                String key = q.getCorrectAnswer();          // e.g., "option2"
                if (q.getOptions() != null && key != null) {
                    String actualText = q.getOptions().get(key);  // "44"
                    if (actualText != null) {
                        q.setCorrectAnswer(actualText);     // replace "option2" with "44"
                    }
                }
            }            
            
            int createdBy = (int) session.getAttribute("userId");
            test.setCreatedBy(createdBy); // <- inject into the test object

            System.out.println("📥 Received test submission: " + test.getTitle());

            try {
                boolean saved = testDAO.saveTest(test);
                if (saved) {
                    System.out.println("✅ Test saved successfully");
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    System.out.println("❌ Failed to save test");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            } catch (SQLException | InterruptedException e) {
                System.out.println("❌ Error while saving test: " + e.getMessage());
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("❌ Server error while saving test.");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ MySQL driver not found", e);
        }
    }
}
