package com.example.testing;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * Servlet that handles user logout.
 *
 * This servlet invalidates the current user session and redirects the user to the login page.
 */

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    /**
     * Handles GET requests to log the user out.
     *
     * Invalidates the existing session if one exists and redirects to the login page.
     *
     * @param request  the HttpServletRequest that may contain a session
     * @param response the HttpServletResponse used to redirect to the login page
     * @throws IOException if a redirect fails
     */

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false); // Don't create a new session
        if (session != null) {
            session.invalidate(); // ❌ Destroy session
            System.out.println("👋 Session invalidated.");
        }
        response.sendRedirect("login.html"); // Redirect back to login page
    }
}
