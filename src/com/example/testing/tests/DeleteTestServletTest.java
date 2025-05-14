package com.example.testing.tests;

import com.example.testing.DeleteTestServlet;
import jakarta.servlet.http.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeleteTestServlet}.
 *
 * These tests cover session-based authorization, successful deletion,
 * SQL exceptions, and retry logic during commit failure.
 */

public class DeleteTestServletTest extends BaseServletTest {

    private DeleteTestServlet servlet;

    /**
     * Initializes the servlet before each test.
     */

    @BeforeEach
    public void setUpServlet() {
        servlet = new DeleteTestServlet();
    }

    /**
     * Verifies that the servlet returns 401 if the session is null.
     */

    @Test
    public void testUnauthorizedWhenSessionIsNull() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        servlet.doDelete(request, response);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("❌ Not logged in."));
    }

    /**
     * Verifies that the servlet returns 401 if the session has no username attribute.
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
     * Tests that the servlet deletes a test and commits successfully.
     */

    @Test
    public void testSuccessWhenTestDeleted() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.executeUpdate()).thenReturn(1);

        DriverManagerMock.install(mockConn);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("teacher1");
        when(request.getParameter("id")).thenReturn("5");

        servlet.doDelete(request, response);

        verify(mockConn).commit();
        verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Tests that the servlet handles a SQL exception during deletion.
     */

    @Test
    public void testServerErrorWhenExecuteUpdateFails() throws Exception {
        Connection mockConn = mock(Connection.class);
        when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("fail"));

        DriverManagerMock.install(mockConn);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("teacher1");
        when(request.getParameter("id")).thenReturn("5");

        servlet.doDelete(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Tests that commit retries once after a transient 'database is locked' error.
     */

    @Test
    public void testCommitFailsAndRetries() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.executeUpdate()).thenReturn(1);

        // Simulate commit throwing 'database is locked' once, then succeeding
        doThrow(new SQLException("database is locked"))
            .doNothing()
            .when(mockConn).commit();

        DriverManagerMock.install(mockConn);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("teacher1");
        when(request.getParameter("id")).thenReturn("5");

        servlet.doDelete(request, response);

        verify(mockConn, times(2)).commit();
        verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Tests that the servlet gives up after repeated commit failures.
     */

    @Test
    public void testCommitFailsPermanently() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.executeUpdate()).thenReturn(1);

        // Always throw 'database is locked' to simulate retry failure
        doThrow(new SQLException("database is locked")).when(mockConn).commit();

        DriverManagerMock.install(mockConn);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("username")).thenReturn("teacher1");
        when(request.getParameter("id")).thenReturn("5");

        servlet.doDelete(request, response);

        verify(mockConn, times(5)).commit(); // 1 try + 2 retries
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
