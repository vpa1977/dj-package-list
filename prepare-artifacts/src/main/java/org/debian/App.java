package org.debian;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.debian.javapackage.DbManager;
import org.debian.javapackage.dependency.SourceListParser;


public class App
{
    private static void runCommand(File workDir, String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workDir);
        pb.inheritIO();
        int code = pb.start().waitFor();
        if (code != 0) {
            StringBuilder msg = new StringBuilder();
            for (var cmd : command) {
                msg.append(cmd);
                msg.append(" ");
            }
            throw new RuntimeException("Failed to run "+ msg.toString());
        }

    }

    private static void refreshAptSources(File workDir, Path tempAptDir, Path tempSources) throws IOException, InterruptedException {
        String sourceList = "Dir::Etc::SourceList=" + tempSources;
        String sourceListDir = "Dir::Etc::SourceParts=" + tempAptDir;
        String listsDir = "Dir::State::Lists=" + tempAptDir.resolve("lists");
        Files.createDirectories(tempAptDir.resolve("lists").resolve("partial"));
        runCommand(workDir, "apt-get", "update",
                "-o", sourceList,
                "-o", sourceListDir,
                "-o", listsDir);
    }
    private static void downloadPkg(File workDir, Path tempAptDir, Path tempSources, String pkg) throws IOException, InterruptedException {
        String sourceList = "Dir::Etc::SourceList=" + tempSources;
        String sourceListDir = "Dir::Etc::SourceParts=" + tempAptDir;
        String listsDir = "Dir::State::Lists=" + tempAptDir.resolve("lists");
        runCommand(workDir, "apt-get", "download",
                "-o", sourceList,
                "-o", sourceListDir,
                "-o", listsDir,
                pkg);
    }

    private static void unpackDebFile(File debFile, File destinationDir) throws IOException, InterruptedException {
        runCommand(new File("."), "dpkg-deb", "-x", debFile.toString(), destinationDir.toString());
    }

    private record Artifact(String groupId, String artifactId, String version) {
    }

    public static Path createTempSourcesList(String mirrorUrl, String releaseName, String[] components) throws IOException {
        StringBuilder content = new StringBuilder();
        String componentsStr = String.join(" ", components);

        List<String> variants = List.of("", "-security", "-proposed", "-updates");

        for (String variant : variants) {
            content.append(String.format("deb %s %s%s %s\n",
                    mirrorUrl, releaseName, variant, componentsStr));
        }

        Path tempFile = Files.createTempFile("apt-sources-", ".list");
        tempFile.toFile().deleteOnExit();
        Files.writeString(tempFile, content.toString());

        return tempFile;
    }

    public static void main( String[] args ) throws IOException, InterruptedException {
        var release = "questing";
        var components = new String[] {"main", "universe"};
        var mirror = "http://nz.archive.ubuntu.com/ubuntu/";

        new File("artifacts.db").delete();
        DbManager dbManager = new DbManager("artifacts.db");
        dbManager.initialize();

        SourceListParser sourceListParser = new SourceListParser();
        //
        var deps = sourceListParser.findBuiltWith(new String[]{
                "default-jdk",
                "default-jdk-headless",
                "default-jre",
                "default-jre-headless",
                "openjdk-8-jdk",
                "openjdk-8-jdk-headless",
        }, release, components);
        // unpack dep[0]
        File debStore = new File("./debstore");
        debStore.mkdirs();
        File tempDir = null;
        var tempSources = createTempSourcesList(mirror, release, components);
        Path tempAptDir = Files.createTempDirectory("apt-tmp-");
        refreshAptSources(debStore, tempAptDir, tempSources);
        try {

            for (var packageName : deps) {
                try {
                    var existingFile = findPackageFile(packageName, debStore);
                    if (existingFile.isEmpty()) {
                        try {
                            downloadPkg(debStore, tempAptDir, tempSources, packageName);
                            existingFile = findPackageFile(packageName, debStore);
                        }
                        catch (Exception e) {
                            System.err.println(e.getMessage());
                            continue;
                        }
                    }
                    tempDir = Files.createTempDirectory("unpackedDebs").toFile();
                    unpackDebFile(existingFile.get(), tempDir);
                    ArrayList<Path> packageFiles = new ArrayList<>();
                    try (var stream = Files.walk(tempDir.toPath())) {
                        stream
                                .filter(Files::isRegularFile)
                                .filter( x -> x.toString().endsWith(".pom"))
                                .forEach(packageFiles::add);
                    } catch (IOException e) {
                        System.err.println("Error walking the file tree: " + e.getMessage());
                    }
                    runCommand(new File("."), "rm", "-rf", tempDir.toString());

                    var version = existingFile.get().getName().split("_")[1];
                    System.out.println("Extracted "+ packageName + " at " + version);
                    System.out.println("Writing database");
                    for (var fileInPackage : packageFiles) {
                        if (!fileInPackage.toString().contains("usr/share/maven-repo")) {
                            continue;
                        }
                        try {
                            var art = parseMavenRepoPath(tempDir.toPath().resolve("usr/share/maven-repo"),
                                    fileInPackage.toFile().getAbsoluteFile().getParentFile().toString());
                            if (art != null) {
                                dbManager.insertArtifact(art.groupId, art.artifactId, art.version, packageName, version);
                            }
                        }
                        catch (Exception e){
                            System.out.println(e.getMessage());
                        }
                    }

                } catch (IOException | InterruptedException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        } finally {
            dbManager.commit();
            if (tempDir != null) {
                tempDir.delete();
            }
        }
    }

    private static Artifact parseMavenRepoPath(Path parent, String pathInRepo) {
        int len = parent.toString().length();
        var fullArtifact = pathInRepo.substring(len+1);
        String[] artifactItems = fullArtifact.split("/");
        if (artifactItems.length < 2) {
            System.out.println("Wrong artifact name "+ fullArtifact);
            return null;
        }
        String version = artifactItems[artifactItems.length - 1];
        String artifactId = artifactItems[artifactItems.length - 2];
        StringBuilder groupId = new StringBuilder();
        for (int i = 0; i < artifactItems.length - 2; ++i) {
            if (i > 0) {
                groupId.append(".");
            }
            groupId.append(artifactItems[i]);
        }
        return new Artifact(groupId.toString(), artifactId, version);
    }

    private static Optional<File> findPackageFile(String pkg, File debStore) {
        return Arrays.stream(debStore.listFiles()).filter(x -> x.toString().contains(pkg+"_")).findFirst();
    }
}
