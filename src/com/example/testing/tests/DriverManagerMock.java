package com.example.testing.tests;

import java.sql.*;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Utility class for mocking JDBC connections using DriverManager during tests.
 * Allows redirecting JDBC calls to a mocked {@link Connection} when using the "jdbc:test" URL prefix.
 */

public class DriverManagerMock {

    /**
     * Installs a fake JDBC driver that returns the provided mock connection
     * when the connection URL starts with "jdbc:test".
     *
     * @param mockConnection The mocked {@link Connection} to return.
     * @throws SQLException If an error occurs while registering the driver.
     */

    public static void install(Connection mockConnection) throws SQLException {
        Driver fakeDriver = new Driver() {
            @Override
            public Connection connect(String url, Properties info) {
                if (url.startsWith("jdbc:test")) {
                    return mockConnection;
                }
                return null;
            }

            @Override public boolean acceptsURL(String url) { 
                return url.startsWith("jdbc:test"); 
            }

            @Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) { 
                return new DriverPropertyInfo[0]; 
            }

            @Override public int getMajorVersion() { 
                return 1; 
            }

            @Override public int getMinorVersion() { 
                return 0; 
            }

            @Override public boolean jdbcCompliant() { 
                return false; 
            }

            @Override public java.util.logging.Logger getParentLogger() { 
                return null; 
            }
        };

        DriverManager.registerDriver(fakeDriver);
    }

    /**
     * Deregisters any drivers registered by this mock class from the {@link DriverManager}.
     * Useful for cleaning up between unit test runs.
     *
     * @throws SQLException If an error occurs while deregistering drivers.
     */
    public static void reset() throws SQLException {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver d = drivers.nextElement();
            if (d.getClass().getName().contains("DriverManagerMock")) {
                DriverManager.deregisterDriver(d);
            }
        }
    }
}
