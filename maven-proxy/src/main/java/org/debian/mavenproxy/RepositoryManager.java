package org.debian.mavenproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class RepositoryManager {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryManager.class);
    private final Path localRepositoryPath;
    private final Path debianPath;

    public RepositoryManager(String localRepositoryBasePath, String debianPath) throws IOException {
        this.localRepositoryPath = Paths.get(localRepositoryBasePath).toAbsolutePath().normalize();
        this.debianPath = Paths.get(debianPath).toAbsolutePath().normalize();
        if (!Files.exists(this.localRepositoryPath)) {
            Files.createDirectories(this.localRepositoryPath);
            logger.info("Created local repository directory: {}", this.localRepositoryPath);
        } else {
            logger.info("Using existing local repository directory: {}", this.localRepositoryPath);
        }
        if (!Files.exists(this.debianPath)) {
            logger.error("Debian maven repo path does not exist : {}", this.debianPath);
        }
        initializeChecksums();
    }

    private void initializeChecksums() throws IOException {
        try (var files = Files.walk(this.debianPath)) {
            files.filter(Files::isRegularFile)
                    .filter(x -> !x.toString().endsWith(".sha1"))
                    .forEach(x -> {
                        var relative = debianPath.relativize(x);
                        var destination = localRepositoryPath.resolve(relative);
                        try {
                            if (!Files.exists(destination.getParent())) {
                                Files.createDirectories(destination.getParent());
                            }
                            var sha1File = Paths.get(destination + ".sha1");
                            if (!Files.exists(sha1File)) {
                                try {
                                    byte[] data = Files.readAllBytes(x);
                                    String sha1 = calculateChecksum(data, "SHA1");
                                    Files.writeString(sha1File, sha1);
                                }
                                catch (AccessDeniedException e ){
                                    logger.error("Access denied: {}",e.getMessage()); // ignore the exception if the access is denied
                                }
                            }
                        } catch (IOException | NoSuchAlgorithmException e) {
                            throw new RuntimeException(e);
                        }
                    });

        }
    }

    public File getArtifact(String relativePath) {
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        Path filePath = localRepositoryPath.resolve(relativePath).normalize();
        if (!filePath.startsWith(localRepositoryPath)) {
            logger.warn("Attempted path traversal detected for retrieval: {}", relativePath);
            return null;
        }
        File file = filePath.toFile();
        if (file.exists()) {
            return file;
        }

        filePath = debianPath.resolve(relativePath).normalize();
        file = filePath.toFile();
        return file.exists() ? file : null;
    }

    public File saveArtifact(String relativePath, InputStream inputStream) {
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        Path filePath = localRepositoryPath.resolve(relativePath).normalize();
        if (!filePath.startsWith(localRepositoryPath)) {
            logger.error("Attempted path traversal detected for saving: {}", relativePath);
            return null;
        }

        try {
            Files.createDirectories(filePath.getParent());
            try (FileOutputStream outputStream = new FileOutputStream(filePath.toFile());
                 FileOutputStream digest = new FileOutputStream(filePath + ".sha1")) {
                byte[] fileData = inputStream.readAllBytes();
                String sha1 = calculateChecksum(fileData, "SHA1");
                digest.write(sha1.getBytes(StandardCharsets.UTF_8));
                outputStream.write(fileData);
            } catch (NoSuchAlgorithmException e) {
                logger.debug("Artifact sha1 - algorithm not found: {}", e.getMessage());
                return null;
            }

            logger.debug("Artifact saved: {}", filePath);
            return filePath.toFile();
        } catch (IOException e) {
            logger.error("Error saving artifact {}: {}", relativePath, e.getMessage(), e);
            return null;
        }
    }

    private String calculateChecksum(byte[] data, String algorithm) throws NoSuchAlgorithmException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        MessageDigest md = MessageDigest.getInstance(algorithm);
        DigestInputStream dis = new DigestInputStream(bis, md);
        HexFormat hex = HexFormat.of();
        return hex.formatHex(dis.getMessageDigest().digest()).toLowerCase();
    }
}
