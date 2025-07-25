package org.debian.mavenproxy.request;

import org.apache.http.HttpResponse;
import org.debian.mavenproxy.Artifact;
import org.debian.mavenproxy.ArtifactParseUtil;
import org.debian.mavenproxy.ContentTypes;
import org.debian.mavenproxy.repositories.DebianRepository;
import org.debian.mavenproxy.repositories.LocalRepository;
import org.debian.mavenproxy.repositories.RemoteRepository;
import org.debian.mavenproxy.repositories.RepositoryContent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RepositoryManager {
    private final ContentTypes contentTypes = new ContentTypes();
    private final LocalRepository localRepository;
    private final DebianRepository debianRepository;
    private final List<RemoteRepository> remoteRepositories;

    public RepositoryManager(String localrepo, String debianRepo, List<String> remotes, List<String> ignoreRules, List<String> replaceRules) {
        localRepository = new LocalRepository(localrepo);
        debianRepository = new DebianRepository(debianRepo, ignoreRules, replaceRules);
        remoteRepositories = new ArrayList<>();
        for (var remote : remotes) {
            remoteRepositories.add(new RemoteRepository(remote));
        }
    }

    public void handleRequest(String relativePath, HttpResponse response, IRepositoryResponse action) {
        String fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
        Artifact requestedArt = ArtifactParseUtil.parse(relativePath);
        try {
            action.writeResponse(response, fileName, requestedArt);
        } catch (FileNotFoundException e) {
            // ignore
        }
        try {
            RepositoryContent art = debianRepository.getArtifact(requestedArt.groupId(), requestedArt.name(), requestedArt.version(), fileName);
            try (InputStream is = art.getInputStream()) {
                localRepository.putArtifact(requestedArt.groupId(), requestedArt.name(), requestedArt.version(), fileName, is);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            action.writeResponse(response, fileName, requestedArt);
        } catch (FileNotFoundException e) {
            // ignore
        }
        for (var repository : remoteRepositories) {
            try {
                RepositoryContent art = repository.getArtifact(requestedArt.groupId(), requestedArt.name(), requestedArt.version(), fileName);
                try (InputStream is = art.getInputStream()) {
                    localRepository.putArtifact(requestedArt.groupId(), requestedArt.name(), requestedArt.version(), fileName, is);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                action.writeResponse(response, fileName, requestedArt);
            } catch (FileNotFoundException e) {
                // ignore
            }
        }
    }
}
