package com.example.testing.tests;

import com.example.testing.ManageClassRosterServlet;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ManageClassRosterServlet}.
 *
 * These tests verify handling of class roster creation requests, including:
 * - Authentication enforcement for POST requests
 * - Processing and persisting new class and student data
 * - Proper JSON response structure and database interaction
 */

public class ManageClassRosterServletTest {

    private ManageClassRosterServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    /**
     * Sets up the servlet and mocks before each test.
     */

    @BeforeEach
    public void setup() {
        servlet = new ManageClassRosterServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
    }

    /**
     * Verifies that a request without a session is rejected with a 401 Unauthorized response.
     */

    @Test
    public void testUnauthorizedRequest() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        servlet.doPost(request, response);

        verify(response).setStatus(401);
        assertTrue(sw.toString().contains("❌ Not logged in"));
    }

    /**
     * Verifies that a valid class roster submission is processed and committed to the database.
     * Also checks that the response includes the submitted data.
     */

    @Test
    public void testSuccessfulRosterSubmission() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(101);  // teacher ID

        String jsonInput = """
            {
                "className": "Biology 101",
                "students": ["Alice Johnson", "Bob Dylan"]
            }
            """;

        BufferedReader reader = new BufferedReader(new StringReader(jsonInput));
        when(request.getReader()).thenReturn(reader);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        // Mock database
        Connection conn = mock(Connection.class);
        PreparedStatement classStmt = mock(PreparedStatement.class);
        PreparedStatement userStmt = mock(PreparedStatement.class);
        PreparedStatement linkStmt = mock(PreparedStatement.class);
        PreparedStatement checkStmt = mock(PreparedStatement.class);
        ResultSet classKeys = mock(ResultSet.class);
        ResultSet userKeys = mock(ResultSet.class);
        ResultSet userCheck = mock(ResultSet.class);

        when(conn.prepareStatement(contains("INSERT INTO classes"), anyInt())).thenReturn(classStmt);
        when(conn.prepareStatement(contains("INSERT INTO users"), anyInt())).thenReturn(userStmt);
        when(conn.prepareStatement(contains("INSERT INTO class_students"))).thenReturn(linkStmt);
        when(conn.prepareStatement(contains("SELECT 1 FROM users"))).thenReturn(checkStmt);

        when(classStmt.getGeneratedKeys()).thenReturn(classKeys);
        when(userStmt.getGeneratedKeys()).thenReturn(userKeys);
        when(checkStmt.executeQuery()).thenReturn(userCheck);
        when(userCheck.next()).thenReturn(false); // user doesn't exist

        when(classKeys.next()).thenReturn(true);
        when(classKeys.getInt(1)).thenReturn(999);

        when(userKeys.next()).thenReturn(true);
        when(userKeys.getInt(1)).thenReturn(111).thenReturn(112);

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
              .thenReturn(conn);

            servlet.doPost(request, response);

            verify(conn).commit();
            assertTrue(sw.toString().contains("Biology 101"));
            assertTrue(sw.toString().contains("students"));
        }
    }
}
