package org.debian.mavenproxy.request;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.debian.mavenproxy.Artifact;
import org.debian.mavenproxy.ArtifactParseUtil;
import org.debian.mavenproxy.repositories.DebianRepository;
import org.debian.mavenproxy.repositories.LocalRepository;
import org.debian.mavenproxy.repositories.RemoteRepository;
import org.debian.mavenproxy.repositories.RepositoryContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RepositoryManager {
    private static final Logger logger = LoggerFactory.getLogger(RepositoryManager.class);

    private final LocalRepository localRepository;
    private final DebianRepository debianRepository;
    private final List<RemoteRepository> remoteRepositories;

    public RepositoryManager(String localrepo, String debianRepo, List<String> remotes, List<String> ignoreRules, List<String> replaceRules) {
        localRepository = new LocalRepository(localrepo);
        if (debianRepo != null) {
            debianRepository = new DebianRepository(debianRepo, ignoreRules, replaceRules);
        } else {
            debianRepository = null;
        }

        remoteRepositories = new ArrayList<>();
        for (var remote : remotes) {
            remoteRepositories.add(new RemoteRepository(remote));
        }
    }

    public void handleRequest(String relativePath, HttpResponse response, IRepositoryResponse action) {
        String fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
        Artifact requestedArt = ArtifactParseUtil.parse(relativePath);

        if (checkLocalRepository(response, action, requestedArt, fileName)) {
            return;
        }
        if (checkDebianRepository(response, action, requestedArt, fileName))  {
            return;
        }

        if (checkRemoteRepositories(response, action, requestedArt, fileName)) {
            return;
        }

        response.setStatusCode(HttpStatus.SC_NOT_FOUND);
    }

    private static boolean checkLocalRepository(HttpResponse response, IRepositoryResponse action, Artifact requestedArt, String fileName) {
        try {
            action.writeResponse(response, fileName, requestedArt);
            return true;
        } catch (FileNotFoundException e) {
            // ignore
        }
        return false;
    }

    private boolean checkRemoteRepositories(HttpResponse response, IRepositoryResponse action, Artifact requestedArt, String fileName) {
        for (var repository : remoteRepositories) {
            try {
                RepositoryContent art = repository.getArtifact(requestedArt.groupId(), requestedArt.name(), requestedArt.version(), fileName);
                try (InputStream is = art.getInputStream()) {
                    localRepository.putArtifact(requestedArt.groupId(), requestedArt.name(), requestedArt.version(), fileName, is);
                } catch (IOException e) {
                    logger.error("error saving artifact", e );
                    response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                    return true;
                }
                action.writeResponse(response, fileName, requestedArt);
                return true;
            } catch (FileNotFoundException e) {
                // ignore
            }
        }
        return false;
    }

    private boolean checkDebianRepository(HttpResponse response, IRepositoryResponse action, Artifact requestedArt, String fileName) {
        try {
            RepositoryContent art = debianRepository.getArtifact(requestedArt.groupId(), requestedArt.name(), requestedArt.version(), fileName);
            try (InputStream is = art.getInputStream()) {
                localRepository.putArtifact(requestedArt.groupId(), requestedArt.name(), requestedArt.version(), fileName, is);
            } catch (IOException e) {
                logger.error("error saving artifact", e );
                response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                return true;
            }
            action.writeResponse(response, fileName, requestedArt);
            return true;
        } catch (FileNotFoundException e) {
            // ignore
        }
        return false;
    }

    public LocalRepository getLocalRepository() {
        return localRepository;
    }
}
