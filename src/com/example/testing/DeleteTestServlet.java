package com.example.testing;

import java.io.IOException;
import java.sql.*;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

/**
 * Servlet that handles deletion of a test from the system.
 *
 * This servlet receives a DELETE request with a `testId` parameter and removes
 * the associated record from the `tests` table. It performs a safe transaction and
 * retries commit attempts if the database is locked.
 */

@WebServlet("/delete-test")
public class DeleteTestServlet extends HttpServlet {

        /**
     * Handles HTTP DELETE requests to remove a test by ID.
     *
     * @param request  the {@link HttpServletRequest} containing the test ID as a parameter
     * @param response the {@link HttpServletResponse} used to return status information
     * @throws IOException if an I/O error occurs while writing the response
     */

    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("❌ Not logged in.");
            return;
        }

        int testId = Integer.parseInt(request.getParameter("id"));

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(
                DBConfig.getUrl(),
                DBConfig.getUser(),
                DBConfig.getPassword()
            );) {
                conn.setAutoCommit(false);
    
                String deleteTest = "DELETE FROM tests WHERE id = ?";
                try (PreparedStatement tStmt = conn.prepareStatement(deleteTest)) {
                    tStmt.setInt(1, testId);
                    tStmt.executeUpdate();
                }
    
                // Retry-safe commit
                boolean committed = false;
                int retries = 5;
                while (retries-- > 0 && !committed) {
                    try {
                        conn.commit();
                        committed = true;
                        System.out.println("✅ Deleted test and related data for testId=" + testId);
                        break;
                    } catch (SQLException e) {
                        if (e.getMessage().contains("database is locked")) {
                            System.out.println("⏳ Retry commit...");
                            Thread.sleep(100);
                        } else {
                            throw e;
                        }
                    }
                }
    
                if (!committed) {
                    response.setStatus(500);
                    response.getWriter().write("❌ Error deleting test.");
                    return;
                }

                response.setStatus(200);
    
            } catch (Exception e) {
                System.out.println("❌ Failed to delete test: " + e.getMessage());
                e.printStackTrace();
                response.setStatus(500);
            }
        }catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ MySQL driver not found", e);
        }
    }
}
