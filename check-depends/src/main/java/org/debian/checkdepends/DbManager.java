package org.debian.checkdepends;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DbManager {

    private static final Logger logger = LoggerFactory.getLogger(DbManager.class);
    private Connection connection;

    public DbManager() {
        initialize();
    }

    private  void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite::resource:artifacts.db");
            connection.setAutoCommit(false);

            logger.info("SQLite database initialized");
        } catch (ClassNotFoundException e) {
            logger.error("SQLite JDBC driver not found: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load SQLite JDBC driver", e);
        } catch (SQLException e) {
            logger.error("Error connecting to database or creating tables: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public List<Artifact> searchArtifacts(String groupId, String artifactId) {
            String sql = "SELECT group_id, artifact_id, version, package_name, package_version " +
                        "FROM imported_artifacts " +
                        "WHERE group_id LIKE ? AND artifact_id LIKE ?";

            List<Artifact> artifacts = new ArrayList<>();

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

                // Set parameters with wildcards
                pstmt.setString(1, (groupId == null ? "" : groupId) + "%");
                pstmt.setString(2, (artifactId == null ? "" : artifactId) + "%");

                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    Artifact artifact = new Artifact(
                            rs.getString("group_id"),
                            rs.getString("artifact_id"),
                            rs.getString("version"),
                            rs.getString("package_name"),
                            rs.getString("package_version").replace("%3a", ":") // fixup import artifact (: encoding)
                    );
                    artifacts.add(artifact);
                }
            } catch (SQLException e) {
                System.err.println("Error during artifact search: " + e.getMessage());
            }
            return artifacts;
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
