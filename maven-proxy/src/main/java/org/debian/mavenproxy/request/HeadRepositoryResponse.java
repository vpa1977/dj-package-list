package org.debian.mavenproxy.request;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.debian.mavenproxy.Artifact;
import org.debian.mavenproxy.ContentTypes;
import org.debian.mavenproxy.repositories.AbstractRepository;

import java.io.FileNotFoundException;

public class HeadRepositoryResponse implements IRepositoryResponse {
    private final AbstractRepository source;
    private ContentTypes contentTypes;

    public HeadRepositoryResponse(AbstractRepository source) {
        this.source = source;
    }
    @Override
    public void writeResponse(HttpResponse response, String fileName, Artifact requestedArt) throws FileNotFoundException {
        int artifactSize =  source.getArtifactSize(requestedArt.groupId(), requestedArt.name(), requestedArt.version(), fileName);
        String contentType = contentTypes.determineContentType(fileName);
        response.setStatusCode(HttpStatus.SC_OK);
        response.setHeader("Content-Type", contentType);
        response.setHeader("Content-Length", "" + artifactSize);
    }
}
