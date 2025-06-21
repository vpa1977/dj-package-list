package org.debian.mavenproxy;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

/**
 * Translates requested artifact to Debian coordiantes
 */
public class ArtifactMapper {
    private final boolean map;
    private HashMap<ArtifactParseUtil.Artifact, File> mappedFiles = new HashMap<>();

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

    public File mapFile(ArtifactParseUtil.Artifact art, File sourceFile)  {
        if (!map) {
            return sourceFile;
        }
        if (!"pom".equals(art.type())) {
            return sourceFile;
        }
        File mapped = mappedFiles.get(art);
        if (mapped != null) {
            return mapped;
        }
        try {
            File newFile = Files.createTempFile("temp", ".pom").toFile();
            newFile.deleteOnExit();
            POMMapper.mapPom(sourceFile, newFile, art.groupId(), art.name(), art.version());
            mappedFiles.put(art, newFile);
            return newFile;
        } catch (ParserConfigurationException | TransformerException | SAXException | IOException e) {
            throw new RuntimeException("Mapping failed " + e.getMessage());
        }
    }

}
