package org.debian.mavenproxy.request;

import org.apache.http.HttpResponse;
import org.debian.mavenproxy.Artifact;

import java.io.FileNotFoundException;

public interface IRepositoryResponse {
    void writeResponse(HttpResponse response, String fileName, Artifact requestedArt) throws FileNotFoundException;
}
