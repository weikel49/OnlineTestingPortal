package com.example.testing;

import java.util.List;

/**
 * Represents a test consisting of a list of multiple-choice questions.
 *
 * Each test has an ID, title, list of questions, the ID of the teacher who created it,
 * and a time limit in minutes.
 */

public class Test {
    private int id;
    private String title;
    private List<Question> questions;
    private int createdBy;
    private int durationMinutes;

    /**
     * Default constructor.
     */
    
    public Test() {}

    /**
     * Constructs a test with ID, title, and question list.
     *
     * @param id the test ID
     * @param title the title of the test
     * @param questions the list of questions in the test
     */
    
    public Test(int id, String title, List<Question> questions) {
        this.id = id;
        this.title = title;
        this.questions = questions;
    }

    /**
     * @return the ID of the test
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
     * @return the title of the test
     */
    
    public String getTitle() {
        return title;
    }

    /**
     * Sets the test title.
     *
     * @param title the test title
     */

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the list of questions in the test
     */
    
    public List<Question> getQuestions() {
        return questions;
    }

    /**
     * Sets the list of questions.
     *
     * @param questions the questions to include in the test
     */

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    /**
     * @return the ID of the teacher who created the test
     */

    public int getCreatedBy(){
        return createdBy;
    }

    /**
     * Sets the creator of the test.
     *
     * @param createdBy the teacher's user ID
     */

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * @return the duration of the test in minutes
     */

    public int getDurationMinutes() {
        return durationMinutes;
    }

    /**
     * Sets the time limit for the test.
     *
     * @param durationMinutes the test duration in minutes
     */

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
