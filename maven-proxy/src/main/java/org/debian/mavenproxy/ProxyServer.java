package org.debian.mavenproxy;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.debian.mavenproxy.request.GetRepositoryResponse;
import org.debian.mavenproxy.request.HeadRepositoryResponse;
import org.debian.mavenproxy.request.RepositoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ProxyServer implements HttpRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);
    private final int port;
    private final RepositoryManager repositoryManager;
    private HttpServer server;
    private HeadRepositoryResponse headRepositoryResponse;
    private GetRepositoryResponse getRepositoryResponse;

    public ProxyServer(RepositoryManager repositoryManager, int port) {
        this.repositoryManager = repositoryManager;
        // Ensure remoteRepositoryUrl ends with a slash for consistent path concatenation
        this.port = port;
        this.headRepositoryResponse = new HeadRepositoryResponse(repositoryManager.getLocalRepository());
        this.getRepositoryResponse = new GetRepositoryResponse(repositoryManager.getLocalRepository());
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
            server = null;
        }
    }

    @Override
    public synchronized void handle(HttpRequest request, HttpResponse response, HttpContext context) throws IOException {
        String requestPath = request.getRequestLine().getUri();
        if (request.getRequestLine().getMethod().equalsIgnoreCase("HEAD")) {
            repositoryManager.handleRequest(requestPath, response, headRepositoryResponse);
            return;
        }

        if (!request.getRequestLine().getMethod().equalsIgnoreCase("GET")) {
            response.setStatusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
            return;
        }
        repositoryManager.handleRequest(requestPath, response, getRepositoryResponse);
    }
}
