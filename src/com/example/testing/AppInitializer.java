package com.example.testing;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Application startup listener for the Online Testing Portal.
 * 
 * This class is triggered when the web application is deployed. It is used
 * to initialize application-wide resources like database configuration.
 */

@WebListener
public class AppInitializer implements ServletContextListener {

    /**
     * Called when the web application is starting up.
     * 
     * This method attempts to load the {@link DBConfig} class early,
     * so the database connection is initialized at startup.
     *
     * @param sce the {@link ServletContextEvent} containing the context info
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            // Force DBConfig to load at startup
            System.out.println("🔧 Warming up DBConfig: " + DBConfig.getUrl());
        } catch (Exception e) {
            System.err.println("❌ Failed to load DBConfig on startup");
            e.printStackTrace();
        }
    }
}
