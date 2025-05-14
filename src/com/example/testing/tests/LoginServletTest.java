package com.example.testing.tests;

import com.example.testing.LoginServlet;
import jakarta.servlet.http.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LoginServlet}.
 *
 * These tests verify the servlet’s login behavior by checking:
 * - Successful authentication and redirection for valid teacher credentials
 * - Session attribute assignment for logged-in users
 * - Proper response when login fails due to incorrect credentials
 */

public class LoginServletTest {

    private LoginServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    /**
     * Sets up mock objects before each test.
     */

    @BeforeEach
    public void setUp() {
        servlet = new LoginServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
    }

    /**
     * Verifies that a teacher with valid credentials is authenticated, assigned session attributes,
     * and redirected to the teacher dashboard.
     */

    @Test
    public void testValidLoginTeacher() throws Exception {
        when(request.getParameter("username")).thenReturn("teacher1");
        when(request.getParameter("password")).thenReturn("test123");
        when(request.getSession()).thenReturn(session);

        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("role")).thenReturn("teacher");
        when(rs.getInt("id")).thenReturn(101);

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
              .thenReturn(conn);

            servlet.doPost(request, response);

            verify(session).setAttribute("username", "teacher1");
            verify(session).setAttribute("role", "teacher");
            verify(session).setAttribute("userId", 101);
            verify(response).sendRedirect("teacher_dashboard.html");
        }
    }

    /**
     * Verifies that an invalid login attempt returns a 401 status and an error message.
     */

    @Test
    public void testInvalidLogin() throws Exception {
        when(request.getParameter("username")).thenReturn("baduser");
        when(request.getParameter("password")).thenReturn("wrongpass");

        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false); // No matching user

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
              .thenReturn(conn);

            servlet.doPost(request, response);

            verify(response).setStatus(401);
            assertTrue(sw.toString().contains("❌ Invalid credentials."));
        }
    }
}
