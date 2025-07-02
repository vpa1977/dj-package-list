package org.debian.mavenproxy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages interactions with the SQLite database for logging and caching metadata.
 * It handles connection, table creation, and insertion of log data.
 */
public class DbManager {

    private static final Logger logger = LoggerFactory.getLogger(DbManager.class);
    private final String dbPath;
    private Connection connection;

    /**
     * Constructs a DbManager with the specified database file path.
     *
     * @param dbPath The file path for the SQLite database.
     */
    public DbManager(String dbPath) {
        this.dbPath = dbPath;
    }

    /**
     * Initializes the database connection and creates necessary tables if they don't exist.
     */
    public void initialize() {
        try {
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            // Establish a connection to the database
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
            // Table for logging unique artifacts requested
            stmt.execute("CREATE TABLE IF NOT EXISTS blacklist (" +
                        "url TEXT," +
                        "PRIMARY KEY(url)"+
                    ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS remote_artifacts (" +
                         "groupId TEXT," +
                         "artifactId TEXT," +
                         "version TEXT," +
                         "requestUrl TEXT," + // e.g., jar, pom, war, etc.
                         "remoteUrl TEXT NOT NULL," + // Full URL of the artifact
                         "PRIMARY KEY(groupId, artifactId, version)" + // Full URL of the artifact
                         ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS artifacts_version_map (" +
                    "groupId TEXT," +
                    "artifactId TEXT," +
                    "version TEXT," +
                    "foundVersion" +
                    "PRIMARY KEY(groupId, artifactId, version)" +
                    ")");
        }
    }

    public boolean isBlacklisted(String url)  {
        String sql = "SELECT COUNT(*) FROM blacklist WHERE url = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, url);
            try (ResultSet q = pstmt.executeQuery()) {
                return q.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.warn("Error quering blacklist for {}, {}", url, e.getMessage());
        }
        return true;
    }

    public void blacklist(String url)  {
        String sql = "INSERT INTO blacklist(url) VALUES(?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, url);
            pstmt.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            logger.warn("Error updating blacklist for {}, {}", url, e.getMessage());
        }
    }

    public void logUniqueArtifact(String groupId, String artifactId, String version, String requestUrl, String remoteUrl) {
        // Use INSERT OR IGNORE to only log unique URLs, preventing duplicates
        String sql = "INSERT INTO remote_artifacts(groupId, artifactId, version, requestUrl, remoteUrl) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, groupId);
            pstmt.setString(2, artifactId);
            pstmt.setString(3, version);
            pstmt.setString(4, requestUrl);
            pstmt.setString(5, remoteUrl);
            pstmt.executeUpdate();
            connection.commit();
            logger.info("Logged unique artifact: {} ({} {} {})", remoteUrl, groupId, artifactId, version);
        } catch (SQLException e) {
            logger.error("Error logging unique artifact for {}: {}", requestUrl, e.getMessage());
        }

    }

    public boolean hasArtifact(String groupId, String artifactId, String version, String requestUrl, String remoteUrl) throws SQLException {
        String sql = "Select COUNT(*) FROM remote_artifacts WHERE groupId=? AND artifactId=? AND version=? AND requestUrl=? AND remoteUrl=? ";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, groupId);
            pstmt.setString(2, artifactId);
            pstmt.setString(3, version);
            pstmt.setString(4, requestUrl);
            pstmt.setString(5, remoteUrl);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
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

    public List<String> findVersion(String groupId, String artifactId) throws SQLException {
        String sql = "SELECT VERSION from imported_artifacts where group_id = ? and artifact_id = ?";
        ArrayList<String> versions = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, groupId);
            pstmt.setString(2, artifactId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                versions.add(rs.getString(1));
            }
        }
        return versions;
    }

    public void addMapping(String groupId, String artifactId, String origVersion, String version) throws SQLException  {
        String sql = "INSERT INTO artifacts_version_map (artifactId, groupId, version, foundVersion) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, groupId);
            pstmt.setString(2, artifactId);
            pstmt.setString(3, origVersion);
            pstmt.setString(4, version);
        }
    }
}
