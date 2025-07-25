package org.debian.mavenproxy.repositories;

import org.debian.mavenproxy.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class LocalRepository extends AbstractRepository {
    private static final Logger logger = LoggerFactory.getLogger(LocalRepository.class);

    public LocalRepository(String localRepositoryBasePath) {
        super(localRepositoryBasePath);
    }

    public void putArtifact(String groupId, String artifactId, String version, String fileName, InputStream is) {
        if (fileName.endsWith(".sha1")) {
            return;
        }
        Path output = Path.of(this.getBase(), groupId.replace(".", "/"), artifactId, version, fileName);
        try (is) {
            byte[] data = is.readAllBytes();
            Files.write(output, data);
            Path sha1 = Path.of(output + ".sha1");
                Files.writeString(sha1, calculateChecksum(data, "SHA1"));
        }
        catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public RepositoryContent getArtifact(String groupId, String artifactId, String version, String fileName) throws FileNotFoundException {
        String ext = fileName.substring(fileName.lastIndexOf(".")+1);
        Path input = Path.of(this.getBase(), groupId.replace(".", "/"), artifactId, version, fileName);
        return new RepositoryContent(new Artifact(groupId, artifactId, version, ext), new FileInputStream(input.toFile()));
    }

    private String calculateChecksum(byte[] data, String algorithm) throws NoSuchAlgorithmException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        MessageDigest md = MessageDigest.getInstance(algorithm);
        DigestInputStream dis = new DigestInputStream(bis, md);
        HexFormat hex = HexFormat.of();
        return hex.formatHex(dis.getMessageDigest().digest()).toLowerCase();
    }
}
