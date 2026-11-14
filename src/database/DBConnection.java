package database;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * JDBC Connection Manager for Vertex Banking.
 *
 * Demonstrates:
 * - DriverManager configuration and Connection creation
 * - Property-based database credentials
 * - Automatic fallback to Simulated Embedded JDBC Engine if live MySQL is offline
 */
public class DBConnection {
    private static String url = "jdbc:mysql://localhost:3306/vertex_banking?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static String user = "root";
    private static String password = "password";
    private static boolean useSimulated = false;
    private static boolean initialized = false;

    static {
        loadProperties();
    }

    private static synchronized void loadProperties() {
        if (initialized) return;
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("vertex_banking.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                url = props.getProperty("db.url", url);
                user = props.getProperty("db.user", user);
                password = props.getProperty("db.password", password);
            }
        } catch (Exception ignored) {
        }
        initialized = true;
    }

    /**
     * Obtains a standard java.sql.Connection.
     * Tries live MySQL first; if unavailable, uses the embedded simulated engine.
     */
    public static Connection getConnection() throws SQLException {
        if (!useSimulated) {
            try {
                // Try registering MySQL driver if present
                Class.forName("com.mysql.cj.jdbc.Driver");
                Connection conn = DriverManager.getConnection(url, user, password);
                return conn;
            } catch (ClassNotFoundException | SQLException e) {
                // Switch to simulated engine silently or with notice
                useSimulated = true;
            }
        }
        return SimulatedJdbcDriver.createConnection();
    }

    public static boolean isUsingSimulated() {
        return useSimulated;
    }

    public static void setForceSimulated(boolean simulated) {
        useSimulated = simulated;
    }
}
