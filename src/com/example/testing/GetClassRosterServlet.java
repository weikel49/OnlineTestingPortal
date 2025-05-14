package com.example.testing;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.*;
import java.util.*;

import com.google.gson.Gson;

/**
 * Servlet that returns the student roster for a specific class.
 *
 * This servlet verifies the logged-in teacher's ownership of the class before
 * returning the class name and a list of enrolled students.
 */

@WebServlet("/get-class-roster")
public class GetClassRosterServlet extends HttpServlet {

    /**
     * Represents a student in the class roster.
     */

    static class Student {
        String fullName;
        String username;
        String password;
    }

    /**
     * Represents the JSON response structure containing class name and students.
     */

    static class ClassRosterResponse {
        String className;
        List<Student> students;
    }

    /**
     * Handles GET requests to fetch the roster for a specific class.
     *
     * The user must be a logged-in teacher. The servlet checks that the teacher owns the class
     * before returning the class name and list of students.
     *
     * @param request  the HttpServletRequest containing classId parameter
     * @param response the HttpServletResponse used to return the JSON or error status
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
        int classId = Integer.parseInt(request.getParameter("classId"));

        ClassRosterResponse classData = new ClassRosterResponse();
        classData.students = new ArrayList<>();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(DBConfig.getUrl(), DBConfig.getUser(), DBConfig.getPassword())) {

                // Check if teacher owns the class
                try (PreparedStatement classCheck = conn.prepareStatement("SELECT name FROM classes WHERE id = ? AND teacher_id = ?")) {
                    classCheck.setInt(1, classId);
                    classCheck.setInt(2, teacherId);
                    ResultSet rs = classCheck.executeQuery();

                    if (!rs.next()) {
                        response.setStatus(403);
                        response.getWriter().write("❌ You do not have access to this class.");
                        return;
                    }

                    classData.className = rs.getString("name");
                }

                // Fetch students (separate try to isolate error handling)
                try (PreparedStatement stmt = conn.prepareStatement("""
                    SELECT u.username, u.password, u.full_name
                    FROM class_students cs
                    JOIN users u ON cs.student_id = u.id
                    WHERE cs.class_id = ?
                """)) {
                    stmt.setInt(1, classId);
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        Student s = new Student();
                        s.fullName = rs.getString("full_name");
                        s.username = rs.getString("username");
                        s.password = rs.getString("password");
                        classData.students.add(s);
                    }
                }

                response.setContentType("application/json");
                new Gson().toJson(classData, response.getWriter());

            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("❌ Server error loading class roster");
            return;
        } catch (ClassNotFoundException e) {
            response.setStatus(500);
            response.getWriter().write("❌ Server error loading class roster");
            e.printStackTrace();
            return;
        }
    }
}
