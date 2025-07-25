package org.debian.mavenproxy.request;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ByteArrayEntity;
import org.debian.mavenproxy.Artifact;
import org.debian.mavenproxy.repositories.AbstractRepository;
import org.debian.mavenproxy.repositories.RepositoryContent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class GetRepositoryResponse implements IRepositoryResponse {
    private final AbstractRepository source;
    private ContentTypes contentTypes;

    public GetRepositoryResponse(AbstractRepository source) {
        this.source = source;
    }
    @Override
    public void writeResponse(HttpResponse response, String fileName, Artifact requestedArt) throws FileNotFoundException {
        RepositoryContent content =  source.getArtifact(requestedArt.groupId(), requestedArt.name(), requestedArt.version(), fileName);
        byte[] data;
        try (InputStream is = content.getInputStream()) {
            data = is.readAllBytes();
            String contentType = contentTypes.determineContentType(fileName);
            response.setStatusCode(HttpStatus.SC_OK);
            response.setHeader("Content-Type", contentType);
            response.setHeader("Content-Length", "" + data.length);
            response.setEntity(new ByteArrayEntity(data));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
