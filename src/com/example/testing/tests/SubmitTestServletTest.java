package com.example.testing.tests;

import com.example.testing.*;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SubmitTestServlet}.
 *
 * These tests verify:
 * - That authentication is required for submitting a test
 * - That a test can be submitted and saved successfully
 * - That options and correct answers are processed as expected
 */

public class SubmitTestServletTest {

    private SubmitTestServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    /**
     * Sets up the servlet and mocks before each test.
     */

    @BeforeEach
    public void setup() {
        servlet = new SubmitTestServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
    }

    /**
     * Verifies that a submission attempt without a valid session returns a 401 status.
     */

    @Test
    public void testUnauthorizedSubmission() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(sw.toString().contains("❌ User not logged in"));
    }

    /**
     * Verifies that a valid test submission is processed correctly and saved using TestDAO.
     */

    @Test
    public void testValidSubmission() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(101);

        String json = """
            {
                "title": "Sample Test",
                "questions": [
                    {
                        "questionText": "2+2?",
                        "correctAnswer": "option2",
                        "options": {
                            "option1": "3",
                            "option2": "4",
                            "option3": "5"
                        }
                    }
                ]
            }
            """;

        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(json)));
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        TestDAO daoMock = mock(TestDAO.class);
        when(daoMock.saveTest(any(com.example.testing.Test.class))).thenReturn(true);

        servlet.testDAO = daoMock;

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString())).thenReturn(mock(Connection.class));

            servlet.doPost(request, response);

            verify(response).setStatus(HttpServletResponse.SC_OK);
        }
    }
}
