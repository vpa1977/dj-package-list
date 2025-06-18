package org.debian.mavenproxy;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MavenRemoteService {
    private static final Logger logger = LoggerFactory.getLogger(MavenRemoteService.class);
    private final String[] remoteRepositoryUrl;
    private final CloseableHttpClient httpClient;
    private final DbManager dbManager;
    private final RepositoryManager repositoryManager;

    public MavenRemoteService(String[] remoteRepositoryUrl, RepositoryManager repositoryManager, DbManager dbManager) {
        this.remoteRepositoryUrl = Arrays.stream(remoteRepositoryUrl).map(x -> x.endsWith("/") ? x : x + "/").toArray(String[]::new);
        this.httpClient = HttpClients.createDefault(); // Create a default HTTP client for forwarding requests
        this.repositoryManager = repositoryManager;
        this.dbManager = dbManager;
    }

    public void fetchAndServeFromRemote(String relativePath, HttpResponse response) throws IOException {
        if (dbManager.isBlacklisted(relativePath)) {
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            response.setEntity(new org.apache.http.entity.StringEntity("Content not found on remote(blacklist).", StandardCharsets.UTF_8));
            logger.warn("No content found on remote for {}.", relativePath);
            return;
        }
        for (var remoteUrl : remoteRepositoryUrl) {
            if (fetchAndServiceFromRemoteUrl(remoteUrl, relativePath, response)) {
                return;
            }
        }
        dbManager.blacklist(relativePath);
        response.setStatusCode(HttpStatus.SC_NOT_FOUND);
        response.setEntity(new org.apache.http.entity.StringEntity("Content not found on remote.", StandardCharsets.UTF_8));
        logger.warn("No content found on remote for {}.", relativePath);
    }

    public void shutdown() throws IOException {
        if (httpClient != null) {
            httpClient.close();
            logger.info("HTTP client closed.");
        }
    }

    private boolean fetchAndServiceFromRemoteUrl(String remoteUrl, String relativePath, HttpResponse response)  throws IOException {
        HttpGet httpGet = new HttpGet(remoteUrl + relativePath);
        CloseableHttpResponse remoteResponse = null;
        InputStream contentStream = null;

        HttpEntity remoteEntity = null;
        try {
            remoteResponse = httpClient.execute(httpGet);
            int statusCode = remoteResponse.getStatusLine().getStatusCode();
            if (HttpStatus.SC_OK != statusCode) {
                return false;
            }
            response.setStatusCode(statusCode); // Set response status code from remote

            remoteEntity = remoteResponse.getEntity();
            if (remoteEntity == null) {
                return false;
            }

            // Copy headers from remote response to local response
            for (Header header : remoteResponse.getAllHeaders()) {
                // Avoid copying headers that might interfere with local serving, e.g., Transfer-Encoding
                if (!header.getName().equalsIgnoreCase("Transfer-Encoding") &&
                        !header.getName().equalsIgnoreCase("Content-Length")) {
                    response.setHeader(header);
                }
            }

            contentStream = remoteEntity.getContent();

            // If it's an artifact (not a checksum), try to cache it
            if (!remoteUrl.endsWith(".sha1") && !remoteUrl.endsWith(".md5") &&
                    statusCode == HttpStatus.SC_OK) { // Only cache if remote fetch was successful

                // Create a temporary input stream to read content twice: once for saving, once for checksum
                // Or, save it and then calculate checksum from the saved file. The latter is simpler.
                File savedFile = repositoryManager.saveArtifact(relativePath, contentStream);
                if (savedFile != null) {
                    response.setEntity(new org.apache.http.entity.FileEntity(savedFile));
                } else {
                    logger.error("Failed to save artifact from remote: {}", remoteUrl);
                    // If saving failed, we still want to serve the content, but won't cache.
                    // This might require re-reading the stream or just passing through.
                    // For simplicity, if saveArtifact returns null, we'll indicate an internal error.
                    response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    response.setEntity(new org.apache.http.entity.StringEntity("Failed to cache and serve artifact.", StandardCharsets.UTF_8));
                }
                logArtifactId(relativePath, remoteUrl);
            } else {
                // For checksums or if not caching, just stream the remote content directly
                response.setEntity(new org.apache.http.entity.InputStreamEntity(contentStream));
            }
            return true;
        } catch (IOException e) {
            logger.error("Error fetching from remote {}: {}", remoteUrl, e.getMessage(), e);
        } finally {
            if (remoteEntity != null) {
                EntityUtils.consumeQuietly(remoteEntity);
            }
            if (remoteResponse != null) {
                remoteResponse.close(); // Close the remote response to release resources
            }
        }
        return false;
    }

    private void logArtifactId(String requestPath, String remoteUrl) {
        try {
            var art = ArtifactParseUtil.parse(requestPath);
            dbManager.logUniqueArtifact(art.groupId(), art.name(),  art.version(), requestPath, remoteUrl);
        }
        catch (RuntimeException e){
            logger.debug("Request path does not match artifact pattern: {} ", e.getMessage());
        }
    }
}
