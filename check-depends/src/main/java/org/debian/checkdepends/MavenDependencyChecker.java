package org.debian.checkdepends;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


public class MavenDependencyChecker {
    public static final String NOT_YET_PACKAGED = "not-yet-packaged";
    private DbManager dbManager = new DbManager();

    public void run(String[] args) throws IOException, InterruptedException {
        Path repoLocal = Files.createTempDirectory("localMaven");
        try {
            String directory = args[1];
            ArrayList<String> commands = new ArrayList<>();

            commands.add("mvn");
            commands.add("-Dmaven.repo.local="+ repoLocal);
            commands.addAll(Arrays.asList(args).stream().skip(2).toList());
            ProcessBuilder pb = new ProcessBuilder(commands);
            pb.directory(new File(directory));
            pb.inheritIO();
            Process proc = pb.start();
            int ret = proc.waitFor();
            if (ret != 0) {
                return;
            }
            String[] fetchedArtifacts = findArtifacts(repoLocal);
            ArrayList<Artifact> result = new ArrayList<>();
            for (var path : fetchedArtifacts) {
                Artifact art = parseMavenRepoPath(repoLocal, path);
                List<Artifact> fullArtifacts =  dbManager.searchArtifacts(art.groupId(), art.artifactId());
                if (fullArtifacts.size() > 0) {
                    for (var x : fullArtifacts) {
                        result.add(new Artifact(art.groupId(), art.artifactId(), art.version(), x.packageName(), x.packageVersion()));
                    }
                } else {
                    result.add(new Artifact(art.groupId(), art.artifactId(), art.version(), NOT_YET_PACKAGED, ""));
                }


            }
            // Now we can print Build-Depends: for the maven project

            // find unique package names and associated artifacts
            HashSet<String> packages = new HashSet<String>();
            HashMap<String, String> packageVersions = new HashMap<>();
            HashMap<String, HashSet<String>> packageToArtifacts = new HashMap<>();
            for (var art : result) {
                packages.add(art.packageName());
                HashSet<String> artIds = packageToArtifacts.get(art.packageName());
                if (artIds == null){
                    artIds = new HashSet<>();
                    artIds.add(art.groupId() + ":"+ art.artifactId() + ":"+ art.version());
                    packageToArtifacts.put(art.packageName(), artIds);
                } else {
                    artIds.add(art.groupId() + ":"+ art.artifactId() + ":"+ art.version());
                }
                packageVersions.put(art.packageName(), art.packageVersion());
            }

            for (var p : packages) {
                if (NOT_YET_PACKAGED.equals(p)) {
                    continue;
                }
                printBuildDepends(p, packageToArtifacts, packageVersions);
            }
            printBuildDepends(NOT_YET_PACKAGED, packageToArtifacts, packageVersions);
        }
        finally {
            cleanup(repoLocal.toFile());
        }
    }

    private static void printBuildDepends(String p, HashMap<String, HashSet<String>> packageToArtifacts, HashMap<String, String> packageVersions) {
        HashSet<String> artIds = packageToArtifacts.get(p);
        for (var art : artIds) {
            System.out.println("# artifact " + art);
        }
        System.out.println(p + ", # best version " + packageVersions.get(p));
    }

    private void cleanup(File f) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("rm", "-rf", f.getAbsolutePath());
        pb.start().waitFor();
        f.delete();
    }

    private String[] findArtifacts(Path repoLocal) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("find", repoLocal.toString(), "-name", "*.pom");
        Process p = pb.start();
        if (p.waitFor() != 0) {
            throw new RuntimeException("Find failed in "+ repoLocal);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            return reader.lines().toList().toArray(String[]::new);
        }
    }

    private static Artifact parseMavenRepoPath(Path parent, String pathInRepo) {
        int len = parent.toString().length();
        var fullArtifact = pathInRepo.substring(len+1);
        String[] artifactItems = fullArtifact.split("/");
        String version = artifactItems[artifactItems.length - 2];
        String artifactId = artifactItems[artifactItems.length - 3];
        StringBuilder groupId = new StringBuilder();
        for (int i = 0; i < artifactItems.length - 3; ++i) {
            if (i > 0) {
                groupId.append(".");
            }
            groupId.append(artifactItems[i]);
        }
        return new Artifact(groupId.toString(), artifactId, version, null, null);
    }

}
