package org.debian.mavenproxy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Translates requested artifact to Debian coordiantes
 */
public class ArtifactMapper {
    private final boolean map;


    public ArtifactMapper(boolean map) throws IOException {
        this.map = map;
    }

    public RemappedArtifact mapRequestPath(String path) {
        ArtifactParseUtil.Artifact art = ArtifactParseUtil.parse(path);
        if (!map) {
            return new RemappedArtifact(art, path, path);
        }

        return new RemappedArtifact(art, path, path);
    }

}
