package org.debian.mavenproxy.repositories;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractRepository {
    private final String base;

    public AbstractRepository(String base) {
        this.base = base;
    }

    public abstract RepositoryContent getArtifact(String groupId, String artifactId, String version, String fileName) throws FileNotFoundException;

    public int getArtifactSize(String groupId, String artifactId, String version, String fileName) throws FileNotFoundException {
        try {
            return getArtifact(groupId, artifactId, version, fileName).getInputStream().readAllBytes().length;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getBase() {
        return this.base;
    }
}
