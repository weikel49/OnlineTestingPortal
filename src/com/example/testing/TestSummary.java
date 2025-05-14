package com.example.testing;

/**
 * Represents a summary of a test for display purposes.
 *
 * Includes the test ID, title, and number of questions. Used primarily
 * when listing available or created tests without including full test details.
 */

public class TestSummary {
    private int id;
    private String title;
    private int questionCount;

    /**
     * Default constructor.
     */

    public TestSummary() {
    }

    /**
     * Constructs a summary with all fields provided.
     *
     * @param id the ID of the test
     * @param title the title of the test
     * @param questionCount the number of questions in the test
     */

    public TestSummary(int id, String title, int questionCount) {
        this.id = id;
        this.title = title;
        this.questionCount = questionCount;
    }

    /**
     * @return the test ID
     */

    public int getId() { 
        return id;
    }

    /**
     * Sets the test ID.
     *
     * @param id the test ID
     */

    public void setId(int id) { 
        this.id = id;
    }

    /**
     * @return the test title
     */

    public String getTitle() { 
        return title;
    }

    /**
     * Sets the test title.
     *
     * @param title the title of the test
     */

    public void setTitle(String title) { 
        this.title = title;
    }

    /**
     * @return the number of questions in the test
     */

    public int getQuestionCount() { 
        return questionCount;
    }

    /**
     * Sets the number of questions in the test.
     *
     * @param questionCount total question count
     */

    public void setQuestionCount(int questionCount) { 
        this.questionCount = questionCount;
    }
}
