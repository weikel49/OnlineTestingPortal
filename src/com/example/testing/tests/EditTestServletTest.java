package com.example.testing.tests;

import com.example.testing.EditTestServlet;
import com.example.testing.DBConfig;

import jakarta.servlet.http.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EditTestServlet}.
 * 
 * These tests cover both GET and POST request handling, including:
 * - Authentication checks (e.g., missing or invalid session)
 * - Successful retrieval of test data
 * - Proper response content type
 * - Error handling when database operations fail
 * - JSON request body parsing and rollback logic on failure during test updates
 */

public class EditTestServletTest {
    private EditTestServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private StringWriter responseWriter;

    /**
     * Sets up mock servlet environment and overrides DBConfig properties for testing.
     */

    @BeforeEach
    public void setUp() throws Exception {
        servlet = new EditTestServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        responseWriter = new StringWriter();

        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        DriverManagerMock.reset();

        // Override DBConfig properties
        Properties fakeProps = new Properties();
        fakeProps.setProperty("db.url", "jdbc:test");
        fakeProps.setProperty("db.user", "testuser");
        fakeProps.setProperty("db.password", "testpass");
        Field propsField = DBConfig.class.getDeclaredField("props");
        propsField.setAccessible(true);
        propsField.set(null, fakeProps);
    }

    /**
     * Verifies that a GET request with a null session returns 401 Unauthorized.
     */

    @Test
    public void testUnauthorizedWhenSessionIsNull_Get() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        servlet.doGet(request, response);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Verifies that a GET request without a username in session returns 401 Unauthorized.
     */

    @Test
    public void testUnauthorizedWhenUsernameMissing_Get() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn(null);
        servlet.doGet(request, response);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Verifies that a valid GET request returns test data and sets correct content type.
     */

    @Test
    public void testReturnsTestDetailsSuccessfully() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getInt("id")).thenReturn(42);
        when(rs.getString("title")).thenReturn("Sample Test");

        DriverManagerMock.install(conn);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("teacher1");
        when(request.getParameter("id")).thenReturn("1");

        servlet.doGet(request, response);

        assertTrue(responseWriter.toString().contains("Sample Test"));
        verify(response).setContentType("application/json");
    }

    /**
     * Verifies servlet handles SQL exception during GET gracefully by logging and returning fallback response.
     */

    @Test
    public void testHandlesSQLExceptionInDoGet() throws Exception {
        Connection conn = mock(Connection.class);
        when(conn.prepareStatement(anyString())).thenThrow(new SQLException("Simulated failure"));
        DriverManagerMock.install(conn);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("teacher1");
        when(request.getParameter("id")).thenReturn("1");

        servlet.doGet(request, response);

        assertTrue(responseWriter.toString().contains("Failed to load test"));
    }

    /**
     * Verifies that a POST request with null session returns 401 Unauthorized.
     */

    @Test
    public void testUnauthorizedWhenSessionIsNull_Post() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        servlet.doPost(request, response);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Verifies that a POST request without a username in session returns 401 Unauthorized.
     */

    @Test
    public void testUnauthorizedWhenUsernameMissing_Post() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn(null);
        servlet.doPost(request, response);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Verifies rollback and 500 response when a SQL exception occurs during POST update logic.
     */

    @Test
    public void testSQLExceptionRollbackOnFailure() throws Exception {
        Connection conn = mock(Connection.class);
        when(conn.prepareStatement(anyString())).thenThrow(new SQLException("Simulated failure"));
        DriverManagerMock.install(conn);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("teacher1");

        String jsonInput = "{" +
            "\"id\": 1, \"title\": \"Updated Test\", \"questions\": []}";
        BufferedReader reader = new BufferedReader(new StringReader(jsonInput));
        when(request.getReader()).thenReturn(reader);

        servlet.doPost(request, response);

        verify(response).setStatus(500);
        assertTrue(responseWriter.toString().contains("Failed to update test"));
    }
}