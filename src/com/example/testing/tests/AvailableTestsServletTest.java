package com.example.testing.tests;

import com.example.testing.AvailableTestsServlet;
import com.example.testing.DBConfig;
import jakarta.servlet.http.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link AvailableTestsServlet}.
 *
 * Tests cover behavior for unauthorized access, simulated database failure,
 * and successful retrieval of available tests.
 */


public class AvailableTestsServletTest extends BaseServletTest {

    private AvailableTestsServlet servlet;

    /**
     * Initializes the servlet before each test.
     */

    @BeforeEach
    public void setUpServlet() {
        servlet = new AvailableTestsServlet();
    }

    /**
     * Verifies that a 401 status is returned when the session is null.
     *
     * @throws Exception if an error occurs during servlet execution
     */

    @Test
    public void testUnauthorizedWhenSessionIsNull() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        servlet.doGet(request, response);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("❌ Not logged in."));
    }

    /**
     * Verifies that the servlet handles a simulated database failure and returns HTTP 500.
     *
     * @throws Exception if an error occurs during mocking or execution
     */

    @Test
    public void testInternalServerErrorWhenDbFails() throws Exception {
        // Inject fake config
        Properties fakeProps = new Properties();
        fakeProps.setProperty("db.url", "jdbc:test");
        fakeProps.setProperty("db.user", "testuser");
        fakeProps.setProperty("db.password", "testpass");

        Field propsField = DBConfig.class.getDeclaredField("props");
        propsField.setAccessible(true);
        propsField.set(null, fakeProps);

        Connection mockConn = mock(Connection.class);
        when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("Simulated failure"));
        DriverManagerMock.install(mockConn);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("student1");
        when(session.getAttribute("userId")).thenReturn(1);
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        servlet.doGet(request, response);

        verify(response).setStatus(500);
        assertTrue(responseWriter.toString().contains("Failed to fetch available tests."));
    }

    /**
     * Verifies that the servlet successfully returns a list of available tests for a student.
     *
     * @throws Exception if an error occurs during setup or execution
     */

    @Test
    public void testReturnsAvailableTestsSuccessfully() throws Exception {
        // Inject fake DBConfig
        Properties fakeProps = new Properties();
        fakeProps.setProperty("db.url", "jdbc:test");
        fakeProps.setProperty("db.user", "testuser");
        fakeProps.setProperty("db.password", "testpass");

        Field propsField = DBConfig.class.getDeclaredField("props");
        propsField.setAccessible(true);
        propsField.set(null, fakeProps);

        // Mocks for two SQL statements
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockMainStmt = mock(PreparedStatement.class);
        PreparedStatement mockSubqueryStmt = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        // ResultSet returns one record, then no more
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(42);
        when(mockResultSet.getString("title")).thenReturn("Sample Test");

        // Statement behavior
        when(mockMainStmt.executeQuery()).thenReturn(mockResultSet);
        doNothing().when(mockMainStmt).setInt(anyInt(), anyInt());

        when(mockSubqueryStmt.executeQuery()).thenReturn(mockResultSet);
        doNothing().when(mockSubqueryStmt).setInt(anyInt(), anyInt());

        // Statement selection
        when(mockConn.prepareStatement(anyString()))
            .thenReturn(mockMainStmt)
            .thenReturn(mockSubqueryStmt);

        DriverManagerMock.install(mockConn);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("student1");
        when(session.getAttribute("userId")).thenReturn(1);
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        try {
            servlet.doGet(request, response);
            String output = responseWriter.toString();
            assertTrue(output.contains("Sample Test"));
            assertTrue(output.contains("\"id\":42"));
            verify(response).setContentType("application/json");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Servlet threw an unexpected exception: " + e.getMessage());
        }
    }
}
