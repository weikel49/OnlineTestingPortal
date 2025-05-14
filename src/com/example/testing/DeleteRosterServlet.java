package com.example.testing;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.*;


/**
 * Servlet that deletes a class and its associated student roster.
 *
 * Receives a DELETE request with a `classId` parameter. If authorized, it
 * deletes the class entry from the `classes` table. If cascading deletes are
 * configured, associated students are also removed from the `class_students` table.
 *
 * Responds with appropriate status codes for success, authorization failure,
 * bad input, or server error.
 */

@WebServlet("/delete-roster")
public class DeleteRosterServlet extends HttpServlet {

    /**
     * Handles HTTP DELETE requests to remove a class roster by ID.
     *
     * @param request  the {@link HttpServletRequest} with the `classId` parameter
     * @param response the {@link HttpServletResponse} used to send success or error messages
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
        int classId;

        try {
            classId = Integer.parseInt(request.getParameter("classId"));
        } catch (NumberFormatException e) {
            response.setStatus(400);
            response.getWriter().write("❌ Invalid class ID.");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(
                DBConfig.getUrl(), 
                DBConfig.getUser(), 
                DBConfig.getPassword())) {

                conn.setAutoCommit(false);

                // Delete the class (cascade deletes students if set up)
                try (PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM classes WHERE id = ?")) {
                    stmt.setInt(1, classId);
                    int affected = stmt.executeUpdate();
                    if (affected == 0) {
                        response.setStatus(404);
                        response.getWriter().write("❌ Class not found.");
                        return;
                    }
                }

                conn.commit();
                response.setStatus(200);
                response.getWriter().write("✅ Class deleted.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("❌ Error deleting class.");
        }
    }
}
