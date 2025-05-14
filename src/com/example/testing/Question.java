package com.example.testing;

import java.util.Map;

/**
 * Represents a multiple-choice question used in a test.
 *
 * Each question has an ID, the question text, a set of answer options,
 * and a string representing the correct answer key (like "option1").
 */

public class Question {
    private int id;
    private String questionText;
    private Map<String, String> options;
    private String correctAnswer;

    /**
     * Default constructor.
     */

    public Question() {}

    /**
     * Constructs a new Question with all fields set.
     *
     * @param id the question ID
     * @param questionText the text of the question
     * @param options a map of option keys (like "option1") to text
     * @param correctAnswer the key of the correct answer
     */

    public Question(int id, String questionText, Map<String, String> options, String correctAnswer) {
        this.id = id;
        this.questionText = questionText;
        this.options = options;
        this.correctAnswer = correctAnswer;
    }

    /**
     * @return the ID of the question
     */

    public int getId() {
        return id;
    }

    /**
     * Sets the ID of the question.
     *
     * @param id the question ID
     */

    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the text of the question
     */

    public String getQuestionText() {
        return questionText;
    }

    /**
     * Sets the question text.
     *
     * @param questionText the question prompt
     */

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    /**
     * @return the map of option keys to answer choices
     */

    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * Sets the answer options for this question.
     *
     * @param options a map of option keys to option text
     */

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    /**
     * @return the key of the correct answer
     */

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    /**
     * Sets the correct answer key.
     *
     * @param correctAnswer the key representing the correct answer (e.g., "option2")
     */

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
}
