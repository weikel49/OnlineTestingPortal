package com.example.testing;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import com.google.gson.Gson;

/**
 * Servlet that retrieves all classes assigned to the currently logged-in teacher.
 *
 * This servlet responds to a GET request and returns a JSON list of class IDs and names.
 * It requires a valid teacher session.
 */

@WebServlet("/get-classes")
public class GetClassesServlet extends HttpServlet {

    /**
     * Represents a class entry returned to the client.
     */

    static class ClassInfo {
        int id;
        String name;
    }

    /**
     * Handles GET requests to fetch the list of classes for the logged-in teacher.
     *
     * The user must be logged in, and their ID must be present in the session.
     * The servlet queries the database for classes where the teacher_id matches the session userId.
     *
     * @param request  the HttpServletRequest containing the session
     * @param response the HttpServletResponse used to write the JSON or error
     * @throws IOException if an input or output error occurs
     */

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(401);
            response.getWriter().write("❌ Not logged in");
            return;
        }

        int teacherId = (int) session.getAttribute("userId");
        List<ClassInfo> classList = new ArrayList<>();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(
                DBConfig.getUrl(),
                DBConfig.getUser(),
                DBConfig.getPassword()
            )) {
                String sql = "SELECT id, name FROM classes WHERE teacher_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, teacherId);
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        ClassInfo cls = new ClassInfo();
                        cls.id = rs.getInt("id");
                        cls.name = rs.getString("name");
                        classList.add(cls);
                    }
                }
            }

            response.setContentType("application/json");
            new Gson().toJson(classList, response.getWriter());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("❌ Error fetching classes");
        }
    }
}
