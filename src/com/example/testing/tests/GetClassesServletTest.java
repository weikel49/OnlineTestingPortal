package com.example.testing.tests;

import com.example.testing.DBConfig;
import com.example.testing.GetClassesServlet;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 * Unit tests for {@link GetClassesServlet}.
 * 
 * These tests verify authentication enforcement, proper response structure,
 * and error handling for scenarios including:
 * - Missing or unauthorized sessions
 * - Successful retrieval of classes assigned to a teacher
 * - SQL exception during query execution
 */

public class GetClassesServletTest {

    private GetClassesServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private StringWriter responseWriter;

    /**
     * Sets up the test environment with mocked servlet, request, response, and session.
     */

    @BeforeEach
    public void setUp() throws Exception {
        servlet = new GetClassesServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    /**
     * Verifies that a request with no session returns a 401 Unauthorized status.
     */

    @Test
    public void testUnauthorizedWhenSessionIsNull() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        servlet.doGet(request, response);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("Not logged in"));
    }

    /**
     * Verifies that a request without a userId in session returns a 401 Unauthorized status.
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
     * Verifies that a valid request returns a list of classes as a JSON array.
     */

    @Test
    public void testReturnsClassListSuccessfully() throws Exception {
        // Mock DB config
        Properties props = new Properties();
        props.setProperty("db.url", "jdbc:test");
        props.setProperty("db.user", "user");
        props.setProperty("db.password", "pass");
        Field propsField = DBConfig.class.getDeclaredField("props");
        propsField.setAccessible(true);
        propsField.set(null, props);

        // Mock DB objects
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        // Ensure every prepareStatement returns the same mock
        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        doNothing().when(stmt).setInt(anyInt(), anyInt());
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getInt("id")).thenReturn(1);
        when(rs.getString("name")).thenReturn("Test Class");

        // Mock session and request
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1);

        try (var mockedDriverManager = mockStatic(DriverManager.class)) {
            mockedDriverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                .thenReturn(conn);
            servlet.doGet(request, response);
        }

        // Capture and validate output
        String json = responseWriter.toString();
        JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
        assertEquals(1, arr.size());
        assertTrue(json.contains("Test Class"));
        verify(response).setContentType("application/json");
    }

    /**
     * Verifies that a SQL exception during class retrieval returns a 500 error status.
     */

    @Test
    public void testServerErrorWhenSQLExceptionThrown() throws Exception {
        Properties props = new Properties();
        props.setProperty("db.url", "jdbc:test");
        props.setProperty("db.user", "user");
        props.setProperty("db.password", "pass");
        Field propsField = DBConfig.class.getDeclaredField("props");
        propsField.setAccessible(true);
        propsField.set(null, props);

        // Simulate DB error
        Connection conn = mock(Connection.class);
        when(conn.prepareStatement(anyString())).thenThrow(new SQLException("Simulated DB failure"));
        DriverManagerMock.install(conn);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1);

        servlet.doGet(request, response);

        verify(response).setStatus(500);
        assertTrue(responseWriter.toString().contains("Error fetching classes"));
    }
}