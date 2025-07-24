package org.debian.mavenproxy.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;

public class MavenBuildExecutor extends BuildExecutor {
    public MavenBuildExecutor(Map<String, Object> config) {
        super(config);
    }

    @Override
    protected Map<String, String> getEnvironment() {
        return Map.of();
    }

    @Override
    protected String[] getCommand() {
        ArrayList<String> command = new ArrayList<>();
        command.add(getBuildCommand());
        command.add("--settings");
        command.add(getSettings());
        command.addAll(getBuildArgs());
        return command.toArray(String[]::new);
    }

    private String getSettings() {
        try {
            File settings = File.createTempFile("settings", ".xml");
            settings.deleteOnExit();
            Files.writeString(settings.toPath(),
                    String.format("""
                            <settings>
                                <mirrors>
                                    <mirror>
                                      <id>default-mirror</id>
                                      <name>Proxy Mirror</name>
                                      <url>http://localhost:8080</url>
                                      <mirrorOf>*</mirrorOf>
                                    </mirror>
                                  </mirrors>
                            </settings>
                            """, getHelperPath(), getHelperPath()));
            return settings.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
