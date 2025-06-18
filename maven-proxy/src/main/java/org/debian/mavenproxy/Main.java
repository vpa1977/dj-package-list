package org.debian.mavenproxy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for the Maven Proxy application.
 * This class parses command-line arguments, initializes the components,
 * and starts the proxy server.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // Default configuration values
        int port = 8080;
        String localRepoPath = "local-maven-proxy-cache";
        String dbFilePath = "maven_proxy.db";
        String[] remoteRepoUrls = new String[] {
            "https://repo.maven.apache.org/maven2/",
            "https://plugins.gradle.org/m2/",
            "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies/"
        };

        // Validate local repository path
        Path repoPath = Paths.get(localRepoPath);
        if (Files.exists(repoPath) && !Files.isDirectory(repoPath)) {
            logger.error("Local repository path exists but is not a directory: {}", localRepoPath);
            System.exit(1);
        }

        logger.info("Starting Maven Proxy with configurations:");
        logger.info("  Port: {}", port);
        logger.info("  Local Repository Path: {}", repoPath.toAbsolutePath());
        logger.info("  Database File: {}", Paths.get(dbFilePath).toAbsolutePath());

        DbManager dbManager = null;
        try {
            // Initialize DbManager
            dbManager = new DbManager(dbFilePath);
            dbManager.initialize();

            // Initialize RepositoryManager
            RepositoryManager repositoryManager = new RepositoryManager(localRepoPath, "/usr/share/maven-repo");

            MavenRemoteService mavenRemoteService = new MavenRemoteService(remoteRepoUrls, repositoryManager, dbManager);
            // Initialize and start ProxyServer
            ProxyServer proxyServer = new ProxyServer(repositoryManager, mavenRemoteService, port);
            proxyServer.start();

            logger.info("Maven Proxy Server is running. Press Ctrl+C to stop.");
        } catch (IOException e) {
            logger.error("Failed to start Maven Proxy Server: {}", e.getMessage(), e);
            if (dbManager != null) {
                dbManager.close();
            }
            System.exit(1);
        } catch (RuntimeException e) {
            logger.error("Application error during initialization: {}", e.getMessage(), e);
            if (dbManager != null) {
                dbManager.close();
            }
            System.exit(1);
        }
    }
}
