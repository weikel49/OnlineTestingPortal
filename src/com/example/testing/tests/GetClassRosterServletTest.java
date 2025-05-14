package com.example.testing.tests;

import com.example.testing.DBConfig;
import com.example.testing.GetClassRosterServlet;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GetClassRosterServlet}.
 * 
 * These tests verify access control and response handling for retrieving a class roster,
 * including:
 * - Unauthorized access when session or user ID is missing
 * - Forbidden access if the class does not belong to the requesting teacher
 * - Proper serialization of roster data
 * - Graceful handling of SQL exceptions
 */

public class GetClassRosterServletTest {

    private GetClassRosterServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private StringWriter responseWriter;

    /**
     * Sets up a mock servlet environment with DBConfig overrides and response capture.
     */

    @BeforeEach
    public void setUp() throws Exception {
        servlet = new GetClassRosterServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        Properties props = new Properties();
        props.setProperty("db.url", "jdbc:test");
        props.setProperty("db.user", "user");
        props.setProperty("db.password", "pass");
        Field propsField = DBConfig.class.getDeclaredField("props");
        propsField.setAccessible(true);
        propsField.set(null, props);
    }

    /**
     * Verifies that a request with no session returns 401 Unauthorized.
     */

    @Test
    public void testUnauthorizedWhenSessionIsNull() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        servlet.doGet(request, response);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("Not logged in"));
    }

    /**
     * Verifies that a request with a missing userId attribute returns 401 Unauthorized.
     */

    @Test
    public void testUnauthorizedWhenUserIdMissing() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(null);
        servlet.doGet(request, response);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("Not logged in"));
    }

    /**
     * Verifies that a user cannot access a class they do not own.
     */

    @Test
    public void testForbiddenIfTeacherDoesNotOwnClass() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ownershipStmt = mock(PreparedStatement.class);
        ResultSet ownershipRs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(ownershipStmt);
        when(ownershipStmt.executeQuery()).thenReturn(ownershipRs);
        when(ownershipRs.next()).thenReturn(false);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1);
        when(request.getParameter("classId")).thenReturn("5");

        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class)) {
            mockedDriver.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                        .thenReturn(conn);
            servlet.doGet(request, response);
        }

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertTrue(responseWriter.toString().contains("do not have access"));
    }

    /**
     * Verifies that the servlet returns a full class roster including student details.
     */

    @Test
    public void testReturnsClassRosterSuccessfully() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ownershipStmt = mock(PreparedStatement.class);
        PreparedStatement studentStmt = mock(PreparedStatement.class);
        ResultSet ownershipRs = mock(ResultSet.class);
        ResultSet studentRs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("FROM classes")) return ownershipStmt;
            return studentStmt;
        });

        when(ownershipStmt.executeQuery()).thenReturn(ownershipRs);
        when(ownershipRs.next()).thenReturn(true);
        when(ownershipRs.getString("name")).thenReturn("Math Class");

        when(studentStmt.executeQuery()).thenReturn(studentRs);
        doNothing().when(studentStmt).setInt(anyInt(), anyInt());
        when(studentRs.next()).thenReturn(true, false);
        when(studentRs.getString("full_name")).thenReturn("Alice");
        when(studentRs.getString("username")).thenReturn("alice01");
        when(studentRs.getString("password")).thenReturn("pass123");

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1);
        when(request.getParameter("classId")).thenReturn("10");

        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class)) {
            mockedDriver.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                        .thenReturn(conn);
            servlet.doGet(request, response);
        }

        String json = responseWriter.toString();
        assertTrue(json.contains("Alice"));
        assertTrue(json.contains("Math Class"));
        verify(response).setContentType("application/json");
    }

    /**
     * Verifies that a SQL exception results in a 500 error and a user-friendly error message.
     */

    @Test
    public void testHandlesSQLExceptionGracefully() throws Exception {
        Connection conn = mock(Connection.class);
        when(conn.prepareStatement(anyString())).thenThrow(new SQLException("fail"));

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1);
        when(request.getParameter("classId")).thenReturn("5");

        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class)) {
            mockedDriver.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                        .thenReturn(conn);
            servlet.doGet(request, response);
        }

        verify(response).setStatus(500);
        assertTrue(responseWriter.toString().contains("class roster"));
    }
}