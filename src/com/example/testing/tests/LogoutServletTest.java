package com.example.testing.tests;

import com.example.testing.LogoutServlet;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LogoutServlet}.
 *
 * These tests verify session termination and redirection behavior:
 * - Ensures an active session is invalidated and redirected to login page
 * - Ensures no error occurs when there is no session
 */

public class LogoutServletTest {

    private LogoutServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    /**
     * Sets up the servlet and mocked request/response/session objects.
     */

    @BeforeEach
    public void setUp() {
        servlet = new LogoutServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
    }

    /**
     * Verifies that an active session is invalidated and redirected to the login page.
     */

    @Test
    public void testLogoutWithActiveSession() throws Exception {
        when(request.getSession(false)).thenReturn(session);

        servlet.doGet(request, response);

        verify(session).invalidate();
        verify(response).sendRedirect("login.html");
    }

    /**
     * Verifies that if no session exists, the servlet still redirects to the login page.
     */

    @Test
    public void testLogoutWithNoSession() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).sendRedirect("login.html");
        verify(session, never()).invalidate();
    }
}
