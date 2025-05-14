package com.example.testing.tests;

import com.example.testing.DeleteRosterServlet;
import jakarta.servlet.http.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeleteRosterServlet}.
 *
 * Covers authorization checks, invalid input, deletion scenarios,
 * and database failure handling.
 */

public class DeleteRosterServletTest extends BaseServletTest {

    private DeleteRosterServlet servlet;

    /**
     * Initializes the servlet before each test.
     */

    @BeforeEach
    public void setUpServlet() {
        servlet = new DeleteRosterServlet();
    }

    /**
     * Verifies the servlet returns 401 when the session is null.
     */

    @Test
    public void testUnauthorizedWhenSessionIsNull() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        servlet.doDelete(request, response);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("❌ Not logged in."));
    }

    /**
     * Verifies the servlet returns 401 when the session does not include a username.
     */

    @Test
    public void testUnauthorizedWhenUsernameMissing() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn(null);
        servlet.doDelete(request, response);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("❌ Not logged in."));
    }

    /**
     * Verifies the servlet returns 400 when classId is not a valid number.
     */

    @Test
    public void testBadRequestWhenClassIdInvalid() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("teacher1");
        when(request.getParameter("classId")).thenReturn("abc");  // not a number

        servlet.doDelete(request, response);
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("❌ Invalid class ID."));
    }

    /**
     * Verifies the servlet returns 404 when the delete operation affects no rows.
     */

    @Test
    public void testNotFoundWhenNothingDeleted() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.executeUpdate()).thenReturn(0); // no rows deleted

        DriverManagerMock.install(mockConn);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("teacher1");
        when(request.getParameter("classId")).thenReturn("99");

        servlet.doDelete(request, response);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        assertTrue(responseWriter.toString().contains("❌ Class not found."));
    }

    /**
     * Verifies the servlet returns 200 when a class is successfully deleted.
     */

    @Test
    public void testSuccessWhenClassDeleted() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.executeUpdate()).thenReturn(1); // one row deleted

        DriverManagerMock.install(mockConn);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("teacher1");
        when(request.getParameter("classId")).thenReturn("42");

        servlet.doDelete(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertTrue(responseWriter.toString().contains("✅ Class deleted."));
    }

    /**
     * Verifies the servlet handles a thrown SQLException and returns HTTP 500.
     */

    @Test
    public void testServerErrorWhenSQLExceptionThrown() throws Exception {
        Connection mockConn = mock(Connection.class);
        when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("Simulated DB failure"));

        DriverManagerMock.install(mockConn);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("teacher1");
        when(request.getParameter("classId")).thenReturn("42");

        servlet.doDelete(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("❌ Error deleting class."));
    }
}
