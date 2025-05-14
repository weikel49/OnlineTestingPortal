package com.example.testing.tests;

import com.example.testing.SubmitAnswersServlet;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SubmitAnswersServlet}.
 *
 * These tests verify:
 * - Authorization handling for student answer submissions
 * - Database interaction to store test answers
 * - Batch processing and commit behavior
 */

public class SubmitAnswersServletTest {

    private SubmitAnswersServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    /**
     * Sets up servlet and mock objects before each test.
     */

    @BeforeEach
    public void setup() {
        servlet = new SubmitAnswersServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
    }

    /**
     * Verifies that an unauthenticated user attempting to submit answers receives a 401 error.
     */

    @Test
    public void testUnauthorizedSubmission() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        servlet.doPost(request, response);

        verify(response).setStatus(401);
        assertTrue(sw.toString().contains("❌ Not logged in."));
    }

    /**
     * Verifies that a valid test submission is parsed, stored in the database using batch inserts,
     * and committed successfully.
     */

    @Test
    public void testSuccessfulSubmission() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("student1");

        String json = """
            {
                "testId": 12,
                "answers": [
                    {"questionId": 1, "selectedAnswer": "A", "correctAnswer": "A"},
                    {"questionId": 2, "selectedAnswer": "B", "correctAnswer": "C"}
                ]
            }
            """;

        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(json)));
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        // DB mocks
        Connection conn = mock(Connection.class);
        PreparedStatement userStmt = mock(PreparedStatement.class);
        PreparedStatement insertStmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        // Prepare mocks for each distinct SQL call
        when(conn.prepareStatement(eq("SELECT id FROM users WHERE username = ?"))).thenReturn(userStmt);
        when(conn.prepareStatement(contains("INSERT INTO results"))).thenReturn(insertStmt);
        when(userStmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt("id")).thenReturn(101);

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
            .thenReturn(conn);

            servlet.doPost(request, response);

            // Verify batch execution
            verify(insertStmt, times(2)).addBatch();
            verify(insertStmt).executeBatch();
            verify(conn).commit();
        }
    }

}
