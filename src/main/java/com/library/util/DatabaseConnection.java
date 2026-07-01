package com.library.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Central JDBC connection provider. Uses HikariCP pooling (production practice
 * instead of opening/closing raw connections per query) and reads config from
 * db.properties, overridable by environment variables for container/deploy use.
 */
public final class DatabaseConnection {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnection.class);
    private static volatile HikariDataSource dataSource;

    private DatabaseConnection() { }

    private static void init() {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Oracle JDBC Driver not found", e);
        }
        Properties props = new Properties();
        try (InputStream in = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            log.error("Failed to load db.properties", e);
        }

        String url = System.getenv().getOrDefault("DB_URL", props.getProperty("db.url"));
        String user = System.getenv().getOrDefault("DB_USER", props.getProperty("db.user"));
        String password = System.getenv().getOrDefault("DB_PASSWORD", props.getProperty("db.password"));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(Integer.parseInt(
                props.getProperty("db.pool.maxSize", "10")));
        config.setMinimumIdle(Integer.parseInt(
                props.getProperty("db.pool.minIdle", "2")));
        config.setPoolName("LibraryPool");
        // Important for correctness: autoCommit is toggled per-transaction in DAOs
        config.setAutoCommit(true);

        dataSource = new HikariDataSource(config);
        log.info("Database connection pool initialized for {}", url);
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            synchronized (DatabaseConnection.class) {
                if (dataSource == null) {
                    init();
                }
            }
        }
        return dataSource.getConnection();
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("Database connection pool shut down");
        }
    }
}
