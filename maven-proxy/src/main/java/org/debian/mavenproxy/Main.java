package org.debian.mavenproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
/**
 * Main class for the Maven Proxy application.
 * This class parses command-line arguments, initializes the components,
 * and starts the proxy server.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {

        Yaml yaml = new Yaml();
        Map<String, Object> config = yaml.load(new FileReader("config.yaml"));
        List<String> ignoreLines = (List<String>)config.get("ignore");
        List<String> replaceLines = (List<String>)config.get("replace");
        // Default configuration values
        int port = (Integer)config.get("port");
        boolean mapArtifacts = (Boolean)config.get("map-artifacts");

        String localRepoPath = "local-maven-proxy-cache";
        String debianRepoPath = "debian-repo";

        String dbFilePath = "maven_proxy.db";
        // will fail to copy over the existing file
       // Files.copy(Paths.get("artifacts.db"), Paths.get(dbFilePath));
        String[] remoteRepoUrls = new String[] {
            "https://repo.maven.apache.org/maven2/",
            "https://dl.google.com/dl/android/maven2/",
            "https://repo.gradle.org/gradle/repo",
            "https://plugins.gradle.org/m2/",
            "https://repo.gradle.org/gradle/public",
            "https://repo.gradle.org/gradle/javascript-public",
            "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies/"
        };

        // Validate local repository path
        Path repoPath = Paths.get(localRepoPath);
        if (Files.exists(repoPath) && !Files.isDirectory(repoPath)) {
            logger.error("Local repository path exists but is not a directory: {}", localRepoPath);
            System.exit(1);
        }
        repoPath = Paths.get(debianRepoPath);
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
            ArtifactMapper mapper = new ArtifactMapper(mapArtifacts, dbManager, replaceLines, ignoreLines);
            RepositoryManager repositoryManager = new RepositoryManager(localRepoPath, debianRepoPath, mapper);

            MavenRemoteService mavenRemoteService = new MavenRemoteService(remoteRepoUrls, repositoryManager, dbManager);
            // Initialize and start ProxyServer
            ProxyServer proxyServer = new ProxyServer(repositoryManager, mavenRemoteService, port);
            proxyServer.start();

            logger.info("Maven Proxy Server is running. Press Ctrl+C to stop.");
        } catch (IOException e) {
            logger.error("Failed to start Maven Proxy Server: {}", e.getMessage(), e);
            dbManager.close();
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
