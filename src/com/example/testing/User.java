package com.example.testing;

/**
 * Represents a user in the system.
 *
 * Contains the username, password, and role (e.g., "student" or "teacher").
 * Used primarily for authentication and role-based logic.
 */

public class User {
    private String username;
    private String password;
    private String role;

    /**
     * Constructs a User object with the provided credentials and role.
     *
     * @param username the user's login name
     * @param password the user's password
     * @param role     the role of the user (e.g., "student", "teacher")
     */

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    /**
     * @return the username
     */

    public String getUsername() {
        return username;
    }

    /**
     * @return the password
     */

    public String getPassword() {
        return password;
    }

    /**
     * @return the user's role
     */

    public String getRole() {
        return role;
    }
}
