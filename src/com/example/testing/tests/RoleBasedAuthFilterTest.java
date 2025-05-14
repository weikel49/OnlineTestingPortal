package com.example.testing.tests;

import com.example.testing.RoleBasedAuthFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RoleBasedAuthFilter}.
 *
 * These tests verify access control based on user roles and session state,
 * including:
 * - Bypassing public pages (e.g., login.html)
 * - Redirecting unauthenticated users
 * - Restricting access to teacher-only or student-only pages
 * - Allowing proper access based on user role
 */

public class RoleBasedAuthFilterTest {

    private RoleBasedAuthFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private FilterChain chain;

    /**
     * Sets up mocks for each test case.
     */

    @BeforeEach
    public void setUp() {
        filter = new RoleBasedAuthFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        chain = mock(FilterChain.class);
    }

    /**
     * Verifies that access to public pages like /login.html bypasses the filter.
     */

    @Test
    public void testLoginPageBypass() throws IOException, ServletException {
        when(request.getServletPath()).thenReturn("/login.html");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoMoreInteractions(response);
    }

    /**
     * Verifies that unauthenticated users are redirected to the login page.
     */

    @Test
    public void testUnauthenticatedUserRedirect() throws IOException, ServletException {
        when(request.getServletPath()).thenReturn("/teacher_dashboard.html");
        when(request.getSession(false)).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("login.html");
        verify(chain, never()).doFilter(request, response);
    }

    /**
     * Verifies that students attempting to access teacher-only pages receive a 403 error.
     */

    @Test
    public void testUnauthorizedTeacherPageAccessByStudent() throws IOException, ServletException {
        when(request.getServletPath()).thenReturn("/teacher_dashboard.html");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("student_user");
        when(session.getAttribute("role")).thenReturn("student");

        filter.doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "🚫 Teachers only.");
        verify(chain, never()).doFilter(request, response);
    }

    /**
     * Verifies that students can access student-only pages.
     */

    @Test
    public void testAuthorizedStudentAccess() throws IOException, ServletException {
        when(request.getServletPath()).thenReturn("/student_dashboard.html");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("student_user");
        when(session.getAttribute("role")).thenReturn("student");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    /**
     * Verifies that teachers can access teacher-only pages.
     */

    @Test
    public void testAuthorizedTeacherAccess() throws IOException, ServletException {
        when(request.getServletPath()).thenReturn("/view_tests.html");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("teacher_user");
        when(session.getAttribute("role")).thenReturn("teacher");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
