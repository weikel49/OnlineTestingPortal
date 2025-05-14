package com.example.testing;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.*;

/**
 * Servlet that handles user login for the online testing portal.
 *
 * Accepts POST requests with username and password. If the credentials are valid,
 * it initializes a user session and redirects based on the user's role.
 */

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    /**
     * Handles POST requests for user authentication.
     *
     * Verifies username and password against the database. If successful,
     * sets session attributes and redirects the user based on role. If not,
     * returns an unauthorized status or error if a database issue occurs.
     *
     * @param request  the HttpServletRequest containing login form data
     * @param response the HttpServletResponse used to return status or redirect
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        System.out.println("🔑 Username: " + username);
        System.out.println("🔒 Password: " + password);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(
                DBConfig.getUrl(),
                DBConfig.getUser(),
                DBConfig.getPassword()
            );
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {
    
                stmt.setString(1, username);
                stmt.setString(2, password);
    
                ResultSet rs = stmt.executeQuery();
    
                if (rs.next()) {
                    String role = rs.getString("role");
                    int userId = rs.getInt("id");
    
                    // Set session attributes
                    HttpSession session = request.getSession();
                    session.setAttribute("username", username);
                    session.setAttribute("role", role);
                    session.setAttribute("userId", userId);
    
                    // Redirect based on role
                    if ("teacher".equalsIgnoreCase(role)) {
                        response.sendRedirect("teacher_dashboard.html");
                    } else {
                        response.sendRedirect("student_dashboard.html");
                    }
                } else {
                    response.setStatus(401); // Unauthorized
                    response.getWriter().write("❌ Invalid credentials.");
                }
    
            } catch (SQLException e) {
                e.printStackTrace();
                response.setStatus(500);
                response.getWriter().write("❌ Database error.");
            }
        }catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ MySQL driver not found", e);
        }

    }
}
