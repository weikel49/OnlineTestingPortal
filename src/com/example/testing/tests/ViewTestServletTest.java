package com.example.testing.tests;

import com.example.testing.ViewTestServlet;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ViewTestServlet}.
 *
 * These tests verify:
 * - That authentication is required for loading test data
 * - That test data including questions and options is correctly fetched and serialized
 * - That a missing test ID results in a 404 response
 */

public class ViewTestServletTest {

    private ViewTestServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    /**
     * Initializes servlet and mocks before each test.
     */

    @BeforeEach
    public void setup() {
        servlet = new ViewTestServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
    }

    /**
     * Verifies that unauthenticated access is blocked and returns 401.
     */

    @Test
    public void testUnauthorizedAccess() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(sw.toString().contains("❌ Not logged in."));
    }

    /**
     * Verifies that a valid request returns full test data including title,
     * questions, options, and metadata in JSON format.
     */

    @Test
    public void testValidTestLoad() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("teacher1");
        when(request.getParameter("id")).thenReturn("15");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        // Mock DB
        Connection conn = mock(Connection.class);
        PreparedStatement testStmt = mock(PreparedStatement.class);
        PreparedStatement questionStmt = mock(PreparedStatement.class);
        PreparedStatement optionStmt = mock(PreparedStatement.class);

        ResultSet testRs = mock(ResultSet.class);
        ResultSet questionRs = mock(ResultSet.class);
        ResultSet optionRs = mock(ResultSet.class);

        when(conn.prepareStatement(startsWith("SELECT title"))).thenReturn(testStmt);
        when(conn.prepareStatement(startsWith("SELECT id"))).thenReturn(questionStmt);
        when(conn.prepareStatement(startsWith("SELECT option_text"))).thenReturn(optionStmt);

        when(testStmt.executeQuery()).thenReturn(testRs);
        when(questionStmt.executeQuery()).thenReturn(questionRs);
        when(optionStmt.executeQuery()).thenReturn(optionRs);

        when(testRs.next()).thenReturn(true);
        when(testRs.getString("title")).thenReturn("Sample Test");
        when(testRs.getInt("duration_minutes")).thenReturn(30);

        when(questionRs.next()).thenReturn(true, false);
        when(questionRs.getInt("id")).thenReturn(1001);
        when(questionRs.getString("question_text")).thenReturn("What is 2 + 2?");
        when(questionRs.getString("correct_answer")).thenReturn("option2");

        when(optionRs.next()).thenReturn(true, true, false);
        when(optionRs.getString("option_text")).thenReturn("3").thenReturn("4");

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
              .thenReturn(conn);

            servlet.doGet(request, response);

            String json = sw.toString();
            assertTrue(json.contains("Sample Test"));
            assertTrue(json.contains("What is 2 + 2?"));
            assertTrue(json.contains("option1"));
            assertTrue(json.contains("option2"));
            assertTrue(json.contains("3"));
            assertTrue(json.contains("4"));
            verify(response).setContentType("application/json");
        }
    }

    /**
     * Verifies that requesting a test ID that does not exist returns a 404 response.
     */

    @Test
    public void testTestNotFound() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("teacher1");
        when(request.getParameter("id")).thenReturn("999");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        Connection conn = mock(Connection.class);
        PreparedStatement testStmt = mock(PreparedStatement.class);
        ResultSet testRs = mock(ResultSet.class);

        when(conn.prepareStatement(startsWith("SELECT title"))).thenReturn(testStmt);
        when(testStmt.executeQuery()).thenReturn(testRs);
        when(testRs.next()).thenReturn(false);

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
              .thenReturn(conn);

            servlet.doGet(request, response);

            verify(response).setStatus(404);
            assertTrue(sw.toString().contains("❌ Test not found."));
        }
    }
}
