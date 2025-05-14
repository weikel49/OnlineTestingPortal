package com.example.testing;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for managing test-related database operations.
 *
 * Provides methods to save a full test (including questions and options)
 * and retrieve summary information about tests created by a teacher.
 */

public class TestDAO {

    /**
     * Saves a test, its questions, and corresponding answer options to the database.
     *
     * This method inserts a new test into the tests table, adds all associated questions,
     * and batches all answer options. It uses a retry-safe commit in case of lock issues.
     *
     * @param test the Test object containing title, questions, and options
     * @return true if the test was saved successfully
     * @throws SQLException if a database error occurs
     * @throws InterruptedException if the thread is interrupted while retrying commit
     */

    public boolean saveTest(Test test) throws SQLException, InterruptedException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            try (Connection conn = DriverManager.getConnection(
                DBConfig.getUrl(),
                DBConfig.getUser(),
                DBConfig.getPassword()
            );
            ) {
                conn.setAutoCommit(false);
        
                // Insert test
                String insertTest = "INSERT INTO tests (title, created_by, duration_minutes) VALUES (?, ?, ?)";
                try (PreparedStatement testStmt = conn.prepareStatement(insertTest)) {
                    testStmt.setString(1, test.getTitle());
                    testStmt.setInt(2, test.getCreatedBy());
                    testStmt.setInt(3, test.getDurationMinutes());
                    testStmt.executeUpdate();
                }
        
                // Get generated test ID
                int testId;
                try (Statement idStmt = conn.createStatement();
                    ResultSet rs = idStmt.executeQuery("SELECT LAST_INSERT_ID()")) {
                    testId = rs.next() ? rs.getInt(1) : -1;
                }
        
                // Insert questions and options
                String insertQuestion = "INSERT INTO questions (test_id, question_text, correct_answer) VALUES (?, ?, ?)";
                String insertOption = "INSERT INTO options (question_id, option_text) VALUES (?, ?)";
        
                try (PreparedStatement questionStmt = conn.prepareStatement(insertQuestion);
                    PreparedStatement optionStmt = conn.prepareStatement(insertOption)) {
        
                    for (Question q : test.getQuestions()) {
                        questionStmt.setInt(1, testId);
                        questionStmt.setString(2, q.getQuestionText());
                        questionStmt.setString(3, q.getCorrectAnswer());
                        questionStmt.executeUpdate();
        
                        int questionId;
                        try (ResultSet qrs = conn.createStatement().executeQuery("SELECT LAST_INSERT_ID()")) {
                            questionId = qrs.next() ? qrs.getInt(1) : -1;
                        }
        
                        for (String opt : q.getOptions().values()) {
                            optionStmt.setInt(1, questionId);
                            optionStmt.setString(2, opt);
                            optionStmt.addBatch();
                        }
                    }
                    optionStmt.executeBatch();
                }
        
                // Retry-safe commit
                int retries = 5;
                while (retries-- > 0) {
                    try {
                        conn.commit();
                        System.out.println("✅ Test and questions saved successfully.");
                        break;
                    } catch (SQLException e) {
                        if (e.getMessage().contains("database is locked")) {
                            System.out.println("⏳ Retry commit...");
                            Thread.sleep(100);
                        } else {
                            throw e;
                        }
                    }
                }
            }
        }catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ MySQL driver not found", e);
        }
        return true;
    }
    
    /**
     * Retrieves summaries of all tests created by the specified teacher.
     *
     * Each summary includes the test ID, title, and number of associated questions.
     *
     * @param teacherId the ID of the teacher
     * @return a list of TestSummary objects
     */

    public static List<TestSummary> getTestSummaries(int teacherId) {
        List<TestSummary> summaries = new ArrayList<>();

        String sql = """
            SELECT t.id, t.title, COUNT(q.id) AS questionCount
            FROM tests t
            LEFT JOIN questions q ON t.id = q.test_id
            WHERE t.created_by = ?
            GROUP BY t.id, t.title
        """;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(
                DBConfig.getUrl(),
                DBConfig.getUser(),
                DBConfig.getPassword());
                PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, teacherId);

                    try (ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        TestSummary summary = new TestSummary();
                        summary.setId(rs.getInt("id"));
                        summary.setTitle(rs.getString("title"));
                        summary.setQuestionCount(rs.getInt("questionCount"));
                        summaries.add(summary);
                    }

                    System.out.println("✅ Fetched test summaries");
                }

            } catch (SQLException e) {
                System.out.println("❌ Error fetching test summaries: " + e.getMessage());
                e.printStackTrace();
            }
        }catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ MySQL driver not found", e);
        }
        return summaries;
    }
}
