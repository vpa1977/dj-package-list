package org.debian.mavenproxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RepositoryManagerTest {

    @TempDir
    private File testDir;
    @TempDir
    private File debianDir;

    @Test
    public void testRepositoryManager() throws IOException {
        /*
        String data = "test data";
        debianDir.mkdirs();
        Path bar1 = Path.of(debianDir.getAbsolutePath(), "foo/bar1");
        Files.createDirectories(bar1);
        Files.writeString(bar1.resolve("file.txt"), data);
        ArtifactMapper mapper = new ArtifactMapper(false, null, null, null);
        OldRepositoryManager rm = new OldRepositoryManager(testDir.getAbsolutePath(), debianDir.getAbsolutePath(), mapper);
        ByteArrayInputStream bis = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        rm.saveArtifact("/foo/bar/artifact.jar", bis);
        String savedData = Files.readString(testDir.toPath().resolve("foo/bar/artifact.jar"));
        assertEquals(data, savedData);
        String savedDataSha1 = Files.readString(testDir.toPath().resolve("foo/bar/artifact.jar.sha1"));
        assertEquals(40, savedDataSha1.length());

        Path dest = Path.of(testDir.getAbsolutePath(), "foo/bar1").normalize();
        String debianSha1 = Files.readString(dest.resolve("file.txt.sha1"));
        assertEquals(savedDataSha1, debianSha1);
*/
    }

}
