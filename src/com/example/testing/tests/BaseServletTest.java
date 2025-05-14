package com.example.testing.tests;

import com.example.testing.DBConfig;
import jakarta.servlet.http.*;

import org.junit.jupiter.api.BeforeEach;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;

import static org.mockito.Mockito.*;

/**
 * Base class for servlet unit tests.
 *
 * Provides reusable setup logic for mocking servlet components,
 * injecting fake database configuration, and resetting JDBC drivers.
 * All servlet test classes should extend this to reduce duplication.
 */

public abstract class BaseServletTest {

    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected HttpSession session;
    protected StringWriter responseWriter;

    /**
     * Runs before each test in subclasses to initialize mocks and configuration.
     *
     * @throws Exception if an error occurs during mock or config setup
     */

    @BeforeEach
    public void baseSetUp() throws Exception {
        resetDriverManager();
        setUpMocks();
        injectFakeDbConfig();
    }

    /**
     * Sets up mocked request, response, session, and output writer.
     *
     * @throws Exception if any mock cannot be created
     */

    @BeforeEach
    protected void setUpMocks() throws Exception {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    /**
     * Replaces static database configuration in {@link DBConfig} with a test-safe set of values.
     *
     * @throws Exception if reflection fails
     */

    protected void injectFakeDbConfig() throws Exception {
        Properties fakeProps = new Properties();
        fakeProps.setProperty("db.url", "jdbc:test");
        fakeProps.setProperty("db.user", "testuser");
        fakeProps.setProperty("db.password", "testpass");

        Field propsField = DBConfig.class.getDeclaredField("props");
        propsField.setAccessible(true);
        propsField.set(null, fakeProps);
    }

    /**
     * Unregisters any test JDBC drivers that may interfere with other tests.
     *
     * @throws SQLException if deregistration fails
     */

    protected void resetDriverManager() throws SQLException {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getName().contains("DriverManagerMock$")) {
                DriverManager.deregisterDriver(driver);
            }
        }
    }
}
