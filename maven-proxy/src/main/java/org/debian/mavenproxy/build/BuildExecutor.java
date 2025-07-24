package org.debian.mavenproxy.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public abstract class BuildExecutor {
    private final Path debianRepo;
    private final String buildCommand;
    private final List<String> buildArgs;
    private final String workingDirectory;

    public BuildExecutor(Map<String, Object> config) {
        this.buildCommand = (String) config.get("build-executable");
        this.buildArgs = (List<String>) config.get("command");
        this.workingDirectory = (String) config.get("workingDirectory");
        this.debianRepo = Path.of((String)config.get("helper-path")).toAbsolutePath();
        if (!Files.exists(this.debianRepo)) {
            throw new RuntimeException(this.debianRepo + " does not exist");
        }
    }

    public int run() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(getCommand())
                .directory(Path.of(workingDirectory).toFile())
                .inheritIO();
        pb.environment().putAll(getEnvironment());
        Process process = pb.start();
        return process.waitFor();
    }

    protected abstract Map<String, String> getEnvironment();
    protected abstract String[] getCommand();

    protected List<String> getBuildArgs() {
        return buildArgs;
    }

    protected String getBuildCommand() {
        return buildCommand;
    }

    protected String getWorkingDirectory() {
        return workingDirectory;
    }

    protected String getHelperPath() {
        return debianRepo.toString();
    }
}
