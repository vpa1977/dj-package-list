package org.debian.mavenproxy.repositories;

import org.debian.mavenproxy.Artifact;

import java.io.InputStream;

public class RepositoryContent {
    private final Artifact foundArtifact;
    private final InputStream is;

    public RepositoryContent(Artifact foundArtifact, InputStream is) {
        this.foundArtifact = foundArtifact;
        this.is = is;
    }

    public InputStream getInputStream() {
        return this.is;
    }

    public Artifact getFoundArtifact() {
        return foundArtifact;
    }
}
