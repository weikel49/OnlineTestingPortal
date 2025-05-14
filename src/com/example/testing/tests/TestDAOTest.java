package com.example.testing.tests;

import com.example.testing.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

/**
 * Unit tests for methods in {@link TestDAO}.
 *
 * These tests verify:
 * - That test summaries are retrieved correctly from the database
 * - That a test and its associated questions and options are saved properly using batch inserts
 * - That generated keys are used correctly for linking test and question records
 */

public class TestDAOTest {

    /**
     * Verifies that test summaries are correctly fetched and mapped into TestSummary objects.
     */

    @Test
    public void testGetTestSummaries() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getInt("id")).thenReturn(1);
        when(rs.getString("title")).thenReturn("Sample Test");
        when(rs.getInt("questionCount")).thenReturn(5);

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
              .thenReturn(conn);

            List<TestSummary> results = TestDAO.getTestSummaries(101);
            assertEquals(1, results.size());
            assertEquals("Sample Test", results.get(0).getTitle());
            assertEquals(5, results.get(0).getQuestionCount());
        }
    }

    /**
     * Verifies that a test with one question and multiple options is saved successfully to the database.
     * Ensures the use of batch insert and commit operations.
     */

    @Test
    public void testSaveTestSuccess() throws Exception {
        // Setup mock test object
        Question q = new Question();
        q.setQuestionText("Capital of France?");
        q.setCorrectAnswer("Paris");
        q.setOptions(Map.of("option1", "Paris", "option2", "Berlin"));

        com.example.testing.Test test = new com.example.testing.Test();

        test.setTitle("Geography");
        test.setCreatedBy(101);
        test.setDurationMinutes(30);
        test.setQuestions(List.of(q));

        // Mock DB components
        Connection conn = mock(Connection.class);
        PreparedStatement testStmt = mock(PreparedStatement.class);
        PreparedStatement questionStmt = mock(PreparedStatement.class);
        PreparedStatement optionStmt = mock(PreparedStatement.class);
        Statement testIdStmt = mock(Statement.class);
        Statement questionIdStmt = mock(Statement.class);
        ResultSet testIdRs = mock(ResultSet.class);
        ResultSet questionIdRs = mock(ResultSet.class);

        when(conn.prepareStatement(contains("INSERT INTO tests"))).thenReturn(testStmt);
        when(conn.prepareStatement(contains("INSERT INTO questions"))).thenReturn(questionStmt);
        when(conn.prepareStatement(contains("INSERT INTO options"))).thenReturn(optionStmt);
        when(conn.createStatement()).thenReturn(testIdStmt, questionIdStmt);
        when(testIdStmt.executeQuery("SELECT LAST_INSERT_ID()")).thenReturn(testIdRs);
        when(questionIdStmt.executeQuery("SELECT LAST_INSERT_ID()")).thenReturn(questionIdRs);
        when(testIdRs.next()).thenReturn(true);
        when(testIdRs.getInt(1)).thenReturn(10); // testId

        when(questionIdRs.next()).thenReturn(true);
        when(questionIdRs.getInt(1)).thenReturn(20); // questionId

        

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
              .thenReturn(conn);

            TestDAO dao = new TestDAO();
            boolean result = dao.saveTest(test);

            assertTrue(result);
            verify(testStmt).setString(1, "Geography");
            verify(questionStmt).setString(2, "Capital of France?");
            verify(optionStmt, times(2)).addBatch();
            verify(optionStmt).executeBatch();
            verify(conn).commit();
        }
    }
}
