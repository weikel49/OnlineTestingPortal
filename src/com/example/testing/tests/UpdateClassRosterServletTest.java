package com.example.testing.tests;

import com.example.testing.UpdateClassRosterServlet;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UpdateClassRosterServlet}.
 *
 * These tests verify:
 * - Proper handling of unauthorized access
 * - Correct behavior when updating a class roster with student information
 * - Insertion, linking, and conditional updates to user and class data
 */

public class UpdateClassRosterServletTest {

    private UpdateClassRosterServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    /**
     * Initializes mock servlet and supporting objects.
     */

    @BeforeEach
    public void setup() {
        servlet = new UpdateClassRosterServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
    }

    /**
     * Verifies that a POST request without an active session is rejected with a 401 status.
     */

    @Test
    public void testUnauthorizedAccess() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(sw.toString().contains("❌ Not logged in."));
    }

    /**
     * Verifies that a valid class roster update:
     * - Removes old student links
     * - Checks for user existence
     * - Inserts new users if needed
     * - Links users to the class
     * - Commits all changes
     */

    @Test
    public void testSuccessfulRosterUpdate() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("teacher1");

        String json = """
            {
                "classId": 42,
                "students": [
                    { "fullName": "Alice Smith", "username": "alice", "password": "pass123" }
                ]
            }
            """;

        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(json)));
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        // Mocks for DB
        Connection conn = mock(Connection.class);
        PreparedStatement deleteStmt = mock(PreparedStatement.class);
        PreparedStatement selectUser = mock(PreparedStatement.class);
        PreparedStatement updateUser = mock(PreparedStatement.class);
        PreparedStatement insertUser = mock(PreparedStatement.class);
        PreparedStatement insertLink = mock(PreparedStatement.class);
        PreparedStatement existsStmt = mock(PreparedStatement.class);
        ResultSet userRs = mock(ResultSet.class);
        ResultSet insertKeys = mock(ResultSet.class);
        ResultSet existsRs = mock(ResultSet.class);

        // Stub prepareStatement mappings
        when(conn.prepareStatement(startsWith("DELETE"))).thenReturn(deleteStmt);
        when(conn.prepareStatement(contains("FROM users WHERE username"))).thenReturn(selectUser);
        when(conn.prepareStatement(startsWith("UPDATE"))).thenReturn(updateUser);
        when(conn.prepareStatement(startsWith("INSERT INTO users"))).thenReturn(insertUser);
        when(conn.prepareStatement(startsWith("INSERT INTO class_students"))).thenReturn(insertLink);
        when(conn.prepareStatement(contains("SELECT 1 FROM users WHERE username"))).thenReturn(existsStmt);

        // Stub userExists()
        doNothing().when(existsStmt).setString(eq(1), anyString());
        when(existsStmt.executeQuery()).thenReturn(existsRs);
        when(existsRs.next()).thenReturn(false, false);

        // Simulate: user does NOT exist
        when(selectUser.executeQuery()).thenReturn(userRs, userRs);
        when(userRs.next()).thenReturn(true); // Force it to find the user
        when(userRs.getInt("id")).thenReturn(201);
        when(userRs.getString("password")).thenReturn("pass123");
        when(userRs.getString("full_name")).thenReturn("Alice Smith");

        // Simulate insertUser block and getting generated ID
        when(insertUser.executeUpdate()).thenReturn(1);
        when(insertUser.getGeneratedKeys()).thenReturn(insertKeys);
        when(insertKeys.next()).thenReturn(true);
        when(insertKeys.getInt(1)).thenReturn(201);

        // Simulate addBatch and executeBatch
        doNothing().when(insertLink).setInt(eq(1), anyInt());
        doNothing().when(insertLink).setInt(eq(2), anyInt());
        doNothing().when(insertLink).addBatch();
        when(insertLink.executeBatch()).thenReturn(new int[] {1});

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
              .thenReturn(conn);

            servlet.doPost(request, response);

            verify(deleteStmt).executeUpdate();
            verify(insertLink).addBatch();
            verify(insertLink).executeBatch();
            verify(conn).commit();
            verify(response).setStatus(200);
        }
    }
}
