package com.example.testing.tests;

import com.example.testing.TestResultsSummaryServlet;
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
 * Unit tests for {@link TestResultsSummaryServlet}.
 *
 * These tests verify:
 * - That authentication is required for viewing results
 * - That test result summaries are correctly fetched and serialized
 * - That unauthorized access is handled with a 401 status
 */

public class TestResultsSummaryServletTest {

    private TestResultsSummaryServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    /**
     * Sets up servlet and mock objects before each test.
     */

    @BeforeEach
    public void setup() {
        servlet = new TestResultsSummaryServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
    }

    /**
     * Verifies that unauthenticated requests are rejected with a 401 Unauthorized response.
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
     * Verifies that the servlet returns a valid JSON summary of student test results
     * for an authenticated teacher.
     */

    @Test
    public void testValidSummaryResults() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("teacher1");
        when(request.getParameter("id")).thenReturn("99");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(true, false);
        when(rs.getString("student")).thenReturn("alice");
        when(rs.getInt("score")).thenReturn(8);
        when(rs.getInt("total")).thenReturn(10);

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
              .thenReturn(conn);

            servlet.doGet(request, response);

            String json = sw.toString();
            assertTrue(json.contains("alice"));
            assertTrue(json.contains("\"score\":8"));
            assertTrue(json.contains("\"total\":10"));
            verify(response).setContentType("application/json");
        }
    }
}
