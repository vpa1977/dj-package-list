package org.debian.javapackage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbManager {

    private static final Logger logger = LoggerFactory.getLogger(DbManager.class);
    private final String dbPath;
    private Connection connection;

    public DbManager(String dbPath) {
        this.dbPath = dbPath;
    }

    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            connection.setAutoCommit(false);

            createTables();
            logger.info("SQLite database initialized at: {}", dbPath);
        } catch (ClassNotFoundException e) {
            logger.error("SQLite JDBC driver not found: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load SQLite JDBC driver", e);
        } catch (SQLException e) {
            logger.error("Error connecting to database or creating tables: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS imported_artifacts (
                        group_id TEXT NOT NULL,
                        artifact_id TEXT NOT NULL,
                        version TEXT NOT NULL,
                        package_name TEXT NOT NULL,
                        package_version TEXT NOT NULL,
                        PRIMARY KEY (group_id, artifact_id, version, package_name)
                    )
                    """);

        }
    }

    public void insertArtifact(String groupId, String artifactId, String version,
            String packageName, String packageVersion) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO imported_artifacts (group_id, artifact_id, version, package_name, package_version) VALUES (?,?,?,?,?)")) {
            stmt.setString(1, groupId);
            stmt.setString(2, artifactId);
            stmt.setString(3, version);
            stmt.setString(4, packageName);
            stmt.setString(5, packageVersion);
            stmt.execute();
        }
        connection.commit();
    }

    public void commit() {
        if (connection != null) {
            try {
                connection.commit();
            } catch (SQLException e) {
                logger.error("Error closing database connection: {}", e.getMessage(), e);
            }
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("SQLite database connection closed.");
            } catch (SQLException e) {
                logger.error("Error closing database connection: {}", e.getMessage(), e);
            }
        }
    }
}
