package com.example.testing;

/**
 * Entry point for running setup logic outside of a servlet context.
 *
 * This class invokes the DatabaseInitializer to set up the database schema
 * and insert any required defaults.
 */
public class Main {

    /**
     * Starts the database initialization process.
     *
     * Can optionally be extended to launch additional setup tasks or a local UI.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        DatabaseInitializer.main();  // Runs schema.sql
        // Optionally start a UI or test logic here
    }
}
