package database;

import java.io.File;
import java.io.FileInputStream;
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
    private static String password = "root";
    private static boolean useSimulated = false;
    private static boolean initialized = false;
    private static String lastConnectionError = null;
    private static boolean fallbackNotified = false;

    static {
        loadProperties();
    }

    private static synchronized void loadProperties() {
        if (initialized) return;
        Properties props = new Properties();
        boolean loaded = false;

        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("vertex_banking.properties")) {
            if (in != null) {
                props.load(in);
                loaded = true;
            }
        } catch (Exception ignored) {
        }

        if (!loaded) {
            File fallbackFile = new File("src/vertex_banking.properties");
            if (fallbackFile.exists()) {
                try (InputStream in = new FileInputStream(fallbackFile)) {
                    props.load(in);
                } catch (Exception ignored) {
                }
            }
        }

        url = props.getProperty("db.url", url);
        user = props.getProperty("db.user", user);
        password = props.getProperty("db.password", password);

        String envUrl = System.getenv("VERTEX_DB_URL");
        String envUser = System.getenv("VERTEX_DB_USER");
        String envPass = System.getenv("VERTEX_DB_PASSWORD");
        if (envUrl != null && !envUrl.isEmpty()) url = envUrl;
        if (envUser != null && !envUser.isEmpty()) user = envUser;
        if (envPass != null) password = envPass;

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
                lastConnectionError = null;
                return conn;
            } catch (ClassNotFoundException | SQLException e) {
                lastConnectionError = e.getMessage();
                useSimulated = true;
                if (!fallbackNotified) {
                    fallbackNotified = true;
                    System.err.println("[DBConnection] MySQL unavailable (" + e.getMessage() + "). Falling back to SimulatedJdbcDriver.");
                }
            }
        }
        return SimulatedJdbcDriver.createConnection();
    }

    public static boolean isUsingSimulated() {
        return useSimulated;
    }

    public static void setForceSimulated(boolean simulated) {
        useSimulated = simulated;
        if (!simulated) {
            fallbackNotified = false;
            lastConnectionError = null;
        }
    }

    public static String getLastConnectionError() {
        return lastConnectionError;
    }

    public static String getConnectionStatus() {
        return useSimulated ? "Simulated Embedded JDBC Engine" : "MySQL (" + url + ")";
    }
}
