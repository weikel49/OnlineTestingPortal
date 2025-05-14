package com.example.testing.tests;

import com.example.testing.StudentTestResultsServlet;
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
 * Unit tests for {@link StudentTestResultsServlet}.
 *
 * These tests verify:
 * - Authentication enforcement for viewing test results
 * - Proper retrieval and scoring of results from the database
 * - JSON response formatting of detailed result data
 */

public class StudentTestResultsServletTest {

    private StudentTestResultsServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    /**
     * Sets up mocked servlet, request, response, and session objects.
     */

    @BeforeEach
    public void setup() {
        servlet = new StudentTestResultsServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
    }

    /**
     * Verifies that unauthenticated users are blocked from accessing results
     * and receive a 401 Unauthorized status.
     */

    @Test
    public void testUnauthenticatedAccess() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        servlet.doGet(request, response);

        verify(response).setStatus(401);
        assertTrue(sw.toString().contains("❌ Not logged in."));
    }

    /**
     * Verifies that valid results for a student are returned correctly,
     * with accurate score calculation and response structure.
     */

    @Test
    public void testValidResultsReturned() throws Exception {
        when(request.getParameter("testId")).thenReturn("42");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("student1");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        // Mock DB objects
        Connection conn = mock(Connection.class);
        PreparedStatement userStmt = mock(PreparedStatement.class);
        PreparedStatement resultStmt = mock(PreparedStatement.class);
        ResultSet userRs = mock(ResultSet.class);
        ResultSet resultRs = mock(ResultSet.class);

        // User lookup
        when(conn.prepareStatement("SELECT id FROM users WHERE username = ?")).thenReturn(userStmt);
        when(userStmt.executeQuery()).thenReturn(userRs);
        when(userRs.next()).thenReturn(true);
        when(userRs.getInt("id")).thenReturn(101);

        // Result query
        when(conn.prepareStatement(anyString())).thenReturn(userStmt, resultStmt);
        when(resultStmt.executeQuery()).thenReturn(resultRs);

        when(resultRs.next()).thenReturn(true, true, false);
        when(resultRs.getString("question_text")).thenReturn("Q1?", "Q2?");
        when(resultRs.getString("selected_answer")).thenReturn("A", "B");
        when(resultRs.getString("correct_answer")).thenReturn("A", "C");

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
              .thenReturn(conn);

            servlet.doGet(request, response);

            verify(response).setContentType("application/json");
            String json = sw.toString();
            assertTrue(json.contains("\"score\":1"));
            assertTrue(json.contains("\"total\":2"));
            assertTrue(json.contains("Q1?"));
            assertTrue(json.contains("Q2?"));
        }
    }
}
