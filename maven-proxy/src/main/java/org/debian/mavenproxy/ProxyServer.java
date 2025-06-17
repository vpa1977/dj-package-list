package org.debian.mavenproxy;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ProxyServer implements HttpRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);
    private final RepositoryManager repositoryManager;
    private final int port;
    private final MavenRemoteService remoteService;
    private HttpServer server;
    private final ContentTypes contentTypes = new ContentTypes();

    public ProxyServer(RepositoryManager repositoryManager, MavenRemoteService remoteService, int port) {
        this.repositoryManager = repositoryManager;
        // Ensure remoteRepositoryUrl ends with a slash for consistent path concatenation
        this.port = port;
        this.remoteService = remoteService;
    }

    public void start() throws IOException {
        server = ServerBootstrap.bootstrap()
            .setListenerPort(port)
            .setHttpProcessor(null) // Use default HTTP processor
            .registerHandler("*", this) // Register this class as the handler for all paths
            .create();

        server.start();
        logger.info("Maven Proxy Server started on port {}", port);

        // Add a shutdown hook to gracefully close the server and database connection
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutdown();
            } catch (IOException e) {
                logger.error("Error during server shutdown hook: {}", e.getMessage(), e);
            }
        }));
    }

    public void shutdown() throws IOException {
        if (server != null) {
            server.shutdown(5, TimeUnit.SECONDS); // Allow 5 seconds for graceful shutdown
            logger.info("Maven Proxy Server shutting down.");
        }
        remoteService.shutdown();
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws IOException {
        if (!request.getRequestLine().getMethod().equalsIgnoreCase("GET")) {
            response.setStatusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
            return;
        }

        String requestPath = request.getRequestLine().getUri();
        logger.debug("Received request for: {}", requestPath);
        File artifactFile = repositoryManager.getArtifact(requestPath);
        if (artifactFile != null) {
            logger.info("Serving artifact from local cache: {}", requestPath);
            response.setStatusCode(HttpStatus.SC_OK);
            String contentType = contentTypes.determineContentType(requestPath);
            response.setHeader("Content-Type", contentType);
            response.setEntity(new org.apache.http.entity.FileEntity(artifactFile));
            return;
        }

        logger.info("Artifact not found locally, attempting to fetch from remote: {}", requestPath);
        remoteService.fetchAndServeFromRemote(requestPath, response);
    }
}
