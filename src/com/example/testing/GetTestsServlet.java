package com.example.testing;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

/**
 * Servlet that retrieves all test summaries created by the currently logged-in teacher.
 *
 * This servlet responds to a GET request and returns a list of test IDs, titles,
 * and question counts for the teacher.
 */

@WebServlet("/get-tests")
public class GetTestsServlet extends HttpServlet {

    /**
     * Handles GET requests to return test summaries for the teacher.
     *
     * The user must be logged in with a valid session. The servlet fetches
     * test summaries from the TestDAO using the teacher's user ID.
     *
     * @param request  the HttpServletRequest containing the session
     * @param response the HttpServletResponse used to write the JSON result
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("❌ Not logged in.");
            return;
        }
        int teacherId = (int) session.getAttribute("userId");
        List<TestSummary> testSummaries = new ArrayList<>();

        try {
            testSummaries = TestDAO.getTestSummaries(teacherId);
        }catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("❌ Failed to fetch test summaries.");
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        new Gson().toJson(testSummaries, response.getWriter());
    }
}
