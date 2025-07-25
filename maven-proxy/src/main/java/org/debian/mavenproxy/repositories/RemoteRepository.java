package org.debian.mavenproxy.repositories;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.debian.mavenproxy.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class RemoteRepository extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(RemoteRepository.class);
    private final CloseableHttpClient httpClient;

    public RemoteRepository(String base) {
        super(base);
        this.httpClient = HttpClients.createDefault(); // Create a default HTTP client for forwarding requests

    }

    @Override
    public int getArtifactSize(String groupId, String artifactId, String version, String fileName) throws FileNotFoundException {
        if (fileName.endsWith(".sha1")) {
            throw new RuntimeException("SHA1 should be requested from local repository");
        }
        String path = getBase() + "/" + groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + fileName;
        HttpHead head = new HttpHead(path);
        CloseableHttpResponse remoteResponse = null;
        try {
            remoteResponse = httpClient.execute(head);
            int statusCode = remoteResponse.getStatusLine().getStatusCode();
            if (HttpStatus.SC_OK != statusCode) {
                throw new FileNotFoundException("not found at " + path);
            }
            return Integer.parseInt(remoteResponse.getFirstHeader("Content-Length").getValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RepositoryContent getArtifact(String groupId, String artifactId, String version, String fileName) throws FileNotFoundException {
        if (fileName.endsWith(".sha1")) {
            throw new RuntimeException("SHA1 should be requested from local repository");
        }
        String path = getBase() + "/" + groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + fileName;
        HttpGet httpGet = new HttpGet(path);
        HttpEntity remoteEntity = null;
        try {
            var remoteResponse = httpClient.execute(httpGet);
            int statusCode = remoteResponse.getStatusLine().getStatusCode();
            if (HttpStatus.SC_OK != statusCode) {
                throw new FileNotFoundException("not found at " + path);
            }

            remoteEntity = remoteResponse.getEntity();
            if (remoteEntity == null) {
                throw new FileNotFoundException("not found at " + path);
            }
            String ext = fileName.substring(fileName.lastIndexOf(".")+1);

            return new RepositoryContent(new Artifact(groupId, artifactId, version, ext),  remoteEntity.getContent());
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }
}
