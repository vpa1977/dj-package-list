package org.debian.mavenproxy.integration;

import org.apache.http.HttpResponse;
import org.debian.mavenproxy.ArtifactMapper;
import org.debian.mavenproxy.DbManager;
import org.debian.mavenproxy.MavenRemoteService;
import org.debian.mavenproxy.OldRepositoryManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.mockito.Mockito;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RemoteFetchIntegrationTest {

    @TempDir
    private File testFile;

    @TempDir
    private File localRepo;

    @TempDir
    private File debianRepo;

    @Test
    public void testRemoteFetch() throws IOException, SQLException {
        String[] remoteRepoUrls = new String[] {
                "https://repo.maven.apache.org/maven2/",
                "https://plugins.gradle.org/m2/"
        };
        DbManager dbManager = new DbManager(testFile +"/test.db");
        dbManager.initialize();
        ArtifactMapper mapper = new ArtifactMapper(false, null, null, null);
        OldRepositoryManager repoManager = new OldRepositoryManager(localRepo.getAbsolutePath(), debianRepo.getAbsolutePath(), mapper);
        MavenRemoteService rp = new MavenRemoteService(remoteRepoUrls, repoManager, dbManager);
        HttpResponse mockResponse = Mockito.mock(org.apache.http.HttpResponse.class);
        rp.fetchAndServeFromRemote("/CodeLineCounter/lineCounterPlugin/1.1.4/lineCounterPlugin-1.1.4.pom", mockResponse);
        assertTrue(dbManager.hasArtifact("CodeLineCounter",
                "lineCounterPlugin",
                "1.1.4",
                "/CodeLineCounter/lineCounterPlugin/1.1.4/lineCounterPlugin-1.1.4.pom",
                "https://plugins.gradle.org/m2/"));
        File artifactFile = repoManager.getArtifact("/CodeLineCounter/lineCounterPlugin/1.1.4/lineCounterPlugin-1.1.4.pom");
        assertTrue(artifactFile.exists());
    }
}
