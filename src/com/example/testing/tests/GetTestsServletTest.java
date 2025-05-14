package com.example.testing.tests;

import com.example.testing.GetTestsServlet;
import com.example.testing.TestDAO;
import com.example.testing.TestSummary;
import jakarta.servlet.http.*;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GetTestsServlet}.
 * 
 * This test suite verifies the servlet's handling of:
 * - Session-based authentication for test summary access
 * - Successful retrieval and serialization of test summaries
 * - Proper HTTP response formatting
 * - Handling of internal exceptions with 500 status and error messaging
 */

public class GetTestsServletTest {

    private GetTestsServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private StringWriter responseWriter;

    /**
     * Sets up a mocked servlet environment and captures response output.
     */

    @BeforeEach
    public void setUp() throws Exception {
        servlet = new GetTestsServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    /**
     * Verifies that a request with no session returns a 401 Unauthorized response.
     */

    @Test
    public void testUnauthorizedWhenSessionIsNull() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        servlet.doGet(request, response);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("Not logged in"));
    }

    /**
     * Verifies that a request missing the user ID returns a 401 Unauthorized response.
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
     * Verifies that valid test summaries are retrieved and returned as JSON with proper headers.
     */

    @Test
    public void testReturnsTestSummariesSuccessfully() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1);

        List<TestSummary> summaries = List.of(new TestSummary(42, "Sample Test", 10));
        try (MockedStatic<TestDAO> mockStatic = mockStatic(TestDAO.class)) {
            mockStatic.when(() -> TestDAO.getTestSummaries(1)).thenReturn(summaries);
            servlet.doGet(request, response);
        }

        String json = responseWriter.toString();
        assertTrue(json.contains("Sample Test"));
        assertTrue(json.contains("42"));
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
    }

    /**
     * Verifies that exceptions during test summary retrieval result in a 500 error response.
     */

    @Test
    public void testHandlesExceptionAndSets500() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1);

        try (MockedStatic<TestDAO> mockStatic = mockStatic(TestDAO.class)) {
            mockStatic.when(() -> TestDAO.getTestSummaries(1)).thenThrow(new RuntimeException("Simulated failure"));
            servlet.doGet(request, response);
        }

        verify(response).setStatus(500);
        assertTrue(responseWriter.toString().contains("Failed to fetch test summaries"));
    }
}
