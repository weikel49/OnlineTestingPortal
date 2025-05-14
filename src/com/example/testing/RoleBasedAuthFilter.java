package com.example.testing;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.*;

/**
 * A servlet filter that restricts access to specific HTML pages based on user roles.
 *
 * Teachers and students can only access pages intended for their role.
 * Unauthenticated users are redirected to the login page.
 */

@WebFilter("/*") // Intercept all requests and filter selectively
public class RoleBasedAuthFilter implements Filter {

    private static final Set<String> teacherOnlyPages = Set.of(
        "/teacher_dashboard.html",
        "/edit_test.html",
        "/create_test.html",
        "/edit_class_roster.html",
        "/manage_class_roster.html",
        "/class_roster_summary.html",
        "/class_roster_detail.html",
        "/view_tests.html",
        "/results_summary.html"
        
    );

    private static final Set<String> studentOnlyPages = Set.of(
        "/student_dashboard.html",
        "/take_test.html",
        "/view_results.html"
    );

    /**
     * Filters requests based on session login status and user role.
     *
     * Redirects unauthenticated users to the login page.
     * Blocks users from accessing pages outside their role.
     *
     * @param request  the incoming ServletRequest
     * @param response the outgoing ServletResponse
     * @param chain    the filter chain to continue processing
     * @throws IOException if an input or output error occurs
     * @throws ServletException if a servlet error occurs
     */

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getServletPath();

        if (path.equals("/login.html")) {
            chain.doFilter(request, response); // allow through with no checks
            return;
        }

        // Only protect HTML pages
        else {  
            if (path.endsWith(".html")) {
                HttpSession session = req.getSession(false);
                if (session == null || session.getAttribute("username") == null) {
                    res.sendRedirect("login.html");
                    return;
                }

                String role = (String) session.getAttribute("role");

                if (teacherOnlyPages.contains(path) && !"teacher".equalsIgnoreCase(role)) {
                    res.sendError(HttpServletResponse.SC_FORBIDDEN, "🚫 Teachers only.");
                    return;
                }

                if (studentOnlyPages.contains(path) && !"student".equalsIgnoreCase(role)) {
                    res.sendError(HttpServletResponse.SC_FORBIDDEN, "🚫 Students only.");
                    return;
                }
            }
        }

        chain.doFilter(request, response); // All clear, continue
    }
}
